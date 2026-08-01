package com.receiptcam.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 촬영 후 편집: 자동/수동 컷 + 밝기/대비/선명도 조정 + 저장. */
public class EditActivity extends AppCompatActivity {

    private static final int DECODE_MAX_EDGE = 3000;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ImageView imageView;
    private TextView noticeLabel;
    private TextView valBrightness;
    private TextView valContrast;
    private TextView valSharp;
    private SeekBar seekBrightness;
    private SeekBar seekContrast;
    private SeekBar seekSharp;
    private Button btnToggle;
    private Button btnSave;

    private File rawFile;
    private Bitmap photoFull;     // decoded + rotated original (max 3000px)
    private Bitmap croppedBase;   // auto/manual cut result (fallback: full photo)
    private Bitmap fullBase;      // scaled full photo (toggle view)
    private Bitmap shownBitmap;   // base + adjustments
    private boolean showingCropped = true;

    private final ActivityResultLauncher<Intent> cropLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && BitmapHolder.croppedResult != null) {
                    croppedBase = BitmapHolder.croppedResult;
                    BitmapHolder.croppedResult = null;
                    showingCropped = true;
                    btnToggle.setText(R.string.btn_toggle_original);
                    noticeLabel.setText(R.string.edit_hint_manual);
                    applyAdjustments();
                }
            });

    private final ActivityResultLauncher<Uri> saveFolderPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), uri -> {
                if (uri == null) return;
                StorageHelper.persistTreePermission(this, uri);
                StorageHelper.setTreeUri(this, uri);
                doSave();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        imageView = findViewById(R.id.imageView);
        noticeLabel = findViewById(R.id.noticeLabel);
        valBrightness = findViewById(R.id.valBrightness);
        valContrast = findViewById(R.id.valContrast);
        valSharp = findViewById(R.id.valSharp);
        seekBrightness = findViewById(R.id.seekBrightness);
        seekContrast = findViewById(R.id.seekContrast);
        seekSharp = findViewById(R.id.seekSharp);
        btnToggle = findViewById(R.id.btnToggle);
        btnSave = findViewById(R.id.btnSave);
        ImageButton btnBack = findViewById(R.id.btnBack);
        Button btnRetake = findViewById(R.id.btnRetake);
        Button btnManualCrop = findViewById(R.id.btnManualCrop);

        String path = getIntent().getStringExtra(CameraActivity.EXTRA_PHOTO_PATH);
        if (path == null) {
            finish();
            return;
        }
        rawFile = new File(path);

        btnBack.setOnClickListener(v -> finish());
        btnRetake.setOnClickListener(v -> finish());
        btnManualCrop.setOnClickListener(v -> openManualCrop());
        btnToggle.setOnClickListener(v -> toggleView());
        btnSave.setOnClickListener(v -> onSaveClicked());

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateValueLabels();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                applyAdjustments();
            }
        };
        seekBrightness.setOnSeekBarChangeListener(listener);
        seekContrast.setOnSeekBarChangeListener(listener);
        seekSharp.setOnSeekBarChangeListener(listener);

        btnSave.setEnabled(false);
        processPhoto();
    }

    private void updateValueLabels() {
        int bright = seekBrightness.getProgress() - 100;
        valBrightness.setText(bright > 0 ? "+" + bright : String.valueOf(bright));
        valContrast.setText(seekContrast.getProgress() + "%");
        valSharp.setText(String.valueOf(seekSharp.getProgress()));
    }

    /** Decode -> auto-detect -> crop (fallback: full photo) -> initial render. */
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
            BitmapHolder.photo = photoFull;

            float[] corners = ReceiptDetector.findReceiptCorners(photoFull);
            BitmapHolder.lastDetectedCorners = corners;

            final boolean ok = corners != null;
            croppedBase = ok
                    ? ReceiptDetector.warpToRect(photoFull, corners)
                    : ImageUtils.scaleToMax(photoFull, ReceiptDetector.OUTPUT_MAX_EDGE);
            fullBase = ImageUtils.scaleToMax(photoFull, ReceiptDetector.OUTPUT_MAX_EDGE);

            runOnUiThread(() -> {
                noticeLabel.setText(ok ? R.string.edit_hint_auto : R.string.edit_hint_fail);
                btnSave.setEnabled(true);
                updateValueLabels();
                applyAdjustments();
            });
        });
    }

    /** Apply sharpness + brightness + contrast off the UI thread. */
    private void applyAdjustments() {
        final Bitmap base = showingCropped ? croppedBase : fullBase;
        if (base == null) return;
        final int sharp = seekSharp.getProgress();
        final int bright = seekBrightness.getProgress() - 100;
        final float contrast = seekContrast.getProgress() / 100f;
        executor.execute(() -> {
            Bitmap result = ImageEnhancer.applyAll(base, sharp, bright, contrast);
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
        applyAdjustments();
    }

    private void openManualCrop() {
        if (BitmapHolder.photo == null) return;
        cropLauncher.launch(new Intent(this, CropActivity.class));
    }

    private void onSaveClicked() {
        if (shownBitmap == null) return;
        if (StorageHelper.getTreeUri(this) == null) {
            Toast.makeText(this, R.string.pick_folder_to_save, Toast.LENGTH_SHORT).show();
            saveFolderPicker.launch(null);
            return;
        }
        doSave();
    }

    private void doSave() {
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
        cleanupCacheFile();
        BitmapHolder.clearAll();
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
