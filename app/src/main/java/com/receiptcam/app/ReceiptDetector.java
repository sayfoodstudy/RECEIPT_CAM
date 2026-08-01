package com.receiptcam.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Finds the receipt (bright paper on darker background) in a photo and
 * perspective-corrects it to a rectangle. Pure Java, no OCR, no external libs.
 */
public final class ReceiptDetector {

    /** Max output long edge of the cropped receipt image. */
    public static final int OUTPUT_MAX_EDGE = 2048;

    /** Analysis image long edge. Small = fast. */
    private static final int ANALYZE_EDGE = 480;

    /** Detected quad must cover at least this fraction of the frame. */
    private static final double MIN_AREA_FRACTION = 0.18;

    /** Minimum foreground/background gray separation for a usable threshold. */
    private static final int MIN_SEPARATION = 18;

    private ReceiptDetector() {}

    /**
     * Find receipt corners in the source image.
     * @return float[8] = (tl.x,tl.y, tr.x,tr.y, br.x,br.y, bl.x,bl.y) in src pixels, or null.
     */
    public static float[] findReceiptCorners(Bitmap src) {
        int srcW = src.getWidth(), srcH = src.getHeight();

        float scale = ANALYZE_EDGE / (float) Math.max(srcW, srcH);
        if (scale > 1f) scale = 1f;
        int w = Math.max(1, Math.round(srcW * scale));
        int h = Math.max(1, Math.round(srcH * scale));
        Bitmap small = Bitmap.createScaledBitmap(src, w, h, true);

        int[] px = new int[w * h];
        small.getPixels(px, 0, w, 0, 0, w, h);
        if (small != src) small.recycle();

        int[] gray = new int[w * h];
        for (int i = 0; i < px.length; i++) {
            int c = px[i];
            int r = (c >> 16) & 0xff, g = (c >> 8) & 0xff, b = c & 0xff;
            gray[i] = (r * 299 + g * 587 + b * 114) / 1000;
        }

        int[] blurred = boxBlurGray(gray, w, h, 2);

        int[] otsu = otsu(blurred);
        if (otsu == null) return null;
        int thr = otsu[0];

        boolean[] mask = new boolean[w * h];
        for (int i = 0; i < blurred.length; i++) mask[i] = blurred[i] >= thr;
        mask = majority3x3(mask, w, h);
        mask = majority3x3(mask, w, h);

        List<int[]> pts = new ArrayList<>();
        for (int y = 0; y < h; y += 2) {
            for (int x = 0; x < w; x += 2) {
                if (mask[y * w + x]) pts.add(new int[]{x, y});
            }
        }
        if (pts.size() < 100) return null;

        List<int[]> hull = convexHull(pts);
        if (hull.size() < 3) return null;

        MinRect rect = minAreaRect(hull);
        if (rect == null) return null;
        if (rect.area < MIN_AREA_FRACTION * (double) w * (double) h) return null;

        float[] ordered = orderCorners(rect.corners);

        // Expand slightly (2%) around the centroid so paper edges are not clipped.
        float cx = 0, cy = 0;
        for (int i = 0; i < 4; i++) { cx += ordered[i * 2]; cy += ordered[i * 2 + 1]; }
        cx /= 4f; cy /= 4f;
        for (int i = 0; i < 4; i++) {
            ordered[i * 2]     = cx + (ordered[i * 2]     - cx) * 1.02f;
            ordered[i * 2 + 1] = cy + (ordered[i * 2 + 1] - cy) * 1.02f;
        }

        float inv = 1f / scale;
        for (int i = 0; i < 8; i++) ordered[i] *= inv;
        return ordered;
    }

