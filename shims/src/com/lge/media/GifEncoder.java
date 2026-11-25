package com.lge.media;
import android.graphics.Bitmap;
/** LG native GIF encoder is not available on AOSP; report failure so the app skips GIF export. */
public class GifEncoder {
    public GifEncoder() {}
    public boolean start(String path) { return false; }
    public void setDelay(int ms) {}
    public boolean addFrame(Bitmap b) { return false; }
    public boolean addFrameDouble(Bitmap a, Bitmap b) { return false; }
    public boolean addFrameTriple(Bitmap a, Bitmap b, Bitmap c) { return false; }
    public boolean finish() { return false; }
}
