package com.receiptcam.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Saves JPEGs into the user-picked folder via Storage Access Framework.
 * Photos never leave the device: no cloud APIs are used anywhere.
 */
public final class StorageHelper {

    private static final String PREFS = "receiptcam_prefs";
    private static final String KEY_TREE = "tree_uri";

    private StorageHelper() {}

    public static Uri getTreeUri(Context context) {
        String s = prefs(context).getString(KEY_TREE, null);
        return s == null ? null : Uri.parse(s);
    }

    public static void setTreeUri(Context context, Uri uri) {
        prefs(context).edit().putString(KEY_TREE, uri == null ? null : uri.toString()).apply();
    }

    public static String folderName(Context context) {
        Uri tree = getTreeUri(context);
        if (tree == null) return null;
        DocumentFile dir = DocumentFile.fromTreeUri(context, tree);
        return dir == null ? null : dir.getName();
    }

    /** Save bitmap as ReceiptCam_yyyyMMdd_HHmmss.jpg in the picked folder. */
    public static Uri saveJpeg(Context context, Bitmap bitmap) throws IOException {
        Uri tree = getTreeUri(context);
        if (tree == null) return null;
        DocumentFile dir = DocumentFile.fromTreeUri(context, tree);
        if (dir == null || !dir.canWrite()) return null;

        String name = "ReceiptCam_"
                + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date())
                + ".jpg";
        DocumentFile file = dir.createFile("image/jpeg", name);
        if (file == null) return null;

        try (OutputStream os = context.getContentResolver().openOutputStream(file.getUri())) {
            if (os == null) return null;
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, os)) return null;
        }
        return file.getUri();
    }

    /** Count .jpg/.jpeg files in the picked folder (top level only). */
    public static int countSavedImages(Context context) {
        Uri tree = getTreeUri(context);
        if (tree == null) return 0;
        DocumentFile dir = DocumentFile.fromTreeUri(context, tree);
        if (dir == null) return 0;
        int count = 0;
        for (DocumentFile f : dir.listFiles()) {
            if (!f.isFile()) continue;
            String n = f.getName();
            if (n == null) continue;
            String lower = n.toLowerCase(Locale.US);
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) count++;
        }
        return count;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
