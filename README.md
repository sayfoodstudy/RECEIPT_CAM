# ReceiptCam

영수증 사진 촬영 → 자동/수동 컷(원근 보정) → 밝기·대비·선명도 조정 → 선택한 폴더에 저장하는 개인용 Android 앱.
OCR(글자 인식)은 하지 않으며, 사진은 폰 안의 지정 폴더에만 저장됩니다(클라우드 자동 동기화 없음).

- 언어: Java 100%
- compileSdk 35 / minSdk 26 / targetSdk 35
- 빌드: GitHub Actions (PC에 Android Studio 설치 불필요)

## 앱 사용 흐름 (v0.2, 삼성 카메라 스타일)

1. 앱을 켜면 바로 **촬영 화면** (좌하단: 마지막 사진 원형 썸네일 → 갤러리, 좌상단: 설정 톱니바퀴, 우상단: 플래시)
2. 처음 사용 시: 설정(톱니바퀴) → **저장 폴더 변경**에서 폴더 선택 (한 번만)
3. 영수증 촬영 → 자동으로 모서리를 감지해 컷 + 원근 보정 후 편집 화면 표시
   - **수동 컷** 버튼: 4개 모서리 원을 드래그해 영수증 가장자리에 직접 맞춘 뒤 ✓ 완료
   - 감지 실패 시 전체 사진으로 진행, `원본 보기`로 원본/컷 전환 가능
4. 화면 아래 **밝기 / 대비 / 선명도** 슬라이더로 글자가 선명하게 보이도록 조정
5. **저장** → `ReceiptCam_yyyyMMdd_HHmmss.jpg` 형식으로 선택한 폴더에 저장

## GitHub 업로드 → Actions 빌드 → 폰 설치 (PC에서)

1. GitHub 계정 생성/로그인 → 우측 상단 **+** → **New repository**
   - 이름: `ReceiptCam`, Private 추천(개인용), **Add a README 체크 해제** 후 생성
2. 생성된 레포 페이지에서 **"uploading an existing file"** 링크 클릭
3. 이 프로젝트 폴더(ReceiptCam) 안의 **모든 파일과 폴더를 드래그&드롭**
   - ⚠️ `.github` 폴더와 `.gitignore` 파일이 꼭 포함되어야 합니다
   - 점(.)으로 시작하는 파일/폴더는 웹 업로드에서 누락될 수 있습니다 → 누락 시 **Add file → Create new file**로 직접 생성
4. **Commit changes** 클릭 → Actions 탭에서 자동으로 빌드 시작 (약 3~5분)
5. 빌드 완료(녹색 체크) 후 해당 실행(run)을 클릭 → 하단 **Artifacts → ReceiptCam-v0.2-apk** 다운로드
   - 다운로드 파일은 zip이며 안에 `ReceiptCam-v0.2.apk`가 들어 있습니다

### 삼성 폰에 설치

- 방법 A: 폰 브라우저에서 GitHub 로그인 → 레포 → Actions → 최근 실행 → Artifacts 다운로드 → 내 파일 앱에서 압축 해제 → APK 탭 → "출처를 알 수 없는 앱 설치" 허용 → 설치
- 방법 B: PC에서 다운로드 후 USB/Quick Share로 폰에 전송 → 설치

이후 코드를 수정해 `main` 브랜치에 올리면 Actions가 자동으로 새 APK를 빌드합니다.
(Actions 탭 → **Build APK → Run workflow** 로 수동 실행도 가능)

## 프로젝트 구조

```
ReceiptCam/
├── .github/workflows/build-apk.yml   # GitHub Actions 빌드 설정
├── app/
│   ├── build.gradle                  # SDK/의존성/버전 (versionCode 2, versionName "0.2")
│   └── src/main/
│       ├── AndroidManifest.xml       # 런처 = CameraActivity
│       ├── java/com/receiptcam/app/
│       │   ├── CameraActivity.java   # 런처: 촬영 화면 (썸네일/설정/플래시)
│       │   ├── EditActivity.java     # 자동/수동 컷 결과 + 밝기/대비/선명도 + 저장
│       │   ├── CropActivity.java     # 수동 모서리 컷
│       │   ├── CropOverlayView.java  # 드래그 가능 모서리 오버레이
│       │   ├── GalleryActivity.java  # 저장 사진 격자 뷰
│       │   ├── ViewerActivity.java   # 전체화면 뷰어
│       │   ├── ZoomableImageView.java# 핀치 줌 이미지 뷰
│       │   ├── SettingsActivity.java # 설정 (저장 폴더 등)
│       │   ├── ReceiptDetector.java  # 영수증 영역 감지 + 원근 보정 (순수 Java)
│       │   ├── ImageEnhancer.java    # 선명도 + 밝기/대비
│       │   ├── ImageUtils.java       # 디코딩/회전/스케일
│       │   ├── StorageHelper.java    # SAF 폴더 저장/목록
│       │   └── BitmapHolder.java     # 화면 간 비트맵 전달
│       └── res/…                     # 레이아웃, 문자열, 아이콘
├── build.gradle, settings.gradle, gradle.properties
└── CHANGELOG.md
```

## 자동 컷 알고리즘

밝은 영수증 종이를 어두운 배경에서 분리(간단한 이진화) → 볼록 껍질(convex hull) →
최소 면적 회전 사각형 → 4점 원근 변환으로 직사각형 보정.
어두운 배경 위에 영수증을 놓고 찍으면 가장 잘 동작합니다. 부정확하면 **수동 컷**으로 맞추세요.

## 변경 규칙 (Regression 방지)

- 기존 기능은 건드리지 않고, 요청받은 변경/추가만 수행
- 변경 시 `versionCode` +1, `versionName` 갱신, CHANGELOG.md 기록