    /**
     * Warp the quad found by {@link #findReceiptCorners} into a rectangle.
     * Corner order must be tl,tr,br,bl.
     */
    public static Bitmap warpToRect(Bitmap src, float[] c) {
        float wTop    = dist(c[0], c[1], c[2], c[3]);
        float wBottom = dist(c[6], c[7], c[4], c[5]);
        float hLeft   = dist(c[0], c[1], c[6], c[7]);
        float hRight  = dist(c[2], c[3], c[4], c[5]);
        float outWf = Math.max(wTop, wBottom);
        float outHf = Math.max(hLeft, hRight);

        float shrink = OUTPUT_MAX_EDGE / Math.max(outWf, outHf);
        if (shrink < 1f) { outWf *= shrink; outHf *= shrink; }
        int outW = Math.max(1, Math.round(outWf));
        int outH = Math.max(1, Math.round(outHf));

        Bitmap out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        canvas.drawColor(0xFFFFFFFF);

        float[] dst = {0, 0, outW, 0, outW, outH, 0, outH};
        Matrix m = new Matrix();
        m.setPolyToPoly(c, 0, dst, 0, 4);

        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        canvas.drawBitmap(src, m, paint);
        return out;
    }

    private static float dist(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1, dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    // ---- grayscale helpers ----

    private static int[] boxBlurGray(int[] src, int w, int h, int radius) {
        int[] tmp = new int[w * h];
        int[] out = new int[w * h];
        int n = 2 * radius + 1;

        for (int y = 0; y < h; y++) {                 // horizontal
            int base = y * w;
            int sum = 0;
            for (int x = -radius; x <= radius; x++) {
                sum += src[base + clamp(x, w)];
            }
            for (int x = 0; x < w; x++) {
                tmp[base + x] = sum / n;
                sum += src[base + clamp(x + radius + 1, w)];
                sum -= src[base + clamp(x - radius, w)];
            }
        }
        for (int x = 0; x < w; x++) {                 // vertical
            int sum = 0;
            for (int y = -radius; y <= radius; y++) {
                sum += tmp[clamp(y, h) * w + x];
            }
            for (int y = 0; y < h; y++) {
                out[y * w + x] = sum / n;
                sum += tmp[clamp(y + radius + 1, h) * w + x];
                sum -= tmp[clamp(y - radius, h) * w + x];
            }
        }
        return out;
    }

    private static int clamp(int v, int size) {
        return v < 0 ? 0 : (v >= size ? size - 1 : v);
    }

    /** @return {threshold, separation} (bright mean minus dark mean), or null if image is flat. */
    private static int[] otsu(int[] gray) {
        int[] hist = new int[256];
        for (int v : gray) hist[v]++;
        int total = gray.length;
        long sumAll = 0;
        for (int i = 0; i < 256; i++) sumAll += (long) i * hist[i];

        long sumB = 0;
        int wB = 0;
        double maxVar = -1;
        int bestT = 128;
        double bestM1 = 0, bestM2 = 0;
        for (int t = 0; t < 256; t++) {
            wB += hist[t];
            if (wB == 0) continue;
            sumB += (long) t * hist[t];
            int wF = total - wB;
            if (wF == 0) break;
            double mB = (double) sumB / wB;
            double mF = (double) (sumAll - sumB) / wF;
            double var = (double) wB * wF * (mB - mF) * (mB - mF);
            if (var > maxVar) {
                maxVar = var;
                bestT = t;
                bestM1 = mB;
                bestM2 = mF;
            }
        }
        if (Math.abs(bestM2 - bestM1) < MIN_SEPARATION) return null;
        return new int[]{bestT, (int) Math.abs(bestM2 - bestM1)};
    }

    /** 3x3 majority filter: smooths speckle noise in the mask. */
    private static boolean[] majority3x3(boolean[] in, int w, int h) {
        boolean[] out = new boolean[w * h];
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int count = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (in[(y + dy) * w + (x + dx)]) count++;
                    }
                }
                out[y * w + x] = count >= 5;
            }
        }
        return out;
    }

    // ---- geometry helpers ----

    private static long cross(int[] o, int[] a, int[] b) {
        return (long) (a[0] - o[0]) * (b[1] - o[1]) - (long) (a[1] - o[1]) * (b[0] - o[0]);
    }

    /** Monotone chain convex hull. Points are int[]{x,y}. */
    private static List<int[]> convexHull(List<int[]> points) {
        List<int[]> pts = new ArrayList<>(points);
        Collections.sort(pts, new Comparator<int[]>() {
            @Override public int compare(int[] a, int[] b) {
                return a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]);
            }
        });
        List<int[]> lower = new ArrayList<>();
        for (int[] p : pts) {
            while (lower.size() >= 2
                    && cross(lower.get(lower.size() - 2), lower.get(lower.size() - 1), p) <= 0) {
                lower.remove(lower.size() - 1);
            }
            lower.add(p);
        }
        List<int[]> upper = new ArrayList<>();
        for (int i = pts.size() - 1; i >= 0; i--) {
            int[] p = pts.get(i);
            while (upper.size() >= 2
                    && cross(upper.get(upper.size() - 2), upper.get(upper.size() - 1), p) <= 0) {
                upper.remove(upper.size() - 1);
            }
            upper.add(p);
        }
        lower.remove(lower.size() - 1);
        upper.remove(upper.size() - 1);
        lower.addAll(upper);
        return lower;
    }

    private static final class MinRect {
        final float[] corners; // 8 floats, walking around the rect
        final double area;
        MinRect(float[] corners, double area) { this.corners = corners; this.area = area; }
    }

    /** Minimum-area bounding rectangle of the hull (rotating calipers over hull edges). */
    private static MinRect minAreaRect(List<int[]> hull) {
        double bestArea = Double.MAX_VALUE;
        float[] best = null;

        for (int i = 0; i < hull.size(); i++) {
            int[] p0 = hull.get(i);
            int[] p1 = hull.get((i + 1) % hull.size());
            double ang = Math.atan2(p1[1] - p0[1], p1[0] - p0[0]);
            double cos = Math.cos(ang), sin = Math.sin(ang);

            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            for (int[] p : hull) {
                // rotate by -ang
                double rx = p[0] * cos + p[1] * sin;
                double ry = -p[0] * sin + p[1] * cos;
                if (rx < minX) minX = rx;
                if (rx > maxX) maxX = rx;
                if (ry < minY) minY = ry;
                if (ry > maxY) maxY = ry;
            }
            double area = (maxX - minX) * (maxY - minY);
            if (area < bestArea) {
                bestArea = area;
                best = new float[8];
                double[][] rc = {{minX, minY}, {maxX, minY}, {maxX, maxY}, {minX, maxY}};
                for (int k = 0; k < 4; k++) {
                    // rotate back by +ang
                    best[k * 2]     = (float) (rc[k][0] * cos - rc[k][1] * sin);
                    best[k * 2 + 1] = (float) (rc[k][0] * sin + rc[k][1] * cos);
                }
            }
        }
        return best == null ? null : new MinRect(best, bestArea);
    }

    /** Order 4 corners as tl, tr, br, bl (screen coords, y down). */
    private static float[] orderCorners(float[] pts) {
        int tl = 0, tr = 0, br = 0, bl = 0;
        float minSum = Float.MAX_VALUE, maxSum = -Float.MAX_VALUE;
        float minDiff = Float.MAX_VALUE, maxDiff = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            float x = pts[i * 2], y = pts[i * 2 + 1];
            float sum = x + y, diff = y - x;
            if (sum < minSum)  { minSum = sum;   tl = i; }
            if (sum > maxSum)  { maxSum = sum;   br = i; }
            if (diff < minDiff){ minDiff = diff; tr = i; }
            if (diff > maxDiff){ maxDiff = diff; bl = i; }
        }
        return new float[]{
                pts[tl * 2], pts[tl * 2 + 1],
                pts[tr * 2], pts[tr * 2 + 1],
                pts[br * 2], pts[br * 2 + 1],
                pts[bl * 2], pts[bl * 2 + 1]
        };
    }
}
