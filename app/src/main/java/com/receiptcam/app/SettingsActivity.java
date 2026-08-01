package com.receiptcam.app;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

/** 설정: 저장 폴더 선택/변경 + 상태 표시. */
public class SettingsActivity extends AppCompatActivity {

    private TextView folderNameLabel;
    private TextView countLabel;

    private final ActivityResultLauncher<Uri> folderPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), uri -> {
                if (uri == null) return;
                StorageHelper.persistTreePermission(this, uri);
                StorageHelper.setTreeUri(this, uri);
                refreshUi();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        folderNameLabel = findViewById(R.id.folderNameLabel);
        countLabel = findViewById(R.id.countLabel);
        ImageButton btnBack = findViewById(R.id.btnBack);
        Button btnChangeFolder = findViewById(R.id.btnChangeFolder);

        btnBack.setOnClickListener(v -> finish());
        btnChangeFolder.setOnClickListener(v -> folderPicker.launch(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUi();
    }

    private void refreshUi() {
        String name = StorageHelper.folderName(this);
        folderNameLabel.setText(name == null
                ? getString(R.string.folder_none) : name);
        int count = StorageHelper.countSavedImages(this);
        countLabel.setText(getString(R.string.saved_count, count));
    }
}
