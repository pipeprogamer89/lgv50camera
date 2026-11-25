package com.lge.camera.port;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaScannerConnection;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Locale;

/**
 * Burns a Cine Video 3D LUT into a recorded MP4 (MediaCodec decode -> GL LUT -> MediaCodec encode),
 * keeping the original AAC audio. Runs after CinemaModuleBase.stopRecorder().
 *
 * LG's HAL cannot apply the look to the 8-bit video stream on AOSP (the Cine usecase returns green),
 * so this is the only way to get the look into the saved file. The camera preview already shows the
 * look through the app's GPU film engine.
 */
public final class CineVideoPost {
    private static final String TAG = "LGCameraPort";
    private static volatile boolean sBusy;
    private static String sLastPath;

    private CineVideoPost() {}

    /** Entry point from the Cinema stop hook. */
    public static void processLatest(Context ctx, String preferredPath, String lutPath, int strength) {
        if (ctx == null || lutPath == null || sBusy) return;
        if (CineCompat.currentLut() <= 1) return;
        File in = preferredPath == null ? null : new File(preferredPath);
        if (in == null || !in.isFile() || in.length() == 0) in = newestVideo(ctx);
        processOne(ctx, in, lutPath, strength);
    }

    /** Debug entry point: process an explicit file. */
    public static void processFile(Context ctx, String path, String lutPath, int strength) {
        if (ctx == null || path == null || lutPath == null || sBusy) return;
        processOne(ctx, new File(path), lutPath, strength);
    }

    private static void processOne(Context ctx, File in, String lutPath, int strength) {
        if (in == null || !in.isFile() || sBusy) return;
        sBusy = true;
        try {
            String path = in.getAbsolutePath();
            if (path.equals(sLastPath)) {
                Log.i(TAG, "cine post: already processed " + path);
                return;
            }
            if (!waitStable(in)) {
                Log.w(TAG, "cine post: file never stabilized " + path);
                return;
            }
            Log.i(TAG, "cine post: processing " + path + " lut=" + lutPath + " strength=" + strength);
            toast(ctx, "Procesando vídeo Cine…");
            File tmp = new File(path + ".cine.tmp");
            boolean ok = transcode(in, tmp, lutPath, strength);
            if (ok && tmp.length() > 4096) {
                File bak = new File(path + ".cine.bak");
                if (bak.exists()) bak.delete();
                if (!in.renameTo(bak)) {
                    Log.e(TAG, "cine post: cannot rename original");
                    tmp.delete();
                    return;
                }
                if (!tmp.renameTo(in)) {
                    Log.e(TAG, "cine post: cannot move result");
                    bak.renameTo(in);
                    tmp.delete();
                    return;
                }
                if ("1".equals(Props.get("debug.lgcamera.keepraw", "0"))) {
                    File raw = new File(path + ".raw.mp4");
                    if (raw.exists()) raw.delete();
                    bak.renameTo(raw);
                    Log.i(TAG, "cine post: kept raw at " + raw);
                } else {
                    bak.delete();
                }
                sLastPath = path;
                MediaScannerConnection.scanFile(ctx, new String[]{path}, null, null);
                toast(ctx, "Vídeo Cine guardado");
                Log.i(TAG, "cine post: done " + path + " (" + in.length() + " bytes)");
            } else {
                tmp.delete();
                toast(ctx, "No se pudo procesar el vídeo Cine");
                Log.e(TAG, "cine post: transcode failed");
            }
        } catch (Throwable t) {
            Log.e(TAG, "cine post failed", t);
        } finally {
            sBusy = false;
        }
    }

    // ------------------------------------------------------------------ file helpers

    private static File newestVideo(Context ctx) {
        File dir = new File("/storage/emulated/0/DCIM/Camera");
        File[] files = dir.listFiles();
        if (files == null) return null;
        File best = null;
        long now = System.currentTimeMillis();
        for (File f : files) {
            if (!f.isFile() || !f.getName().toLowerCase(Locale.US).endsWith(".mp4")) continue;
            if (now - f.lastModified() > 10 * 60 * 1000L) continue;
            if (best == null || f.lastModified() > best.lastModified()) best = f;
        }
        return best;
    }

