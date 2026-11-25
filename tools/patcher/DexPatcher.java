import com.android.tools.smali.dexlib2.AccessFlags;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c;
import com.android.tools.smali.dexlib2.iface.DexFile;
import com.android.tools.smali.dexlib2.iface.ExceptionHandler;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.MethodImplementation;
import com.android.tools.smali.dexlib2.iface.MethodParameter;
import com.android.tools.smali.dexlib2.iface.TryBlock;
import com.android.tools.smali.dexlib2.iface.debug.DebugItem;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c;
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction31i;
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction31i;
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rc;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction35c;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction3rc;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.rewriter.DexRewriter;
import com.android.tools.smali.dexlib2.rewriter.InstructionRewriter;
import com.android.tools.smali.dexlib2.rewriter.MethodImplementationRewriter;
import com.android.tools.smali.dexlib2.rewriter.MethodParameterRewriter;
import com.android.tools.smali.dexlib2.rewriter.MethodReferenceRewriter;
import com.android.tools.smali.dexlib2.rewriter.MethodRewriter;
import com.android.tools.smali.dexlib2.rewriter.Rewriter;
import com.android.tools.smali.dexlib2.rewriter.RewriterModule;
import com.android.tools.smali.dexlib2.rewriter.Rewriters;
import com.android.tools.smali.dexlib2.rewriter.TypeRewriter;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Patches the LG V50 camera dex files so they run on AOSP (Android 13-15):
 *  1. TYPES: LG subclasses of platform services are mapped back to the AOSP class
 *     (getSystemService() returns the AOSP object, so the LG check-cast would fail).
 *  2. STATIC: static platform calls that no longer exist are redirected to Compat/PortShim.
 *  3. VIRTUAL: instance calls to methods LG added to AOSP interfaces become
 *     invoke-static Compat.x(receiver, args...).
 *  4. STUBS: app methods that only touch LG framework fields are replaced by "return default".
 *  5. PortShim.init(this) is injected at the start of LGCameraApplication.onCreate().
 * All debug info is dropped (dexlib2 writer bug corrupts it; verified with dexdump).
 *
 * Usage: DexPatcher in.dex out.dex
 */
public class DexPatcher {
    static final String SHIM = "Lcom/lge/camera/port/PortShim;";
    static final String COMPAT = "Lcom/lge/camera/port/Compat;";
    static final String SCREENSHOTS = "Lcom/lge/camera/port/Screenshots;";
    static final String CINE = "Lcom/lge/camera/port/CineCompat;";
    static final String APP = "Lcom/lge/camera/app/LGCameraApplication;";

    static final Map<String, String> TYPES = new HashMap<>();
    static final Map<String, String[]> STATIC = new HashMap<>();   // key -> {class, name}
    static final Map<String, String[]> VIRTUAL = new HashMap<>();
    static final Map<String, String[]> CALLS = new HashMap<>();    // app-internal static call sites only (definitions untouched)  // key -> {class, name, receiverType}
    static final Map<String, String> STUBS = new HashMap<>();
    static final Map<String, String[]> STRINGS = new HashMap<>(); // method key -> {from, to} const-string in that method only
    static final Map<String, String[]> HOOKS = new HashMap<>();   // method key -> {class, name} called at method entry (receiver as first Object arg)
    static final Map<String, int[]> LITERALS = new HashMap<>();   // method key -> {from, to} for CONST literals in that method only      // method key -> description
    static final Map<String, Integer> hits = new TreeMap<>();

