package com.receiptcam.app;

import android.graphics.Bitmap;

/**
 * Activity 간 큰 비트맵 전달용 임시 보관소 (Intent 크기 제한 회피).
 * 단일 프로세스 앱 전용.
 */
public final class BitmapHolder {

    /** 디코딩된 원본 사진 (수동 컷 화면의 소스). */
    public static Bitmap photo;

    /** 마지막 자동 감지 모서리 (수동 컷의 초기 가이드). null 가능. */
    public static float[] lastDetectedCorners;

    /** 수동 컷 결과 비트맵. */
    public static Bitmap croppedResult;

    private BitmapHolder() {}

    public static void clearAll() {
        photo = null;
        lastDetectedCorners = null;
        croppedResult = null;
    }
}