    private static boolean waitStable(File f) {
        long last = -1;
        for (int i = 0; i < 40; i++) {
            long len = f.length();
            if (len > 0 && len == last) return true;
            last = len;
            try { Thread.sleep(500); } catch (InterruptedException e) { return false; }
        }
        return false;
    }

    private static void toast(final Context ctx, final String msg) {
        new Handler(Looper.getMainLooper()).post(() -> {
            try { Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show(); } catch (Throwable ignored) {}
        });
    }

    // ------------------------------------------------------------------ transcode

    private static boolean transcode(File in, File out, String lutPath, int strength) {
        MediaExtractor ex = new MediaExtractor();
        MediaCodec dec = null, enc = null;
        MediaMuxer mux = null;
        Surface decSurface = null;
        android.graphics.SurfaceTexture st = null;
        Egl egl = null;
        try {
            ex.setDataSource(in.getAbsolutePath());
            int vTrack = -1, aTrack = -1;
            MediaFormat vf = null, af = null;
            for (int i = 0; i < ex.getTrackCount(); i++) {
                MediaFormat f = ex.getTrackFormat(i);
                String mime = f.getString(MediaFormat.KEY_MIME);
                if (mime == null) continue;
                if (vTrack < 0 && mime.startsWith("video/")) { vTrack = i; vf = f; }
                else if (aTrack < 0 && mime.startsWith("audio/")) { aTrack = i; af = f; }
            }
            if (vTrack < 0 || vf == null) return false;

            int w = vf.getInteger(MediaFormat.KEY_WIDTH);
            int h = vf.getInteger(MediaFormat.KEY_HEIGHT);
            int rotation = vf.containsKey(MediaFormat.KEY_ROTATION) ? vf.getInteger(MediaFormat.KEY_ROTATION) : 0;
            int fps = vf.containsKey(MediaFormat.KEY_FRAME_RATE) ? vf.getInteger(MediaFormat.KEY_FRAME_RATE) : 30;
            if (fps <= 0) fps = 30;
            int bitrate = vf.containsKey(MediaFormat.KEY_BIT_RATE) ? vf.getInteger(MediaFormat.KEY_BIT_RATE) : 0;
            // keep the original quality: ~w*h*fps/4 (15 Mbps for 1080p30), never below 16 Mbps
            int target = Math.max(24_000_000, (int) ((long) w * h * fps / 3));
            if (bitrate < target) bitrate = target;
            String inMime = vf.getString(MediaFormat.KEY_MIME);
            String outMime = inMime != null && inMime.contains("hevc") ? "video/hevc" : "video/avc";
            Log.i(TAG, "cine post: " + w + "x" + h + " " + outMime + " fps=" + fps
                    + " br=" + bitrate + " rot=" + rotation);

            // ----- decoder -> SurfaceTexture
            dec = MediaCodec.createDecoderByType(inMime);
            int[] tex = new int[1];
            GLES30.glGenTextures(1, tex, 0);
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
            st = new android.graphics.SurfaceTexture(tex[0]);
            st.setDefaultBufferSize(w, h);
            decSurface = new Surface(st);
            // Some Qualcomm decoders apply KEY_ROTATION to the output, which double-rotates the
            // frame (the muxer already carries the rotation hint). Decode unrotated.
            MediaFormat decFmt = new MediaFormat(vf);
            if (decFmt.containsKey(MediaFormat.KEY_ROTATION)) decFmt.removeKey(MediaFormat.KEY_ROTATION);
            dec.configure(decFmt, decSurface, null, 0);
            dec.start();

            // ----- encoder
            enc = MediaCodec.createEncoderByType(outMime);
            MediaFormat ef = MediaFormat.createVideoFormat(outMime, w, h);
            ef.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            ef.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
            ef.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
            ef.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            enc.configure(ef, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            Surface encSurface = enc.createInputSurface();
            enc.start();
            try { Log.i(TAG, "cine post: encoder input " + enc.getInputFormat()); } catch (Throwable ignored) {}

            // ----- EGL + shader + LUT
            egl = new Egl(encSurface, w, h, tex[0]);
            int lutTex = egl.loadLut(lutPath);
            if (lutTex == 0) return false;

            // ----- muxer
            mux = new MediaMuxer(out.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            if (rotation != 0) mux.setOrientationHint(rotation);
            int aOut = -1;
            if (af != null) {
                try { aOut = mux.addTrack(af); } catch (Throwable t) { Log.w(TAG, "add audio", t); }
            }

            State state = new State();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean inputDone = false, outputDone = false;
            ex.selectTrack(vTrack);
            long lastPts = -1;

            while (!outputDone) {
                if (!inputDone) {
                    int idx = dec.dequeueInputBuffer(10000);
                    if (idx >= 0) {
                        ByteBuffer decIn = dec.getInputBuffer(idx);
                        int size = decIn == null ? -1 : ex.readSampleData(decIn, 0);
                        if (size < 0) {
                            dec.queueInputBuffer(idx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        } else {
                            long pts = ex.getSampleTime();
                            dec.queueInputBuffer(idx, 0, size, pts, ex.getSampleFlags());
                            ex.advance();
                        }
                    }
                }

                int oi = dec.dequeueOutputBuffer(info, 10000);
                if (oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.i(TAG, "cine post: decoder output " + dec.getOutputFormat());
                    continue;
                }
                if (oi >= 0) {
                    boolean render = info.size != 0;
                    dec.releaseOutputBuffer(oi, render);
                    if (render) {
                        st.updateTexImage();
                        long pts = info.presentationTimeUs;
                        if (pts <= lastPts) pts = lastPts + 1000;
                        lastPts = pts;
                        egl.draw(st, lutTex, strength / 100f, pts);
                        drainEncoder(enc, mux, state, aOut, false);
                    }
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true;
                }
            }

            // signal EOS and drain until the encoder reports end-of-stream
            drainEncoder(enc, mux, state, aOut, true);

            // copy audio afterwards (the muxer is already started by now)
            if (aOut >= 0 && state.started) {
                ex.unselectTrack(vTrack);
                ex.selectTrack(aTrack);
                ByteBuffer abuf = ByteBuffer.allocateDirect(256 * 1024);
                MediaCodec.BufferInfo ai = new MediaCodec.BufferInfo();
                while (true) {
                    int size = ex.readSampleData(abuf, 0);
                    if (size < 0) break;
                    ai.set(0, size, ex.getSampleTime(), ex.getSampleFlags());
                    try { mux.writeSampleData(aOut, abuf, ai); } catch (Throwable t) { Log.w(TAG, "audio write", t); break; }
                    ex.advance();
                }
            }

            if (state.vOut < 0 || !state.started) {
                Log.e(TAG, "cine post: no video track muxed");
                return false;
            }

            if (state.started) mux.stop();
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "cine transcode failed", t);
            return false;
        } finally {
            try { if (mux != null) mux.release(); } catch (Throwable ignored) {}
            try { if (enc != null) { enc.stop(); enc.release(); } } catch (Throwable ignored) {}
            try { if (dec != null) { dec.stop(); dec.release(); } } catch (Throwable ignored) {}
            try { if (decSurface != null) decSurface.release(); } catch (Throwable ignored) {}
            try { if (st != null) st.release(); } catch (Throwable ignored) {}
            try { if (egl != null) egl.release(); } catch (Throwable ignored) {}
            try { ex.release(); } catch (Throwable ignored) {}
        }
    }

    private static final class State {
        int vOut = -1;
        boolean started;
    }

    private static void drainEncoder(MediaCodec enc, MediaMuxer mux, State state, int aOut, boolean eos) {
        try {
            if (eos) {
                try { enc.signalEndOfInputStream(); } catch (Throwable t) { Log.w(TAG, "signal EOS", t); }
            }
            MediaCodec.BufferInfo bi = new MediaCodec.BufferInfo();
            long deadline = System.currentTimeMillis() + 15000;
            while (true) {
                int oi = enc.dequeueOutputBuffer(bi, eos ? 20000 : 0);
                if (oi == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (eos && System.currentTimeMillis() < deadline) continue;
                    break;
                }
                if (oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (state.vOut < 0) {
                        try { state.vOut = mux.addTrack(enc.getOutputFormat()); } catch (Throwable t) { Log.e(TAG, "add video", t); }
                        if (!state.started && state.vOut >= 0) {
                            try { mux.start(); state.started = true; } catch (Throwable t) { Log.e(TAG, "mux start", t); }
                        }
                    }
                    continue;
                }
                if (oi < 0) {
                    if (eos && System.currentTimeMillis() < deadline) continue;
                    break;
                }
                ByteBuffer ob = enc.getOutputBuffer(oi);
                if (ob != null && bi.size > 0
                        && (bi.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
                        && state.started) {
                    ob.position(bi.offset);
                    ob.limit(bi.offset + bi.size);
                    try { mux.writeSampleData(state.vOut, ob, bi); } catch (Throwable t) { Log.w(TAG, "video write", t); }
                }
                boolean end = (bi.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                enc.releaseOutputBuffer(oi, false);
                if (end) break;
            }
        } catch (Throwable t) {
            Log.e(TAG, "drain encoder", t);
        }
    }

    // ------------------------------------------------------------------ GL

    private static final class Egl {
        private EGLDisplay display;
        private EGLContext context;
        private EGLSurface surface;
        private int program;
        private int aPosition, aTexCoord, uSTMatrix, uTex, uLut, uStrength;
        private FloatBuffer quad, texCoord;
        private final float[] stMatrix = new float[16];
        private int camTex;

        Egl(Surface encSurface, int w, int h, int camTex) throws Exception {
            this.camTex = camTex;
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            int[] ver = new int[2];
            if (!EGL14.eglInitialize(display, ver, 0, ver, 1)) throw new IOException("eglInitialize");
            int[] attrs = {
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                    0x3142 /*EGL_RECORDABLE_ANDROID*/, 1,
                    EGL14.EGL_NONE
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] n = new int[1];
            if (!EGL14.eglChooseConfig(display, attrs, 0, configs, 0, 1, n, 0) || n[0] == 0) {
                throw new IOException("eglChooseConfig");
            }
            int[] ctxAttrs = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE};
            context = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT, ctxAttrs, 0);
            int[] sAttrs = {EGL14.EGL_NONE};
            surface = EGL14.eglCreateWindowSurface(display, configs[0], encSurface, sAttrs, 0);
            if (surface == null) throw new IOException("eglCreateWindowSurface");
            if (!EGL14.eglMakeCurrent(display, surface, surface, context)) throw new IOException("eglMakeCurrent");
            int[] dim = new int[1];
            EGL14.eglQuerySurface(display, surface, EGL14.EGL_WIDTH, dim, 0);
            int sw = dim[0];
            EGL14.eglQuerySurface(display, surface, EGL14.EGL_HEIGHT, dim, 0);
            Log.i(TAG, "cine post: egl surface " + sw + "x" + dim[0]);
            GLES30.glViewport(0, 0, w, h);
            program = build();
            aPosition = GLES30.glGetAttribLocation(program, "aPosition");
            aTexCoord = GLES30.glGetAttribLocation(program, "aTexCoord");
            uSTMatrix = GLES30.glGetUniformLocation(program, "uSTMatrix");
            uTex = GLES30.glGetUniformLocation(program, "uTex");
            uLut = GLES30.glGetUniformLocation(program, "uLut");
            uStrength = GLES30.glGetUniformLocation(program, "uStrength");
            float[] q = {-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f};
            quad = ByteBuffer.allocateDirect(q.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            quad.put(q).position(0);
            float[] t = {0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f};
            texCoord = ByteBuffer.allocateDirect(t.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            texCoord.put(t).position(0);
        }

        int loadLut(String path) {
            File f = new File(path);
            int n = 36, size = n * n * n * 3;
            if (!f.isFile() || f.length() < size) {
                Log.e(TAG, "cine post: bad LUT " + path + " len=" + (f.isFile() ? f.length() : -1));
                return 0;
            }
            byte[] data = new byte[size];
            try (FileInputStream is = new FileInputStream(f)) {
                int off = 0;
                while (off < size) {
                    int r = is.read(data, off, size - off);
                    if (r <= 0) break;
                    off += r;
                }
                if (off != size) return 0;
            } catch (Throwable t) {
                Log.e(TAG, "cine post: read LUT", t);
                return 0;
            }
            ByteBuffer bb = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
            bb.put(data).position(0);
            int[] tex = new int[1];
            GLES30.glGenTextures(1, tex, 0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, tex[0]);
            GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_R, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexImage3D(GLES30.GL_TEXTURE_3D, 0, GLES30.GL_RGB8, n, n, n, 0,
                    GLES30.GL_RGB, GLES30.GL_UNSIGNED_BYTE, bb);
            int err = GLES30.glGetError();
            if (err != 0) Log.w(TAG, "cine post: LUT gl error 0x" + Integer.toHexString(err));
            return tex[0];
        }

        private boolean loggedMatrix;
        void draw(android.graphics.SurfaceTexture st, int lutTex, float strength, long ptsUs) {
            st.getTransformMatrix(stMatrix);
            if (!loggedMatrix) {
                loggedMatrix = true;
                StringBuilder sb = new StringBuilder("ST matrix:");
                for (float v : stMatrix) sb.append(' ').append(String.format(Locale.US, "%.3f", v));
                Log.i(TAG, sb.toString());
            }
            GLES30.glUseProgram(program);
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, camTex);
            GLES30.glUniform1i(uTex, 0);
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, lutTex);
            GLES30.glUniform1i(uLut, 1);
            GLES30.glUniform1f(uStrength, strength);
            GLES30.glUniformMatrix4fv(uSTMatrix, 1, false, stMatrix, 0);
            GLES30.glEnableVertexAttribArray(aPosition);
            GLES30.glVertexAttribPointer(aPosition, 2, GLES30.GL_FLOAT, false, 0, quad);
            GLES30.glEnableVertexAttribArray(aTexCoord);
            GLES30.glVertexAttribPointer(aTexCoord, 2, GLES30.GL_FLOAT, false, 0, texCoord);
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
            GLES30.glDisableVertexAttribArray(aPosition);
            GLES30.glDisableVertexAttribArray(aTexCoord);
            android.opengl.EGLExt.eglPresentationTimeANDROID(display, surface, ptsUs * 1000);
            EGL14.eglSwapBuffers(display, surface);
        }

        private int build() {
            String vs = "#version 300 es\n" +
                    "uniform mat4 uSTMatrix;\n" +
                    "in vec4 aPosition;\n" +
                    "in vec4 aTexCoord;\n" +
                    "out vec2 vTex;\n" +
                    "void main(){ gl_Position=aPosition; vTex=(uSTMatrix*aTexCoord).xy; }\n";
            String fs = "#version 300 es\n" +
                    "#extension GL_OES_EGL_image_external_essl3 : require\n" +
                    "precision highp float;\n" +
                    "uniform samplerExternalOES uTex;\n" +
                    "uniform sampler3D uLut;\n" +
                    "uniform float uStrength;\n" +
                    "in vec2 vTex;\n" +
                    "out vec4 o;\n" +
                    "void main(){ vec4 c=texture(uTex,vTex); vec3 l=texture(uLut,clamp(c.rgb,0.0,1.0)).rgb;\n" +
                    " o=vec4(mix(c.rgb,l,uStrength),1.0); }\n";
            int v = shader(GLES30.GL_VERTEX_SHADER, vs);
            int f = shader(GLES30.GL_FRAGMENT_SHADER, fs);
            int p = GLES30.glCreateProgram();
            GLES30.glAttachShader(p, v);
            GLES30.glAttachShader(p, f);
            GLES30.glLinkProgram(p);
            int[] ok = new int[1];
            GLES30.glGetProgramiv(p, GLES30.GL_LINK_STATUS, ok, 0);
            if (ok[0] == 0) {
                Log.e(TAG, "cine post: link " + GLES30.glGetProgramInfoLog(p));
            }
            GLES30.glDeleteShader(v);
            GLES30.glDeleteShader(f);
            return p;
        }

        private int shader(int type, String src) {
            int s = GLES30.glCreateShader(type);
            GLES30.glShaderSource(s, src);
            GLES30.glCompileShader(s);
            int[] ok = new int[1];
            GLES30.glGetShaderiv(s, GLES30.GL_COMPILE_STATUS, ok, 0);
            if (ok[0] == 0) Log.e(TAG, "cine post: shader " + GLES30.glGetShaderInfoLog(s));
            return s;
        }

        void release() {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            if (surface != null) EGL14.eglDestroySurface(display, surface);
            if (context != null) EGL14.eglDestroyContext(display, context);
            EGL14.eglTerminate(display);
        }
    }
}
