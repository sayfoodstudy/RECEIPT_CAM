package com.receiptcam.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 저장 폴더 안 영수증 사진 격자 뷰. */
public class GalleryActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final LruCache<String, Bitmap> thumbCache =
            new LruCache<String, Bitmap>(8 * 1024 * 1024) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return value.getByteCount();
                }
            };

    private final List<DocumentFile> items = new ArrayList<>();
    private GalleryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        ImageButton btnBack = findViewById(R.id.btnBack);
        GridView gridView = findViewById(R.id.gridView);
        TextView emptyLabel = findViewById(R.id.emptyLabel);

        btnBack.setOnClickListener(v -> finish());

        adapter = new GalleryAdapter();
        gridView.setAdapter(adapter);
        gridView.setEmptyView(emptyLabel);

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            DocumentFile f = items.get(position);
            Intent intent = new Intent(this, ViewerActivity.class);
            intent.putExtra(ViewerActivity.EXTRA_URI, f.getUri().toString());
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        executor.execute(() -> {
            List<DocumentFile> files = StorageHelper.listSavedImages(this);
            runOnUiThread(() -> {
                items.clear();
                items.addAll(files);
                adapter.notifyDataSetChanged();
            });
        });
    }

    private class GalleryAdapter extends BaseAdapter {
        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView cell;
            if (convertView == null) {
                cell = (ImageView) getLayoutInflater()
                        .inflate(R.layout.item_gallery, parent, false);
            } else {
                cell = (ImageView) convertView;
            }

            final DocumentFile doc = items.get(position);
            final String key = doc.getUri().toString();
            cell.setTag(key);

            Bitmap cached = thumbCache.get(key);
            if (cached != null) {
                cell.setImageBitmap(cached);
            } else {
                cell.setImageDrawable(null);
                executor.execute(() -> {
                    Bitmap thumb = ImageUtils.decodeUriSampled(
                            GalleryActivity.this, doc.getUri(), 256);
                    if (thumb == null) return;
                    thumbCache.put(key, thumb);
                    runOnUiThread(() -> {
                        if (key.equals(cell.getTag())) {
                            cell.setImageBitmap(thumb);
                        }
                    });
                });
            }
            return cell;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
