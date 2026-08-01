# ReceiptCam

영수증 사진 촬영 → 자동 컷(원근 보정) → 선명도 조절 → 선택한 폴더에 저장하는 개인용 Android 앱.
OCR(글자 인식)은 하지 않으며, 사진은 폰 안의 지정 폴더에만 저장됩니다(클라우드 자동 동기화 없음).

- 언어: Java 100%
- compileSdk 35 / minSdk 26 / targetSdk 35
- 빌드: GitHub Actions (PC에 Android Studio 설치 불필요)

## 앱 사용 흐름

1. 첫 실행 → **저장 폴더 선택** (갤러리 앱의 폴더 또는 내 파일의 폴더 아무거나)
2. **영수증 촬영** → 카메라 화면에서 촬영 (Flash ON/OFF 가능)
3. 자동으로 영수증 영역을 감지해 컷 + 원근 보정 → **선명도** 슬라이더 조절
   - 감지 실패 시 전체 사진으로 진행, `원본 보기` 버튼으로 원본/자동컷 전환 가능
4. **저장** → `ReceiptCam_yyyyMMdd_HHmmss.jpg` 형식으로 선택한 폴더에 저장

## GitHub 업로드 → Actions 빌드 → 폰 설치 (PC에서)

1. GitHub 계정 생성/로그인 → 우측 상단 **+** → **New repository**
   - 이름: `ReceiptCam`, Private 추천(개인용), **Add a README 체크 해제** 후 생성
2. 생성된 레포 페이지에서 **"uploading an existing file"** 링크 클릭
3. 이 프로젝트 폴더(ReceiptCam) 안의 **모든 파일과 폴더를 드래그&드롭**
   - ⚠️ `.github` 폴더와 `.gitignore` 파일이 꼭 포함되어야 합니다
   - Mac Finder는 점(.)으로 시작하는 항목이 숨겨져 있으니 `Cmd + Shift + .` 를 눌러 표시
   - Windows 탐색기는 그대로 보입니다
4. **Commit changes** 클릭 → Actions 탭에서 자동으로 빌드 시작 (약 3~5분)
5. 빌드 완료(녹색 체크) 후 해당 실행(run)을 클릭 → 하단 **Artifacts → ReceiptCam-v0.1-apk** 다운로드
   - 다운로드 파일은 zip이며 안에 `ReceiptCam-v0.1.apk`가 들어 있습니다

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
│   ├── build.gradle                  # SDK/의존성/버전 (versionCode 1, versionName "0.1")
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/receiptcam/app/
│       │   ├── MainActivity.java     # 폴더 선택 + 촬영 시작
│       │   ├── CameraActivity.java   # CameraX 촬영
│       │   ├── EditActivity.java     # 자동 컷 + 선명도 + 저장
│       │   ├── ReceiptDetector.java  # 영수증 영역 감지 + 원근 보정 (순수 Java)
│       │   ├── ImageEnhancer.java    # 선명도(언샤프 마스크)
│       │   ├── ImageUtils.java       # 디코딩/회전/스케일
│       │   └── StorageHelper.java    # SAF 폴더 저장
│       └── res/…                     # 레이아웃, 문자열, 아이콘
├── build.gradle, settings.gradle, gradle.properties
└── CHANGELOG.md
```

## 자동 컷 알고리즘 (v0.1)

밝은 영수증 종이를 어두운 배경에서 분리(간단한 이진화) → 볼록 껍질(convex hull) →
최소 면적 회전 사각형 → 4점 원근 변환으로 직사각형 보정.
어두운 배경 위에 영수증을 놓고 찍으면 가장 잘 동작합니다.

## 변경 규칙 (Regression 방지)

- 기존 기능은 건드리지 않고, 요청받은 변경/추가만 수행
- 변경 시 `versionCode` +1, `versionName` 갱신, CHANGELOG.md 기록
