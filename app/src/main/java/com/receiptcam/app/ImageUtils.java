package com.receiptcam.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import androidx.exifinterface.media.ExifInterface;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/** Bitmap decode / rotate helpers. */
public final class ImageUtils {

    private ImageUtils() {}

    /** Decode a JPEG file, downsampled so the long edge is <= maxEdge, honoring EXIF rotation. */
    public static Bitmap decodeSampled(File file, int maxEdge) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

        int sample = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / (float) sample > maxEdge) {
            sample *= 2;
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
        if (bmp == null) return null;
        return rotateFromExif(bmp, file.getAbsolutePath());
    }

    /** Rotate bitmap according to EXIF orientation tag, if present. */
    public static Bitmap rotateFromExif(Bitmap bmp, String path) {
        int rotation;
        try {
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:  rotation = 90;  break;
                case ExifInterface.ORIENTATION_ROTATE_180: rotation = 180; break;
                case ExifInterface.ORIENTATION_ROTATE_270: rotation = 270; break;
                default: return bmp;
            }
        } catch (IOException e) {
            return bmp;
        }
        Matrix m = new Matrix();
        m.postRotate(rotation);
        Bitmap rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
        if (rotated != bmp) bmp.recycle();
        return rotated;
    }

    /** Return bitmap scaled so its long edge is <= maxEdge (returns input if already small). */
    public static Bitmap scaleToMax(Bitmap bmp, int maxEdge) {
        int w = bmp.getWidth(), h = bmp.getHeight();
        int longEdge = Math.max(w, h);
        if (longEdge <= maxEdge) return bmp;
        float s = maxEdge / (float) longEdge;
        Bitmap scaled = Bitmap.createScaledBitmap(bmp,
                Math.max(1, Math.round(w * s)), Math.max(1, Math.round(h * s)), true);
        scaled.setDensity(bmp.getDensity());
        return scaled;
    }

    /**
     * Decode an image from a content Uri (SAF documents), downsampled so the
     * long edge is <= maxEdge. Used for gallery thumbnails / viewer.
     */
    public static Bitmap decodeUriSampled(Context context, Uri uri, int maxEdge) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                if (in == null) return null;
                BitmapFactory.decodeStream(in, null, bounds);
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

            int sample = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / (float) sample > maxEdge) {
                sample *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                if (in == null) return null;
                return BitmapFactory.decodeStream(in, null, opts);
            }
        } catch (IOException e) {
            return null;
        }
    }
}