    static {
        TYPES.put("Landroid/media/AudioManagerEx;", "Landroid/media/AudioManager;");
        TYPES.put("Landroid/app/ActivityManagerEx;", "Landroid/app/ActivityManager;");

        STATIC.put("Landroid/util/FloatMath;->sqrt(F)F", new String[]{SHIM, "sqrt"});
        STATIC.put("Landroid/os/SystemProperties;->get(Ljava/lang/String;)Ljava/lang/String;", new String[]{COMPAT, "getProp"});
        STATIC.put("Landroid/view/SurfaceControl;->getPhysicalDisplayIds()[J", new String[]{COMPAT, "getPhysicalDisplayIds"});
        STATIC.put("Landroid/view/SurfaceControl;->captureLayers(Landroid/view/SurfaceControl;Landroid/graphics/Rect;F)Landroid/view/SurfaceControl$ScreenshotGraphicBuffer;",
                new String[]{COMPAT, "captureLayers"});
        STATIC.put("Landroid/security/KeyStore;->getApplicationContext()Landroid/content/Context;",
                new String[]{COMPAT, "keyStoreApplicationContext"});

        STATIC.put("Landroid/hardware/Camera;->getCameraInfo(ILandroid/hardware/Camera$CameraInfo;)V", new String[]{COMPAT, "getCameraInfo"});
        // LG screenshots (Live Photo frames, transitions) -> PixelCopy of the preview views
        CALLS.put("Lcom/lge/camera/util/Utils;->getScreenShot(Landroid/content/Context;IIZLandroid/graphics/Rect;ZIZZ)Landroid/graphics/Bitmap;", new String[]{SCREENSHOTS, "getScreenShot"});
        CALLS.put("Lcom/lge/camera/util/Utils;->getScreenShot(Landroid/content/Context;IIZLandroid/graphics/Rect;Z)Landroid/graphics/Bitmap;", new String[]{SCREENSHOTS, "getScreenShot"});
        CALLS.put("Lcom/lge/camera/util/Utils;->getScreenShotWithCropRect(Landroid/app/Activity;IILandroid/graphics/Rect;IZZ)Landroid/graphics/Bitmap;", new String[]{SCREENSHOTS, "getScreenShotWithCropRect"});

        VIRTUAL.put("Landroid/app/IActivityManager;->getAllStackInfos()Ljava/util/List;", new String[]{COMPAT, "getAllStackInfos"});
        VIRTUAL.put("Landroid/view/IWindowManager;->isOneHandUxOn()Z", new String[]{COMPAT, "isOneHandUxOn"});
        VIRTUAL.put("Landroid/view/IWindowManager;->isDesktopModeEnabled()Z", new String[]{COMPAT, "isDesktopModeEnabled"});
        VIRTUAL.put("Landroid/view/IWindowManager;->setForcedDisplayRotationForUser(IZ)V", new String[]{COMPAT, "setForcedDisplayRotationForUser"});
        VIRTUAL.put("Landroid/view/IWindowManager;->setNavigationBarFromApps(IZ)V", new String[]{COMPAT, "setNavigationBarFromApps"});
        VIRTUAL.put("Landroid/app/IActivityTaskManager;->minimizeAllFreeforms(I)V", new String[]{COMPAT, "minimizeAllFreeforms"});
        // Close without the AOSP "unconfigure" (fake stream) that crashes the LG camera HAL.
        VIRTUAL.put("Landroid/hardware/camera2/CameraCaptureSession;->close()V", new String[]{COMPAT, "closeSession"});
        // Video session without JPEG outputs (avoids the HAL's deferred snapshot session crash).
        VIRTUAL.put("Lcom/lge/camera/device/api2/BlockingCameraManager;->createRecordingSession(Ljava/lang/Boolean;Landroid/hardware/camera2/CameraDevice;Ljava/util/ArrayList;Landroid/os/Handler;ILandroid/hardware/camera2/CaptureRequest;)Landroid/hardware/camera2/CameraCaptureSession;", new String[]{COMPAT, "createRecordingSession"});
        // AI CAM without LG's EllieVision scene service: the smartcam LUT map can be empty.
        VIRTUAL.put("Lcom/lge/camera/managers/FilmEmulatorManager;->setCurLutAndValue(Ljava/lang/String;)V", new String[]{COMPAT, "setCurLutAndValue"});
        VIRTUAL.put("Landroid/hardware/camera2/CameraManager;->getCameraIdList()[Ljava/lang/String;", new String[]{COMPAT, "getCameraIdList"});

        // Writes LG-only WindowManager.LayoutParams.bypassKeyFlags.
        // No photos while recording: the video session is created without JPEG outputs (see
        // Compat.createRecordingSession), so hide the live-snapshot button everywhere.
        STUBS.put("Lcom/lge/camera/device/CameraCapabilities;->isLiveSnapshotSupported()Z", "false");
        STUBS.put("Lcom/lge/camera/app/ext/SteadyCamModule;->isLiveSnapshotSupported()Z", "false");
        STUBS.put("Lcom/lge/camera/app/ManualVideoModule;->isLiveSnapshotSupported()Z", "false");
        // HDR10 / Cine Video: the 10-bit "2stream HDR10 EISv3" usecase fails to mmap the preview
        // gralloc buffer on LineageOS (CSLMapNativeBuffer errno 22) and the preview freezes.
        // Record 8-bit instead: drop OP_SETTING_HDR10 (0xe040 -> no-op 0xe000) and the
        // org.quic.camera2.streamconfigs.HDRVideoMode request tag. Cine filters still apply.
        LITERALS.put("Lcom/lge/camera/device/api2/CameraOperationMode;->getOperationMode(Lcom/lge/camera/device/api2/Parameters2;ZZZI)I", new int[]{0xe040, 0xe000});
        STUBS.put("Lcom/lge/camera/device/api2/BuilderSet;->applyHDR10(Landroid/hardware/camera2/CaptureRequest$Builder;)V", "no-op");
        // Cine Video: LG's cine usecase needs 10-bit buffers LineageOS gralloc can't provide (green,
        // empty video). Run it on the normal video usecase and apply the cine looks as film LUTs
        // (cine_N.lut, converted by tools/cine2film.py, shipped by the HAL-fix Magisk module).
        LITERALS.put("Lcom/lge/camera/device/api2/CameraOperationMode;-><clinit>()V", new int[]{0xe700, 0xe080});
        // The recorder's persistent input surface is created through a throwaway HEVC encoder
        // configured as Main10 HDR10 (0x1000) in Cine/HDR10, which makes the surface buffers 10-bit;
        // the camera writes 8-bit into them -> green garbage. Use HEVC Main (1).
        LITERALS.put("Lcom/lge/camera/app/VideoRecorder;->createPersistentSurface(Ljava/lang/String;ILjava/lang/String;)Landroid/view/Surface;", new int[]{0x1000, 0x1});
        // LG's "downloaded filters" (LG SmartWorld) extend the film menu dynamically: used to ship
        // the Cine Video looks as extra Photo/Video filters (see tools/cine2film.py, out/cinefilters).
        STUBS.put("Lcom/lge/camera/constants/FunctionProperties;->isSupportedFilterDownload()Z", "true");
        // Cine Video: the HAL CineEffect only runs in LG's 10-bit HDR10 usecase, which LineageOS
        // cannot back. CinemaModule disables the app's film-filter menu (and the GPU LUT engine that
        // drives preview + recording) for that reason. Re-enable it: the same Cine looks are shipped
        // as film LUTs, so Cine Video can use the normal 8-bit video pipeline.
        STUBS.put("Lcom/lge/camera/app/ext/CinemaModuleBase;->isSupportedFilterMenu()Z", "true");
        // CinemaModuleBase.initializeSettingMenus() calls setSpecificSettingValueAndDisable(
        // "key_film_emulator", "0_film.dat"): it forces the film LUT off AND disables the setting
        // (checkToUseFilmEffect then returns false). Point that one call at a dummy key so the real
        // film filter stays enabled in Cine Video.
        STRINGS.put("Lcom/lge/camera/app/ext/CinemaModuleBase;->initializeSettingMenus()V",
                new String[]{"key_film_emulator", "key_film_emulator_cinema_disabled"});

        // Cine Video looks through the app's own GPU film engine (8-bit, works on AOSP).
        // See CineCompat for the whole story.
        HOOKS.put("Lcom/lge/camera/app/ext/CinemaModuleBase;->initializeSettingMenus()V",
                new String[]{CINE, "onCinemaInit"});
        HOOKS.put("Lcom/lge/camera/managers/ext/CinemaFilterManager;->setCinemaLUT()V",
                new String[]{CINE, "onCinemaLut"});
        HOOKS.put("Lcom/lge/camera/app/DefaultCameraModule;->checkToUseFilmEffect()Z",
                new String[]{CINE, "beforeFilmGate"});
        HOOKS.put("Lcom/lge/camera/app/ext/CinemaModuleBase;->stopRecorder()V",
                new String[]{CINE, "onCinemaStopRecorder"});
        // LG Hi-Fi audio asks MediaRecorder for encoder 12, which AOSP rejects.
        VIRTUAL.put("Lcom/lge/media/MediaRecorderEx;->setAudioEncoder(I)V",
                new String[]{CINE, "setAudioEncoder"});
        // In Cine Video the GPU engine is used for the *preview* only. LG's HAL already applies
        // the selected look to the encoded stream (the 8-bit CineEffect path works); handing the
        // recorder's persistent surface to the GPU engine as well makes MediaRecorder fail
        // (SurfaceMediaSource could not be initialized) and crashes the app.
        VIRTUAL.put("Lcom/lge/camera/managers/AdvancedSelfieManager;->prepareRecording(Ljava/lang/String;Z)V",
                new String[]{CINE, "prepareFilmRecording"});
        // Same reason: don't tell the preview-only GPU engine to start/stop writing to the
        // recorder surface in Cine Video.
        VIRTUAL.put("Lcom/lge/camera/managers/AdvancedSelfieManager;->startRecorder()V",
                new String[]{CINE, "startFilmRecording"});
        VIRTUAL.put("Lcom/lge/camera/managers/AdvancedSelfieManager;->stopRecorder(Z)V",
                new String[]{CINE, "stopFilmRecording"});
        CALLS.put("Lcom/lge/camera/managers/FilmEmulatorHelper;->getFilterDownloadPath()Ljava/lang/String;", new String[]{COMPAT, "filterDownloadPath"});
        STUBS.put("Lcom/lge/camera/util/AppControlUtil;->setBypassAttribute(Landroid/view/Window;IZ)V", "no-op");
    }

