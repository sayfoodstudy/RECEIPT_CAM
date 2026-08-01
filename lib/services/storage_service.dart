// 저장 서비스
// 핵심: 사용자가 선택한 폴더 + .nomedia 파일 자동 생성 (클라우드 제외)

import 'dart:io';
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

class StorageService {
  static const String _lastPathKey = 'last_save_path';

  /// 마지막 저장 경로 불러오기
  static Future<String?> getLastSavePath() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_lastPathKey);
  }

  /// 저장 경로 저장
  static Future<void> setLastSavePath(String path) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_lastPathKey, path);
  }

  /// 폴더 내 .nomedia 파일 생성 (Android 미디어 스캐너 제외)
  static Future<bool> createNomedia(String folderPath) async {
    try {
      final nomediaFile = File('$folderPath/.nomedia');
      if (!await nomediaFile.exists()) {
        await nomediaFile.create();
      }
      return true;
    } catch (e) {
      return false;
    }
  }

  /// 최종 저장 함수
  /// [folderPath]: 사용자가 선택한 폴더 (예: /storage/emulated/0/Documents/Receipts)
  /// [sourceImagePath]: 편집 완료된 이미지 경로
  static Future<String?> saveReceipt({
    required String folderPath,
    required String sourceImagePath,
    String fileNamePrefix = 'RECEIPT',
  }) async {
    try {
      // 폴더 존재 확인/생성
      final dir = Directory(folderPath);
      if (!await dir.exists()) {
        await dir.create(recursive: true);
      }

      // .nomedia 생성 (클라우드 업로드 방지)
      await createNomedia(folderPath);

      // 파일명 생성 (시간 기준)
      final now = DateTime.now();
      final fileName = '${fileNamePrefix}_${now.year}${_pad(now.month)}${_pad(now.day)}_${_pad(now.hour)}${_pad(now.minute)}${_pad(now.second)}.jpg';
      final destPath = '$folderPath/$fileName';

      final sourceFile = File(sourceImagePath);
      if (!await sourceFile.exists()) return null;

      await sourceFile.copy(destPath);

      // 마지막 경로 저장
      await setLastSavePath(folderPath);

      return destPath;
    } catch (e) {
      return null;
    }
  }

  static String _pad(int n) => n.toString().padLeft(2, '0');
}
