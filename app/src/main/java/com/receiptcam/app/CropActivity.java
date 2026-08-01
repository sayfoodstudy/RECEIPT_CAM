package com.receiptcam.app;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 수동 컷: 사진 위 4개 모서리를 드래그해 맞춘 뒤 원근 보정. */
public class CropActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private CropOverlayView cropView;
    private ImageButton btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bitmap photo = BitmapHolder.photo;
        if (photo == null) {
            finish();
            return;
        }
        setContentView(R.layout.activity_crop);

        cropView = findViewById(R.id.cropView);
        btnConfirm = findViewById(R.id.btnConfirmCrop);
        ImageButton btnCancel = findViewById(R.id.btnCancelCrop);

        cropView.setImage(photo);

        // 초기 가이드: 자동 감지 모서리, 없으면 화면 안쪽 12% 사각형
        float[] init = BitmapHolder.lastDetectedCorners;
        if (init == null) {
            int w = photo.getWidth(), h = photo.getHeight();
            init = new float[]{
                    w * 0.12f, h * 0.12f,
                    w * 0.88f, h * 0.12f,
                    w * 0.88f, h * 0.88f,
                    w * 0.12f, h * 0.88f
            };
        }
        cropView.setCorners(init);

        btnCancel.setOnClickListener(v -> finish());
        btnConfirm.setOnClickListener(v -> confirmCrop());
    }

    private void confirmCrop() {
        Bitmap photo = BitmapHolder.photo;
        if (photo == null) {
            finish();
            return;
        }
        btnConfirm.setEnabled(false);
        final float[] corners = cropView.getCorners();
        executor.execute(() -> {
            try {
                Bitmap result = ReceiptDetector.warpToRect(photo, corners);
                BitmapHolder.croppedResult = result;
                runOnUiThread(() -> {
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (IllegalArgumentException | OutOfMemoryError e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.saved_fail, Toast.LENGTH_SHORT).show();
                    btnConfirm.setEnabled(true);
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
