# 영수증캠 (ReceiptCam) — 설계 문서

## 1. 프로젝트 개요
- **목적**: 영수증 사진 촬영 → 1회 편집(자동 컷 + 선명도) → 저장
- **핵심 차별점**: 기존 스캐너 앱처럼 **영수증 안의 글자(OCR)를 인식하지 않음**. 오직 **영수증 바깥부분(CUT)**만 분리하고 저장.
- **저장 전략**: 사용자가 원하는 폴더 선택 + `.nomedia` 파일 자동 생성 → Google Photos 등 클라우드 동기화 제외.

## 2. 플랫폼 / 기술 스택
- **프레임워크**: Flutter (Dart) — 단일 코드로 Android APK 빌드 가능, GitHub Actions 연동 쉬움
- **저장소**: GitHub (이 레포 자체)
- **빌드**: GitHub Actions → `flutter build apk` → Artifact로 APK 다운로드
- **카메라**: `camera` 패키지 (기본 카메라 UI와 동일한 경험)
- **자동 컷(문서 감지)**: `edge_detection` 패키지 (OpenCV 기반 문서 가장자리 감지, OCR 없음)
- **선명도/대비**: `image` 패키지 (contrast, sharpen)
- **저장 경로/권한**: `path_provider` + `permission_handler`

## 3. 앱 흐름 (3-1 요구사항)
```
[앱 실행] → [카메라 화면] → [촬영] → [자동 문서 감지/자르기 화면] → [1회 수정(자르기 보정 + 선명도)] → [저장 경로 선택] → [저장 완료]
```
- **수정은 딱 1회만**: 촬영 후 한 번만 편집 화면이 열리고, 저장 시 바로 저장됨.
- **OCR 차단**: `edge_detection` 사용 시 텍스트 추출 기능 완전 비활성화. 오직 테두리 좌표만 추출.

## 4. 저장 위치 구조 (3-2 요구사항)
- 사용자가 폴더 선택 (예: `/Download/Receipts`, `/Documents/Work`)
- 저장 시 해당 폴더 안에 `.nomedia` 파일 자동 생성
- `.nomedia`가 있으면 Android 미디어 스캐너가 폴더 내 사진을 인덱싱하지 않아 Google Photos, OneDrive 등의 자동 업로드를 방지함
- 추가로 앱 내부 `SharedPreferences`에 마지막 저장 경로를 저장하여 재사용 가능

## 5. 폴더 구조 (GitHub 레포)
```
receipt_app_project/
├── .github/
│   └── workflows/
│       └── build_apk.yml      # GitHub Actions: APK 빌드
├── android/                   # Flutter 기본
├── ios/
├── lib/
│   ├── main.dart
│   ├── screens/
│   │   ├── camera_screen.dart
│   │   ├── edit_screen.dart
│   │   └── save_screen.dart
│   ├── services/
│   │   ├── receipt_scanner.dart   # 자동 컷 로직 (OCR 없음)
│   │   └── storage_service.dart   # 저장 경로/ .nomedia 처리
│   └── widgets/
├── pubspec.yaml
├── README.md                  # GitHub 사용 가이드
└── design.md                  # 이 문서
```

## 6. 핵심 기능 상세

### 6-1 자동 컷 (Edge Detection)
- `edge_detection` 패키지 사용
- 촬영 후 이미지 분석 → 가장 큰 사각형(영수증) 감지 → 자르기 제안
- 사용자가 모서리를 1번만 조정 가능 (수정 단계)

### 6-2 선명도 수정 (No OCR)
- `image` 패키지 사용: `contrast` + `sharpen`
- 사용자가 슬라이더로 1단계 조절 가능 (선명도 정도)

### 6-3 저장 경로 분리
- `StorageService.pickFolder()` → 파일 선택기
- 저장 시 `File('$path/.nomedia').create()` 실행
- 파일명: `RECEIPT_YYYYMMDD_HHMMSS.jpg`

## 7. GitHub 워크플로우 (APK 완성까지)
1. 이 레포를 클론
2. `flutter pub get`
3. `flutter build apk --release`
4. GitHub Actions: Push 시 자동 APK 빌드 → Artifacts에서 다운로드
5. APK 설치 후 테스트

## 8. 주의사항 / 제한
- iOS는 본 프로젝트 범위 아님 (APK 중심)
- 자동 컷이 100% 완벽하지 않음 — 1회 보정 단계로 커버
- `.nomedia`는 Android 미디어 스캐너 기준이므로 일부 클라우드 앱(삼성 클라우드 등)은 별도 제외 설정 필요할 수 있음
