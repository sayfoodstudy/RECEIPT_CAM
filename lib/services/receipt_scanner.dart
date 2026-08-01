// 영수증 스캐너 서비스
// 핵심: OCR 없이 오직 문서(영수증) 테두리 감지 + 자르기만 수행

import 'dart:io';
import 'package:edge_detection/edge_detection.dart';
import 'package:image/image.dart' as img_lib;
import 'package:path_provider/path_provider.dart';

class ReceiptScanner {
  /// 촬영된 이미지 경로를 받아 영수증 테두리 감지 후 자른 이미지 경로 반환
  /// [ocrEnabled]는 이 앱에서 항상 false (글자 인식 금지)
  static Future<String?> scanReceipt({
    required String imagePath,
    bool ocrEnabled = false, // 반드시 false
  }) async {
    try {
      // edge_detection 패키지: 문서 가장자리만 감지
      // 이 메서드는 이미지 내 최대 사각형(문서)을 찾아 자름
      final result = await EdgeDetection.detectEdge(
        imagePath,
        canUseGallery: false,
        edgeAlgorithm: EdgeAlgorithm.Canny,
        // OCR 관련 옵션은 사용하지 않음 (패키지 자체 OCR 기능 비활성화)
      );

      if (result != null && result.isSuccess) {
        // 자른 이미지 저장 경로 (임시)
        final tempDir = await getTemporaryDirectory();
        final croppedPath = '${tempDir.path}/cropped_receipt.jpg';
        // 실제로는 edge_detection 결과 경로를 사용하거나 파일 복사
        // 여기서는 개념적 코드로 처리
        return result.croppedPath ?? croppedPath;
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  /// 1회 수정: 선명도/대비 조절 (OCR 없이 이미지 처리만)
  static Future<String?> enhanceImage(String imagePath, {double contrast = 1.2}) async {
    try {
      final bytes = await File(imagePath).readAsBytes();
      final original = img_lib.decodeImage(bytes);
      if (original == null) return null;

      // 대비 조절 (선명도 향상)
      final enhanced = img_lib.adjustColor(original, contrast: contrast);
      // 샤프닝(선명도) 효과 추가 가능
      final sharpened = img_lib.convolve(enhanced, [0, -1, 0, -1, 5, -1, 0, -1, 0]);

      final dir = await getTemporaryDirectory();
      final outPath = '${dir.path}/enhanced_receipt.jpg';
      await File(outPath).writeAsBytes(img_lib.encodeJpg(sharpened, quality: 95));
      return outPath;
    } catch (e) {
      return null;
    }
  }
}
