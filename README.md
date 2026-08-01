# 영수증캠 (ReceiptCam) — 영수증 전용 카메라

> **목적**: 영수증 사진 촬영 → 자동 컷 → 1회 선명도 수정 → 저장 경로 분리 (클라우드 제외)
>
> **핵심**: 글자 인식(OCR) 없이 오직 **CUT(영수증 바깥부분 자르기)**만 수행.

## 빠른 시작 (GitHub 기반)

### 1. 클론
```bash
git clone https://github.com/YOUR_USERNAME/receipt_app_project.git
cd receipt_app_project
```

### 2. Flutter 설치 (로컬 개발 시)
- [Flutter 설치 가이드](https://docs.flutter.dev/get-started/install)

### 3. 패키지 설치
```bash
flutter pub get
```

### 4. APK 빌드 (로컬)
```bash
flutter build apk --release
```
빌드 완료 후 `build/app/outputs/flutter-apk/app-release.apk` 확인.

### 5. GitHub Actions로 자동 APK (권장)
이 레포에 `push`하면 `.github/workflows/build_apk.yml`이 자동으로 APK를 빌드하고 Artifacts에 저장합니다.
- **Actions 탭** → 워크플로우 완료 후 **Artifacts** 다운로드 → 폰에 설치

## 앱 사용 흐름

```
[실행] → [카메라 화면 — 일반 카메라와 동일]
  ↓ 촬영
[자동 감지 화면 — 영수증 테두리 제안]
  ↓ 1회 수정 (자르기 보정 / 선명도 슬라이더)
[저장 경로 선택 화면]
  ↓ 저장
[저장 완료 — 폴더에 .nomedia 자동 생성]
```

## 3-1 기능: 자동 컷 (CUT) — OCR 없음
- 촬영 후 `edge_detection` 패키지가 문서 테두리만 감지
- 사용자가 모서리를 1번 조정 가능
- **텍스트 추출 기능 완전 차단** — 글자 인식하지 않음

## 3-2 기능: 저장 경로 분리
- 사용자가 폴더를 선택할 수 있음 (기본: `Download/Receipts`)
- 저장 시 폴더 안에 `.nomedia` 파일을 만들어 Android 미디어 스캐너가 제외하도록 함
- 결과: Google Photos, OneDrive 등의 자동 업로드 방지

## 폴더 구조 안내
```
lib/
├── main.dart
├── screens/
│   ├── camera_screen.dart   # 일반 카메라 UI
│   ├── edit_screen.dart     # 1회 수정 (자르기/선명도)
│   └── save_screen.dart     # 저장 경로 선택
└── services/
    ├── receipt_scanner.dart # 자동 컷 서비스 (OCR 없음)
    └── storage_service.dart # 저장 + .nomedia 처리
```

## 라이선스
MIT — 자유롭게 수정/배포 가능 (단, 이 레포 기반으로 상업 이용 시 원작자 표기 권장)
