package com.receiptcam.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

/** 핀치 줌 + 드래그 이동이 가능한 간단한 이미지 뷰. */
public class ZoomableImageView extends AppCompatImageView {

    private final Matrix mat = new Matrix();
    private ScaleGestureDetector scaleDetector;
    private float currentScale = 1f;
    private float lastX, lastY;
    private boolean scaling = false;

    public ZoomableImageView(Context context) {
        super(context);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        float target = currentScale * detector.getScaleFactor();
                        target = Math.max(1f, Math.min(target, 6f));
                        float ratio = target / currentScale;
                        mat.postScale(ratio, ratio, detector.getFocusX(), detector.getFocusY());
                        currentScale = target;
                        setImageMatrix(mat);
                        return true;
                    }
                });
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        post(this::fitToCenter);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        post(this::fitToCenter);
    }

    /** 이미지를 화면 중앙에 맞추고 줌 상태 초기화. */
    public void fitToCenter() {
        Drawable d = getDrawable();
        if (d == null || getWidth() == 0 || getHeight() == 0) return;
        int dw = d.getIntrinsicWidth();
        int dh = d.getIntrinsicHeight();
        if (dw <= 0 || dh <= 0) return;
        float scale = Math.min(getWidth() / (float) dw, getHeight() / (float) dh);
        float dx = (getWidth() - dw * scale) / 2f;
        float dy = (getHeight() - dh * scale) / 2f;
        mat.reset();
        mat.setScale(scale, scale);
        mat.postTranslate(dx, dy);
        currentScale = 1f;
        setImageMatrix(mat);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        scaling = scaleDetector.isInProgress();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!scaling && event.getPointerCount() == 1 && currentScale > 1f) {
                    mat.postTranslate(event.getX() - lastX, event.getY() - lastY);
                    setImageMatrix(mat);
                }
                lastX = event.getX();
                lastY = event.getY();
                break;
        }
        return true;
    }
}