    static String key(MethodReference m) {
        return m.getDefiningClass() + "->" + m.getName() + "(" + String.join("", m.getParameterTypes()) + ")" + m.getReturnType();
    }

    static void hit(String what) { hits.merge(what, 1, Integer::sum); }

    static MethodReference staticTarget(MethodReference m, String[] t) {
        List<String> params = new ArrayList<>();
        params.add("Ljava/lang/Object;"); // receiver
        params.addAll(Arrays.asList(m.getParameterTypes().toArray(new String[0])));
        return new ImmutableMethodReference(t[0], t[1], params, m.getReturnType());
    }

    public static void main(String[] args) throws Exception {
        DexFile in = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.forApi(30));
        DexRewriter rw = new DexRewriter(new RewriterModule() {
            @Override
            public Rewriter<String> getTypeRewriter(Rewriters r) {
                return new TypeRewriter() {
                    @Override
                    protected String rewriteUnwrappedType(String t) {
                        String n = TYPES.get(t);
                        if (n != null) { hit("type " + t); return n; }
                        return t;
                    }
                };
            }

            @Override
            public Rewriter<MethodReference> getMethodReferenceRewriter(Rewriters r) {
                return new MethodReferenceRewriter(r) {
                    @Override
                    public MethodReference rewrite(MethodReference m) {
                        String[] t = STATIC.get(key(m));
                        if (t != null) {
                            hit("static " + key(m));
                            return new ImmutableMethodReference(t[0], t[1], m.getParameterTypes(), m.getReturnType());
                        }
                        return super.rewrite(m);
                    }
                };
            }

            @Override
            public Rewriter<Instruction> getInstructionRewriter(Rewriters r) {
                return new InstructionRewriter(r) {
                    @Override
                    public Instruction rewrite(Instruction insn) {
                        Opcode op = insn.getOpcode();
                        if (insn instanceof ReferenceInstruction
                                && ((ReferenceInstruction) insn).getReference() instanceof MethodReference) {
                            MethodReference m = (MethodReference) ((ReferenceInstruction) insn).getReference();
                            String[] c = CALLS.get(key(m));
                            if (c != null && (op == Opcode.INVOKE_STATIC || op == Opcode.INVOKE_STATIC_RANGE)) {
                                hit("call " + key(m));
                                MethodReference target = new ImmutableMethodReference(c[0], c[1], m.getParameterTypes(), m.getReturnType());
                                if (op == Opcode.INVOKE_STATIC) {
                                    Instruction35c i = (Instruction35c) insn;
                                    return new ImmutableInstruction35c(op, i.getRegisterCount(), i.getRegisterC(), i.getRegisterD(),
                                            i.getRegisterE(), i.getRegisterF(), i.getRegisterG(), target);
                                }
                                Instruction3rc i = (Instruction3rc) insn;
                                return new ImmutableInstruction3rc(op, i.getStartRegister(), i.getRegisterCount(), target);
                            }
                            String[] t = VIRTUAL.get(key(m));
                            if (t != null) {
                                hit("virtual " + key(m));
                                MethodReference target = staticTarget(m, t);
                                if (op == Opcode.INVOKE_VIRTUAL || op == Opcode.INVOKE_INTERFACE || op == Opcode.INVOKE_DIRECT) {
                                    Instruction35c i = (Instruction35c) insn;
                                    return new ImmutableInstruction35c(Opcode.INVOKE_STATIC, i.getRegisterCount(),
                                            i.getRegisterC(), i.getRegisterD(), i.getRegisterE(), i.getRegisterF(),
                                            i.getRegisterG(), target);
                                }
                                if (op == Opcode.INVOKE_VIRTUAL_RANGE || op == Opcode.INVOKE_INTERFACE_RANGE || op == Opcode.INVOKE_DIRECT_RANGE) {
                                    Instruction3rc i = (Instruction3rc) insn;
                                    return new ImmutableInstruction3rc(Opcode.INVOKE_STATIC_RANGE,
                                            i.getStartRegister(), i.getRegisterCount(), target);
                                }
                                throw new IllegalStateException("unexpected opcode " + op + " for " + key(m));
                            }
                        }
                        return super.rewrite(insn);
                    }
                };
            }

            @Override
            public Rewriter<MethodParameter> getMethodParameterRewriter(Rewriters r) {
                return new MethodParameterRewriter(r) {
                    @Override
                    public MethodParameter rewrite(MethodParameter p) {
                        MethodParameter q = super.rewrite(p);
                        return new ImmutableMethodParameter(q.getType(), q.getAnnotations(), null);
                    }
                };
            }

            @Override
            public Rewriter<MethodImplementation> getMethodImplementationRewriter(Rewriters r) {
                return new MethodImplementationRewriter(r) {
                    @Override
                    public MethodImplementation rewrite(MethodImplementation impl) {
                        final MethodImplementation base = super.rewrite(impl);
                        return new MethodImplementation() {
                            public int getRegisterCount() { return base.getRegisterCount(); }
                            public Iterable<? extends Instruction> getInstructions() { return base.getInstructions(); }
                            public List<? extends TryBlock<? extends ExceptionHandler>> getTryBlocks() { return base.getTryBlocks(); }
                            public Iterable<? extends DebugItem> getDebugItems() { return Collections.<DebugItem>emptyList(); }
                        };
                    }
                };
            }

            @Override
            public Rewriter<Method> getMethodRewriter(Rewriters r) {
                return new MethodRewriter(r) {
                    @Override
                    public Method rewrite(Method value) {
                        Method m = super.rewrite(value);
                        String k = key(m);
                        String[] str = STRINGS.get(k);
                        if (str != null && m.getImplementation() != null) {
                            MutableMethodImplementation mut = new MutableMethodImplementation(m.getImplementation());
                            List<com.android.tools.smali.dexlib2.builder.BuilderInstruction> ins = mut.getInstructions();
                            for (int i = 0; i < ins.size(); i++) {
                                Instruction in = ins.get(i);
                                Opcode op = in.getOpcode();
                                if (op != Opcode.CONST_STRING && op != Opcode.CONST_STRING_JUMBO) continue;
                                Object ref = ((ReferenceInstruction) in).getReference();
                                if (!(ref instanceof StringReference)) continue;
                                if (!str[0].equals(((StringReference) ref).getString())) continue;
                                int reg = ((OneRegisterInstruction) in).getRegisterA();
                                StringReference nr = new com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference(str[1]);
                                com.android.tools.smali.dexlib2.builder.BuilderInstruction ni =
                                        op == Opcode.CONST_STRING
                                                ? new com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c(op, reg, nr)
                                                : new com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31c(op, reg, nr);
                                mut.replaceInstruction(i, ni);
                                hit("string " + str[0] + "->" + str[1] + " in " + k);
                            }
                            m = withImpl(m, mut);
                        }
                        int[] lit = LITERALS.get(k);
                        if (lit != null && m.getImplementation() != null) {
                            MutableMethodImplementation mut = new MutableMethodImplementation(m.getImplementation());
                            List<com.android.tools.smali.dexlib2.builder.BuilderInstruction> ins = mut.getInstructions();
                            for (int i = 0; i < ins.size(); i++) {
                                Instruction in = ins.get(i);
                                if (in.getOpcode() == Opcode.CONST_16
                                        && ((com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21s) in).getNarrowLiteral() == lit[0]) {
                                    mut.replaceInstruction(i, new com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21s(
                                            Opcode.CONST_16, ((com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21s) in).getRegisterA(), lit[1]));
                                    hit("literal16 " + Integer.toHexString(lit[0]) + "->" + Integer.toHexString(lit[1]) + " in " + k);
                                    continue;
                                }
                                if (in.getOpcode() == Opcode.CONST && ((Instruction31i) in).getNarrowLiteral() == lit[0]) {
                                    mut.replaceInstruction(i, new com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31i(
                                            Opcode.CONST, ((Instruction31i) in).getRegisterA(), lit[1]));
                                    hit("literal " + Integer.toHexString(lit[0]) + "->" + Integer.toHexString(lit[1]) + " in " + k);
                                }
                            }
                            m = withImpl(m, mut);
                        }
                        String[] hook = HOOKS.get(k);
                        if (hook != null && m.getImplementation() != null) {
                            MethodImplementation impl = m.getImplementation();
                            MutableMethodImplementation mut = new MutableMethodImplementation(impl);
                            boolean isStatic = AccessFlags.STATIC.isSet(m.getAccessFlags());
                            List<String> types = new ArrayList<>();
                            if (!isStatic) types.add("Ljava/lang/Object;");
                            for (CharSequence p : m.getParameterTypes()) types.add(p.toString());
                            MethodReference target = new ImmutableMethodReference(hook[0], hook[1], types, "V");
                            int regs = impl.getRegisterCount();
                            int words = 0;
                            for (CharSequence p : m.getParameterTypes()) {
                                char c = p.charAt(0);
                                words += (c == 'J' || c == 'D') ? 2 : 1;
                            }
                            int first = isStatic ? regs - words : regs - 1 - words;
                            List<Integer> list = new ArrayList<>();
                            if (!isStatic) list.add(first);
                            int r = first + (isStatic ? 0 : 1);
                            for (CharSequence p : m.getParameterTypes()) {
                                list.add(r);
                                char c = p.charAt(0);
                                r += (c == 'J' || c == 'D') ? 2 : 1;
                            }
                            boolean small = list.size() <= 5;
                            for (int reg : list) if (reg > 15) small = false;
                            com.android.tools.smali.dexlib2.builder.BuilderInstruction call;
                            if (small) {
                                int[] q = new int[5];
                                for (int i = 0; i < list.size(); i++) q[i] = list.get(i);
                                call = new com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c(
                                        Opcode.INVOKE_STATIC, list.size(), q[0], q[1], q[2], q[3], q[4], target);
                            } else {
                                call = new com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc(
                                        Opcode.INVOKE_STATIC_RANGE, first, r - first, target);
                            }
                            mut.addInstruction(0, call);
                            hit("hook " + k + " -> " + hook[0] + "->" + hook[1]);
                            m = withImpl(m, mut);
                        }
                        if (STUBS.containsKey(k)) {
                            hit("stub " + k);
                            return withImpl(m, stubImpl(m));
                        }                        if (APP.equals(m.getDefiningClass()) && "onCreate".equals(m.getName())
                                && m.getParameterTypes().isEmpty() && "V".equals(m.getReturnType())) {
                            MethodImplementation impl = m.getImplementation();
                            MutableMethodImplementation mut = new MutableMethodImplementation(impl);
                            int p0 = impl.getRegisterCount() - 1; // 'this' is the only parameter
                            mut.addInstruction(0, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 1, p0, 0, 0, 0, 0,
                                    new ImmutableMethodReference(SHIM, "init",
                                            Collections.singletonList("Landroid/app/Application;"), "V")));
                            hit("inject PortShim.init");
                            return withImpl(m, mut);
                        }
                        return m;
                    }
                };
            }
        });
        DexFile out = rw.getDexFileRewriter().rewrite(in);
        DexPool.writeTo(args[1], out);
        System.out.println(args[0] + ":");
        hits.forEach((k, v) -> System.out.println("  " + k + " x" + v));
    }

    static Method withImpl(Method m, MethodImplementation impl) {
        return new ImmutableMethod(m.getDefiningClass(), m.getName(), m.getParameters(), m.getReturnType(),
                m.getAccessFlags(), m.getAnnotations(), m.getHiddenApiRestrictions(), impl);
    }

    /** Body that returns the default value for the method's return type. */
    static MethodImplementation stubImpl(Method m) {
        int params = 0;
        for (CharSequence p : m.getParameterTypes()) params += (p.charAt(0) == 'J' || p.charAt(0) == 'D') ? 2 : 1;
        if (!AccessFlags.STATIC.isSet(m.getAccessFlags())) params++;
        MutableMethodImplementation impl = new MutableMethodImplementation(params + 1);
        String rt = m.getReturnType();
        if (rt.equals("V")) {
            impl.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
        } else if (rt.length() == 1 && "ZBSCIF".contains(rt)) {
            impl.addInstruction(new BuilderInstruction11n(Opcode.CONST_4, 0, "true".equals(STUBS.get(key(m))) ? 1 : 0));
            impl.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));
        } else if (rt.startsWith("L") || rt.startsWith("[")) {
            impl.addInstruction(new BuilderInstruction11n(Opcode.CONST_4, 0, 0));
            impl.addInstruction(new BuilderInstruction11x(Opcode.RETURN_OBJECT, 0));
        } else {
            throw new IllegalStateException("stub for wide return type not supported: " + key(m));
        }
        return impl;
    }
}
