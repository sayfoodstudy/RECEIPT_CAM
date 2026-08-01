package com.receiptcam.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Surface;
import android.widget.Button;
import android.widget.ImageButton;
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

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;

public class CameraActivity extends AppCompatActivity {

    public static final String EXTRA_PHOTO_PATH = "photo_path";

    private PreviewView previewView;
    private ImageCapture imageCapture;
    private boolean flashOn = false;
    private Button btnFlash;

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
        ImageButton btnShutter = findViewById(R.id.btnShutter);

        btnShutter.setOnClickListener(v -> takePhoto());
        btnFlash.setOnClickListener(v -> toggleFlash());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
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
        btnFlash.setText(flashOn ? "Flash ON" : "Flash OFF");
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
                        Intent intent = new Intent(CameraActivity.this, EditActivity.class);
                        intent.putExtra(EXTRA_PHOTO_PATH, photoFile.getAbsolutePath());
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(CameraActivity.this,
                                getString(R.string.capture_failed) + exception.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
