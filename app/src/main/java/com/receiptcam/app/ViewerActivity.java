package com.receiptcam.app;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 저장된 영수증 전체화면 뷰어 (핀치 줌 지원). */
public class ViewerActivity extends AppCompatActivity {

    public static final String EXTRA_URI = "image_uri";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ZoomableImageView viewerImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);

        viewerImage = findViewById(R.id.viewerImage);
        ImageButton btnClose = findViewById(R.id.btnCloseViewer);
        btnClose.setOnClickListener(v -> finish());

        String uriStr = getIntent().getStringExtra(EXTRA_URI);
        if (uriStr == null) {
            finish();
            return;
        }
        Uri uri = Uri.parse(uriStr);

        executor.execute(() -> {
            Bitmap bmp = ImageUtils.decodeUriSampled(this, uri, 2400);
            runOnUiThread(() -> {
                if (bmp == null) {
                    finish();
                } else {
                    viewerImage.setImageBitmap(bmp);
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
