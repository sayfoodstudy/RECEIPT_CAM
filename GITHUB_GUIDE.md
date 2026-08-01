# GitHub 업로드 → APK 완성 가이드

## 1. 레포 생성 (GitHub)
1. GitHub에서 새 레포지토리 만들기 (이름: receipt-cam 등)
2. 이 폴더 내용 전체를 푸시:
```bash
git init
git add .
git commit -m "init: receipt cam skeleton"
git remote add origin https://github.com/YOUR_USERNAME/receipt-cam.git
git branch -M main
git push -u origin main
```

## 2. GitHub Actions로 APK 자동 빌드
- `push`하면 `.github/workflows/build_apk.yml`이 자동 실행됨
- **Actions 탭** → `Build APK` → 완료 후 **Artifacts** 다운로드
- APK는 `build/app/outputs/flutter-apk/app-release.apk`

## 3. 로컬에서 테스트 (선택)
```bash
flutter pub get
flutter build apk --release
```

## 4. 반드시 확인해야 할 수정사항 (AI 코드이므로 확인 필수)
### A. 자동 컷 패키지 (`edge_detection`)
- `lib/services/receipt_scanner.dart`의 `EdgeDetection.detectEdge()`는 패키지 실제 API와 다를 수 있음
- `pubspec.yaml`의 `edge_detection` 버전을 확인하고 실제 메서드명으로 변경 필요
- 대안: `google_mlkit_document_scanner`를 사용하되 OCR 옵션 명시적 해제

### B. 저장 경로 / Android 권한
- Android 10+ (API 30 이상)에서 임의 폴더 저장은 `MANAGE_EXTERNAL_STORAGE` 권한이 필요할 수 있음
- `android/app/src/main/AndroidManifest.xml`에 추가:
```xml
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
```
- 앱 설정에서 '모든 파일 액세스 권한' 허용 필요

### C. `.nomedia` 생성
- `StorageService.createNomedia()`는 이미 구현됨
- 폴더 선택 후 저장하면 자동 생성됨

### D. OCR 차단 확인
- `ReceiptScanner.scanReceipt()`에서 `ocrEnabled = false`로 강제함
- 추가로 `google_mlkit_text_recognition` 등이 `pubspec.yaml`에 없도록 확인 (없음)

## 5. 핵심 기능 점검 리스트 (3-1 / 3-2)
- [ ] 일반 카메라처럼 촬영되는가?
- [ ] 촬영 후 **1회**만 수정 화면이 뜨는가?
- [ ] 수정 시 **글자 인식 없이** 영수증 테두리만 자르는가?
- [ ] 선명도/대비 조절이 가능한가?
- [ ] 저장 시 **폴더 선택**이 가능한가?
- [ ] 저장 후 `.nomedia`가 생성되어 클라우드 동기화가 방지되는가?

## 6. 다음 단계 제안
1. 위 코드로 1차 APK 빌드 및 테스트
2. `edge_detection` 실제 API 확인 후 `receipt_scanner.dart` 수정
3. Android 권한(`MANAGE_EXTERNAL_STORAGE`) 추가
4. 원본 카메라 UX가 부족하면 `camerawesome` 패키지로 교체 가능

궁금한 점이나 빌드 오류가 나면 이 레포 기준으로 바로 수정하겠습니다.
