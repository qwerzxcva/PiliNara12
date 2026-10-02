import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:PiliPlus/utils/storage_pref.dart';
import 'package:PiliPlus/utils/storage_key.dart';
import 'package:PiliPlus/utils/storage.dart';

/// 可选的视频渲染器（mpv --vo）
const _kVideoRenderers = {
  'gpu': 'GPU（OpenGL）— 通用且稳定',
  'gpu-next': 'GPU-Next（Vulkan）— 新设备最佳性能',
  'mediacodec_embed': 'MediaCodec Embed — 功耗最低，不支持超分辨率',
};

class RendererSettingsPage extends StatefulWidget {
  const RendererSettingsPage({super.key});

  @override
  State<RendererSettingsPage> createState() => _RendererSettingsPageState();
}

class _RendererSettingsPageState extends State<RendererSettingsPage> {
  late String _renderer = Pref.androidVideoRenderer;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _renderer = Pref.androidVideoRenderer;
  }

  void _updateRenderer(String value) {
    setState(() => _renderer = value);
    GStorage.setting.put(SettingBoxKey.androidVideoRenderer, value);
    if (mounted) Get.back();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('视频渲染器')),
      body: ListView(
        children: _kVideoRenderers.entries.map((e) {
          final selected = _renderer == e.key;
          return RadioListTile<String>(
            title: Text(e.key, style: theme.textTheme.titleMedium),
            subtitle: Text(e.value, style: theme.textTheme.bodySmall),
            value: e.key,
            groupValue: _renderer,
            onChanged: _updateRenderer,
          );
        }).toList(),
      ),
    );
  }
}
