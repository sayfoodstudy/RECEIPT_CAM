package com.receiptcam.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * 사진 위에 4개의 모서리 핸들을 드래그해 컷 영역을 맞추는 커스텀 뷰.
 * 모서리 순서: tl, tr, br, bl (비트맵 좌표계).
 */
public class CropOverlayView extends View {

    private Bitmap bitmap;
    private final float[] corners = new float[8];
    private boolean cornersReady = false;

    private final Matrix imgToView = new Matrix();
    private final Matrix viewToImg = new Matrix();
    private int viewW, viewH;

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handleFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handleRing = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dimPaint = new Paint();

    private int activeHandle = -1;
    private final float touchRadius;
    private final float handleRadius;

    public CropOverlayView(Context context) {
        this(context, null);
    }

    public CropOverlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CropOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        float density = getResources().getDisplayMetrics().density;
        touchRadius = 44 * density;
        handleRadius = 13 * density;

        linePaint.setColor(0xFF26A69A);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2.5f * density);

        handleFill.setColor(0xFFFFFFFF);
        handleFill.setStyle(Paint.Style.FILL);

        handleRing.setColor(0xFF26A69A);
        handleRing.setStyle(Paint.Style.STROKE);
        handleRing.setStrokeWidth(3 * density);

        dimPaint.setColor(0x66000000);
    }

    public void setImage(Bitmap bmp) {
        bitmap = bmp;
        computeMatrices();
        invalidate();
    }

    /** 모서리 4개(비트맵 좌표, tl tr br bl) 설정. */
    public void setCorners(float[] c) {
        System.arraycopy(c, 0, corners, 0, 8);
        cornersReady = true;
        invalidate();
    }

    /** 현재 모서리 좌표(비트맵 좌표, tl tr br bl) 반환. */
    public float[] getCorners() {
        float[] out = new float[8];
        System.arraycopy(corners, 0, out, 0, 8);
        return out;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewW = w;
        viewH = h;
        computeMatrices();
    }

    private void computeMatrices() {
        if (bitmap == null || viewW == 0 || viewH == 0) return;
        float scale = Math.min(viewW / (float) bitmap.getWidth(),
                viewH / (float) bitmap.getHeight());
        float dx = (viewW - bitmap.getWidth() * scale) / 2f;
        float dy = (viewH - bitmap.getHeight() * scale) / 2f;
        imgToView.reset();
        imgToView.setScale(scale, scale);
        imgToView.postTranslate(dx, dy);
        imgToView.invert(viewToImg);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null || !cornersReady) return;

        canvas.drawBitmap(bitmap, imgToView, null);

        float[] v = new float[8];
        imgToView.mapPoints(v, corners);

        // 영역 바깥 어둡게 (even-odd)
        Path dim = new Path();
        dim.setFillType(Path.FillType.EVEN_ODD);
        dim.addRect(0, 0, viewW, viewH, Path.Direction.CW);
        dim.moveTo(v[0], v[1]);
        dim.lineTo(v[2], v[3]);
        dim.lineTo(v[4], v[5]);
        dim.lineTo(v[6], v[7]);
        dim.close();
        canvas.drawPath(dim, dimPaint);

        // 가이드 선
        Path quad = new Path();
        quad.moveTo(v[0], v[1]);
        quad.lineTo(v[2], v[3]);
        quad.lineTo(v[4], v[5]);
        quad.lineTo(v[6], v[7]);
        quad.close();
        canvas.drawPath(quad, linePaint);

        // 모서리 핸들
        for (int i = 0; i < 4; i++) {
            canvas.drawCircle(v[i * 2], v[i * 2 + 1], handleRadius, handleFill);
            canvas.drawCircle(v[i * 2], v[i * 2 + 1], handleRadius, handleRing);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (bitmap == null || !cornersReady) return false;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                float[] v = new float[8];
                imgToView.mapPoints(v, corners);
                activeHandle = -1;
                float best = touchRadius;
                for (int i = 0; i < 4; i++) {
                    float dx = event.getX() - v[i * 2];
                    float dy = event.getY() - v[i * 2 + 1];
                    float d = (float) Math.sqrt(dx * dx + dy * dy);
                    if (d <= best) {
                        best = d;
                        activeHandle = i;
                    }
                }
                return activeHandle >= 0;
            }
            case MotionEvent.ACTION_MOVE: {
                if (activeHandle < 0) return false;
                float[] pt = {event.getX(), event.getY()};
                viewToImg.mapPoints(pt);
                pt[0] = Math.max(0, Math.min(pt[0], bitmap.getWidth() - 1));
                pt[1] = Math.max(0, Math.min(pt[1], bitmap.getHeight() - 1));
                corners[activeHandle * 2] = pt[0];
                corners[activeHandle * 2 + 1] = pt[1];
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                activeHandle = -1;
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }
}
