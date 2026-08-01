package com.receiptcam.app;

import android.graphics.Bitmap;

/**
 * Sharpness enhancement (unsharp mask) in pure Java.
 * out = src + amount * (src - blur(src))
 */
public final class ImageEnhancer {

    private ImageEnhancer() {}

    /**
     * @param src     input bitmap
     * @param percent 0..100 sharpening strength (0 = no change)
     * @return sharpened bitmap (or src when percent <= 0)
     */
    public static Bitmap unsharpMask(Bitmap src, int percent) {
        if (percent <= 0) return src;

        int w = src.getWidth(), h = src.getHeight();
        int[] orig = new int[w * h];
        src.getPixels(orig, 0, w, 0, 0, w, h);

        int radius = Math.max(1, Math.min(w, h) / 350);
        int[] blurred = boxBlurArgb(orig, w, h, radius);

        float amount = percent * 0.015f;
        int[] out = new int[w * h];
        for (int i = 0; i < orig.length; i++) {
            int o = orig[i], b = blurred[i];
            int r = boost((o >> 16) & 0xff, (b >> 16) & 0xff, amount);
            int g = boost((o >> 8) & 0xff,  (b >> 8) & 0xff,  amount);
            int bl = boost(o & 0xff,        b & 0xff,         amount);
            out[i] = 0xFF000000 | (r << 16) | (g << 8) | bl;
        }
        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888);
    }

    private static int boost(int o, int b, float amount) {
        int v = Math.round(o + amount * (o - b));
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    private static int[] boxBlurArgb(int[] src, int w, int h, int radius) {
        int[] tmp = new int[w * h];
        int[] out = new int[w * h];
        int n = 2 * radius + 1;

        for (int y = 0; y < h; y++) {                 // horizontal
            int base = y * w;
            int rSum = 0, gSum = 0, bSum = 0;
            for (int x = -radius; x <= radius; x++) {
                int c = src[base + clamp(x, w)];
                rSum += (c >> 16) & 0xff; gSum += (c >> 8) & 0xff; bSum += c & 0xff;
            }
            for (int x = 0; x < w; x++) {
                tmp[base + x] = ((rSum / n) << 16) | ((gSum / n) << 8) | (bSum / n);
                int add = src[base + clamp(x + radius + 1, w)];
                int rem = src[base + clamp(x - radius, w)];
                rSum += ((add >> 16) & 0xff) - ((rem >> 16) & 0xff);
                gSum += ((add >> 8) & 0xff) - ((rem >> 8) & 0xff);
                bSum += (add & 0xff) - (rem & 0xff);
            }
        }
        for (int x = 0; x < w; x++) {                 // vertical
            int rSum = 0, gSum = 0, bSum = 0;
            for (int y = -radius; y <= radius; y++) {
                int c = tmp[clamp(y, h) * w + x];
                rSum += (c >> 16) & 0xff; gSum += (c >> 8) & 0xff; bSum += c & 0xff;
            }
            for (int y = 0; y < h; y++) {
                out[y * w + x] = ((rSum / n) << 16) | ((gSum / n) << 8) | (bSum / n);
                int add = tmp[clamp(y + radius + 1, h) * w + x];
                int rem = tmp[clamp(y - radius, h) * w + x];
                rSum += ((add >> 16) & 0xff) - ((rem >> 16) & 0xff);
                gSum += ((add >> 8) & 0xff) - ((rem >> 8) & 0xff);
                bSum += (add & 0xff) - (rem & 0xff);
            }
        }
        return out;
    }

    private static int clamp(int v, int size) {
        return v < 0 ? 0 : (v >= size ? size - 1 : v);
    }
}
