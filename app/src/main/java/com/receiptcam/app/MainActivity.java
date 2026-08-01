package com.receiptcam.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView folderLabel;
    private TextView countLabel;
    private Button btnCapture;

    private final ActivityResultLauncher<Uri> folderPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), uri -> {
                if (uri == null) return;
                try {
                    getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                } catch (SecurityException e) {
                    try {
                        getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException ignored) { }
                }
                StorageHelper.setTreeUri(this, uri);
                refreshUi();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        folderLabel = findViewById(R.id.folderLabel);
        countLabel = findViewById(R.id.countLabel);
        btnCapture = findViewById(R.id.btnCapture);
        Button btnPickFolder = findViewById(R.id.btnPickFolder);

        btnPickFolder.setOnClickListener(v -> folderPicker.launch(null));
        btnCapture.setOnClickListener(v -> {
            if (StorageHelper.getTreeUri(this) == null) {
                Toast.makeText(this, R.string.no_folder, Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(this, CameraActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUi();
    }

    private void refreshUi() {
        String name = StorageHelper.folderName(this);
        if (name == null) {
            folderLabel.setText(R.string.folder_not_selected);
            countLabel.setText("");
            btnCapture.setEnabled(false);
            btnCapture.setAlpha(0.5f);
        } else {
            folderLabel.setText(getString(R.string.folder_selected_prefix) + name);
            int count = StorageHelper.countSavedImages(this);
            countLabel.setText(getString(R.string.saved_count, count));
            btnCapture.setEnabled(true);
            btnCapture.setAlpha(1.0f);
        }
    }
}
