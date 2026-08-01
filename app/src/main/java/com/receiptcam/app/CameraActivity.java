package com.receiptcam.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 런처 화면: 삼성 카메라 스타일 촬영 화면. */
public class CameraActivity extends AppCompatActivity {

    public static final String EXTRA_PHOTO_PATH = "photo_path";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private PreviewView previewView;
    private ImageCapture imageCapture;
    private ImageButton btnFlash;
    private ImageView thumbImage;
    private TextView bannerFolder;

    private boolean flashOn = false;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    Toast.makeText(this, R.string.camera_permission_needed, Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.previewView);
        btnFlash = findViewById(R.id.btnFlash);
        thumbImage = findViewById(R.id.thumbImage);
        bannerFolder = findViewById(R.id.bannerFolder);
        FrameLayout thumbWrapper = findViewById(R.id.thumbWrapper);
        ImageButton btnShutter = findViewById(R.id.btnShutter);
        ImageButton btnSettings = findViewById(R.id.btnSettings);

        btnShutter.setOnClickListener(v -> takePhoto());
        btnFlash.setOnClickListener(v -> toggleFlash());
        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        thumbWrapper.setOnClickListener(v ->
                startActivity(new Intent(this, GalleryActivity.class)));
        bannerFolder.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bannerFolder.setVisibility(
                StorageHelper.getTreeUri(this) == null ? View.VISIBLE : View.GONE);
        refreshThumbnail();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                int rotation = previewView.getDisplay() != null
                        ? previewView.getDisplay().getRotation()
                        : Surface.ROTATION_0;
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setFlashMode(flashOn
                                ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF)
                        .setTargetRotation(rotation)
                        .build();

                provider.unbindAll();
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA,
                        preview, imageCapture);
            } catch (Exception e) {
                Toast.makeText(this, R.string.capture_failed, Toast.LENGTH_SHORT).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void toggleFlash() {
        flashOn = !flashOn;
        btnFlash.setImageResource(R.drawable.ic_flash);
        btnFlash.setColorFilter(flashOn ? 0xFFFFD54F : 0xFFFFFFFF);
        if (imageCapture != null) {
            imageCapture.setFlashMode(flashOn
                    ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF);
        }
    }

    private void takePhoto() {
        if (imageCapture == null) return;
        File photoFile = new File(getCacheDir(),
                "receipt_raw_" + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(options, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(
                            @NonNull ImageCapture.OutputFileResults outputFileResults) {
                        // 주의: 런처 화면이므로 finish() 하지 않음 — 편집 화면 종료 시 복귀
                        Intent intent = new Intent(CameraActivity.this, EditActivity.class);
                        intent.putExtra(EXTRA_PHOTO_PATH, photoFile.getAbsolutePath());
                        startActivity(intent);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(CameraActivity.this,
                                getString(R.string.capture_failed) + exception.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** 마지막 저장 사진을 원형 썸네일 버튼에 표시 (삼성 카메라 스타일). */
    private void refreshThumbnail() {
        executor.execute(() -> {
            DocumentFile newest = StorageHelper.findNewestImage(this);
            Bitmap thumb = newest == null
                    ? null
                    : ImageUtils.decodeUriSampled(this, newest.getUri(), 240);
            runOnUiThread(() -> {
                if (thumb != null) {
                    thumbImage.setPadding(0, 0, 0, 0);
                    thumbImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    thumbImage.setImageBitmap(thumb);
                } else {
                    int pad = (int) (14 * getResources().getDisplayMetrics().density);
                    thumbImage.setPadding(pad, pad, pad, pad);
                    thumbImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    thumbImage.setImageResource(R.drawable.ic_image);
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
