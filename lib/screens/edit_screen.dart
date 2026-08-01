import 'dart:io';
import 'package:flutter/material.dart';
import 'package:photo_view/photo_view.dart';
import 'package:file_picker/file_picker.dart';
import '../services/receipt_scanner.dart';
import '../services/storage_service.dart';
import 'save_screen.dart';

class EditScreen extends StatefulWidget {
  final String imagePath;
  const EditScreen({super.key, required this.imagePath});

  @override
  State<EditScreen> createState() => _EditScreenState();
}

class _EditScreenState extends State<EditScreen> {
  bool _processing = true;
  String? _croppedPath;
  String? _enhancedPath;
  double _contrast = 1.2;

  @override
  void initState() {
    super.initState();
    _runScan();
  }

  Future<void> _runScan() async {
    // 1) 자동 컷 (OCR 없음 — 테두리 감지만)
    final scanned = await ReceiptScanner.scanReceipt(imagePath: widget.imagePath);
    setState(() {
      _croppedPath = scanned ?? widget.imagePath; // 감지 실패 시 원본
      _processing = false;
    });
  }

  Future<void> _applyEnhance() async {
    if (_croppedPath == null) return;
    setState(() => _processing = true);
    final enhanced = await ReceiptScanner.enhanceImage(
      _croppedPath!,
      contrast: _contrast,
    );
    setState(() {
      _enhancedPath = enhanced ?? _croppedPath;
      _processing = false;
    });
  }

  void _goToSave() {
    if (_enhancedPath == null && _croppedPath == null) return;
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => SaveScreen(
          imagePath: _enhancedPath ?? _croppedPath ?? widget.imagePath,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final displayPath = _enhancedPath ?? _croppedPath ?? widget.imagePath;

    return Scaffold(
      appBar: AppBar(title: const Text('수정 (1회)')),
      body: _processing
          ? const Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [CircularProgressIndicator(), SizedBox(height: 16), Text('영수증 테두리 감지 중...')]))
          : Column(
              children: [
                Expanded(
                  child: Container(
                    color: Colors.black,
                    child: PhotoView(
                      imageProvider: FileImage(File(displayPath)),
                      minScale: PhotoViewComputedScale.contained,
                      maxScale: 4.0,
                    ),
                  ),
                ),
                // 1회 수정 영역
                Container(
                  padding: const EdgeInsets.all(16),
                  color: Colors.white,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text('선명도 / 대비', style: Theme.of(context).textTheme.titleSmall),
                          ),
                          Text(_contrast.toStringAsFixed(1)),
                        ],
                      ),
                      Slider(
                        value: _contrast,
                        min: 0.8,
                        max: 2.0,
                        divisions: 6,
                        label: _contrast.toStringAsFixed(1),
                        onChanged: (val) => setState(() => _contrast = val),
                        onChangeEnd: (_) => _applyEnhance(),
                      ),
                      const SizedBox(height: 12),
                      Row(
                        children: [
                          Expanded(
                            child: FilledButton.icon(
                              onPressed: _applyEnhance,
                              icon: const Icon(Icons.enhance_photo_outlined),
                              label: const Text('선명도 적용'),
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: FilledButton.icon(
                              style: FilledButton.styleFrom(backgroundColor: Colors.grey[800]),
                              onPressed: _goToSave,
                              icon: const Icon(Icons.save),
                              label: const Text('저장'),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      const Text('※ 글자 인식(OCR)은 수행하지 않습니다. 오직 컷(자르기)만 합니다.', style: TextStyle(fontSize: 11, color: Colors.red)),
                    ],
                  ),
                ),
              ],
            ),
    );
  }
}
