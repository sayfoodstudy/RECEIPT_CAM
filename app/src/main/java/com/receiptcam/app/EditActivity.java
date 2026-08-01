package com.receiptcam.app;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditActivity extends AppCompatActivity {

    private static final int DECODE_MAX_EDGE = 3000;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ImageView imageView;
    private TextView noticeLabel;
    private TextView sharpValue;
    private SeekBar seekSharp;
    private Button btnToggle;
    private Button btnSave;

    private File rawFile;
    private Bitmap photoFull;     // decoded + rotated original (max 3000px)
    private Bitmap croppedBase;   // auto-cropped receipt (fallback: full photo)
    private Bitmap fullBase;      // scaled full photo (toggle view)
    private Bitmap shownBitmap;   // cropped/full with current sharpness applied
    private boolean showingCropped = true;
    private boolean cropSucceeded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        imageView = findViewById(R.id.imageView);
        noticeLabel = findViewById(R.id.noticeLabel);
        sharpValue = findViewById(R.id.sharpValue);
        seekSharp = findViewById(R.id.seekSharp);
        btnToggle = findViewById(R.id.btnToggle);
        btnSave = findViewById(R.id.btnSave);
        Button btnRetake = findViewById(R.id.btnRetake);

        String path = getIntent().getStringExtra(CameraActivity.EXTRA_PHOTO_PATH);
        if (path == null) {
            finish();
            return;
        }
        rawFile = new File(path);

        btnRetake.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveCurrent());
        btnToggle.setOnClickListener(v -> toggleView());

        seekSharp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sharpValue.setText(String.valueOf(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                applySharpness();
            }
        });

        btnSave.setEnabled(false);
        processPhoto();
    }

    /** Decode -> detect receipt -> crop (or full-photo fallback) -> initial sharpen. */
    private void processPhoto() {
        executor.execute(() -> {
            photoFull = ImageUtils.decodeSampled(rawFile, DECODE_MAX_EDGE);
            if (photoFull == null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.capture_failed, Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }

            float[] corners = ReceiptDetector.findReceiptCorners(photoFull);
            cropSucceeded = corners != null;
            if (cropSucceeded) {
                croppedBase = ReceiptDetector.warpToRect(photoFull, corners);
            } else {
                croppedBase = ImageUtils.scaleToMax(photoFull,
                        ReceiptDetector.OUTPUT_MAX_EDGE);
            }
            fullBase = ImageUtils.scaleToMax(photoFull, ReceiptDetector.OUTPUT_MAX_EDGE);

            runOnUiThread(() -> {
                noticeLabel.setText(cropSucceeded
                        ? R.string.crop_ok_notice : R.string.crop_failed_notice);
                btnSave.setEnabled(true);
                applySharpness();
            });
        });
    }

    /** Apply current seekbar sharpness to the active base image, off the UI thread. */
    private void applySharpness() {
        final Bitmap base = showingCropped ? croppedBase : fullBase;
        if (base == null) return;
        final int amount = seekSharp.getProgress();
        executor.execute(() -> {
            Bitmap result = ImageEnhancer.unsharpMask(base, amount);
            runOnUiThread(() -> {
                shownBitmap = result;
                imageView.setImageBitmap(result);
            });
        });
    }

    private void toggleView() {
        showingCropped = !showingCropped;
        btnToggle.setText(showingCropped
                ? R.string.btn_toggle_original : R.string.btn_toggle_cropped);
        applySharpness();
    }

    private void saveCurrent() {
        if (shownBitmap == null) return;
        btnSave.setEnabled(false);
        executor.execute(() -> {
            boolean ok = false;
            try {
                ok = StorageHelper.saveJpeg(this, shownBitmap) != null;
            } catch (IOException ignored) { }
            final boolean success = ok;
            runOnUiThread(() -> {
                Toast.makeText(this,
                        success ? R.string.saved_ok : R.string.saved_fail,
                        Toast.LENGTH_SHORT).show();
                if (success) {
                    cleanupAndClose();
                } else {
                    btnSave.setEnabled(true);
                }
            });
        });
    }

    private void cleanupAndClose() {
        if (rawFile != null && rawFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            rawFile.delete();
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
        if (isFinishing()) cleanupCacheFile();
    }

    private void cleanupCacheFile() {
        if (rawFile != null && rawFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            rawFile.delete();
        }
    }
}
