import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:PiliPlus/utils/storage_pref.dart';
import 'package:PiliPlus/utils/storage_key.dart';
import 'package:PiliPlus/utils/storage.dart';

/// 可选的硬件解码器（mpv --hwdec）
const _kHwdecOptions = {
  'auto': 'Auto — 启用任意可用解码器',
  'auto-safe': 'Auto-Safe — 启用最佳解码器',
  'mediacodec': 'MediaCodec (Android)',
  'mediacodec-copy': 'MediaCodec Copy (Android)',
  'vulkan': 'Vulkan (全平台，实验性)',
  'vulkan-copy': 'Vulkan Copy (全平台，实验性)',
  'no': '关闭（软解）',
};

class DecoderSettingsPage extends StatefulWidget {
  const DecoderSettingsPage({super.key});

  @override
  State<DecoderSettingsPage> createState() => _DecoderSettingsPageState();
}

class _DecoderSettingsPageState extends State<DecoderSettingsPage> {
  late String _decoder = Pref.hardwareDecoding;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _decoder = Pref.hardwareDecoding;
  }

  void _updateDecoder(String value) {
    setState(() => _decoder = value);
    GStorage.setting.put(SettingBoxKey.hardwareDecoding, value);
    if (mounted) Get.back();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('硬件解码器')),
      body: ListView(
        children: _kHwdecOptions.entries.map((e) {
          final selected = _decoder == e.key;
          return RadioListTile<String>(
            title: Text(e.key, style: theme.textTheme.titleMedium),
            subtitle: Text(e.value, style: theme.textTheme.bodySmall),
            value: e.key,
            groupValue: _decoder,
            onChanged: _updateDecoder,
          );
        }).toList(),
      ),
    );
  }
}
