import 'dart:io';
import 'package:flutter/material.dart';
import 'package:file_picker/file_picker.dart';
import '../services/storage_service.dart';

class SaveScreen extends StatefulWidget {
  final String imagePath;
  const SaveScreen({super.key, required this.imagePath});

  @override
  State<SaveScreen> createState() => _SaveScreenState();
}

class _SaveScreenState extends State<SaveScreen> {
  String? _selectedFolder;
  bool _saving = false;
  String? _resultPath;

  @override
  void initState() {
    super.initState();
    _loadLastPath();
  }

  Future<void> _loadLastPath() async {
    final last = await StorageService.getLastSavePath();
    if (last != null) setState(() => _selectedFolder = last);
  }

  Future<void> _pickFolder() async {
    try {
      final result = await FilePicker.platform.getDirectoryPath();
      if (result != null) {
        setState(() => _selectedFolder = result);
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('폴더 선택 실패: $e')));
    }
  }

  Future<void> _save() async {
    if (_selectedFolder == null || _selectedFolder!.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('저장 폴더를 선택하세요')));
      return;
    }
    setState(() => _saving = true);
    final saved = await StorageService.saveReceipt(
      folderPath: _selectedFolder!,
      sourceImagePath: widget.imagePath,
    );
    setState(() {
      _saving = false;
      _resultPath = saved;
    });
    if (saved != null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('저장 완료: $saved\n(.nomedia 생성 완료 — 클라우드 제외)'),
          duration: const Duration(seconds: 5),
        ),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('저장 실패')));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('저장 위치')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('저장 폴더를 선택하세요.', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
            const SizedBox(height: 8),
            const Text('이 폴더 안에 사진이 저장됩니다. .nomedia 파일이 함께 생성되어 클라우드 동기화를 방지합니다.', style: TextStyle(color: Colors.black54)),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: Text(
                    _selectedFolder ?? '선택된 폴더 없음',
                    style: TextStyle(color: _selectedFolder == null ? Colors.grey : Colors.black),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                TextButton.icon(
                  onPressed: _pickFolder,
                  icon: const Icon(Icons.folder_open),
                  label: const Text('폴더 선택'),
                ),
              ],
            ),
            const Divider(height: 32),
            const Text('미리보기', style: TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            Expanded(
              child: Container(
                decoration: BoxDecoration(border: Border.all(color: Colors.grey), borderRadius: BorderRadius.circular(8)),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(8),
                  child: Image.file(File(widget.imagePath), fit: BoxFit.contain),
                ),
              ),
            ),
            const SizedBox(height: 20),
            SizedBox(
              width: double.infinity,
              height: 56,
              child: FilledButton.icon(
                onPressed: _saving ? null : _save,
                icon: _saving ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white)) : const Icon(Icons.save_alt),
                label: Text(_saving ? '저장 중...' : '저장하기'),
                style: FilledButton.styleFrom(fontSize: 18),
              ),
            ),
            if (_resultPath != null)
              Padding(
                padding: const EdgeInsets.only(top: 12),
                child: Text('저장된 파일: $_resultPath', style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.green)),
              ),
          ],
        ),
      ),
    );
  }
}
