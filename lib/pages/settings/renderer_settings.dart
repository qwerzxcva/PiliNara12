import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:PiliPlus/utils/storage_pref.dart';
import 'package:PiliPlus/utils/storage_key.dart';
import 'package:PiliPlus/utils/storage.dart';

/// 可选的视频渲染器（mpv --vo）
///
/// 注意：media_kit_video（Starfallan native 分支）当前只完整支持 gpu 和 null。
/// gpu-next / mediacodec_embed 在 libmpv Android 层有支持，但 media_kit 的
/// AndroidVideoController 未实现对应逻辑，选择后会导致黑屏，故暂不提供。
const _kVideoRenderers = {
  'gpu': 'GPU（OpenGL）— 默认，通用且稳定，支持超分辨率',
  'null': 'NULL（软件渲染到Surface）— 无GPU加速，兼容性最好',
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
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16),
            child: Text(
              '当前只支持 GPU（OpenGL）和 NULL。'
              'GPU-Next（Vulkan）和 MediaCodec Embed 在 media_kit '
              'AndroidVideoController 中尚未完整实现，选后者会黑屏。',
              style: theme.textTheme.bodySmall?.copyWith(color: Colors.orange[700]),
            ),
          ),
          ..._kVideoRenderers.entries.map((e) {
            final selected = _renderer == e.key;
            return RadioListTile<String>(
              title: Text(e.key, style: theme.textTheme.titleMedium),
              subtitle: Text(e.value, style: theme.textTheme.bodySmall),
              value: e.key,
              groupValue: _renderer,
              onChanged: _updateRenderer,
            );
          }).toList(),
        ],
      ),
    );
  }
}
