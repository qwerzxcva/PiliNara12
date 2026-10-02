import 'package:PiliPlus/utils/storage_pref.dart';
import 'package:PiliPlus/utils/storage_key.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

class PanoramaSettingsPage extends StatelessWidget {
  const PanoramaSettingsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('全景背景设置')),
      body: ListView(
        padding: const EdgeInsets.symmetric(vertical: 8),
        children: [
          SwitchListTile(
            title: const Text('启用全景背景'),
            subtitle: const Text('详情页背景随滚动视差模糊'),
            value: true, // TODO: 从 Pref 读取
            onChanged: (v) {},
          ),
          const Divider(height: 24),
          _SliderSetting(
            title: '模糊强度',
            subtitle: '当前：35%',
            value: 35,
            max: 100,
            onChanged: (v) {},
          ),
          _SliderSetting(
            title: '顶部不透明度',
            subtitle: '当前：90%',
            value: 90,
            max: 100,
            onChanged: (v) {},
          ),
          _SliderSetting(
            title: '过渡范围',
            subtitle: '当前：100%',
            value: 100,
            max: 100,
            onChanged: (v) {},
          ),
          const Divider(height: 24),
          SwitchListTile(
            title: const Text('启用视差动画'),
            subtitle: const Text('背景随滚动产生平滑过渡效果'),
            value: true,
            onChanged: (v) {},
          ),
          SwitchListTile(
            title: const Text('滚动联动'),
            subtitle: const Text('背景与内容滚动同步'),
            value: true,
            onChanged: (v) {},
          ),
        ],
      ),
    );
  }
}

class _SliderSetting extends StatelessWidget {
  final String title;
  final String subtitle;
  final double value;
  final double max;
  final Function(double) onChanged;

  const _SliderSetting({
    required this.title,
    required this.subtitle,
    required this.value,
    required this.max,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: const TextStyle(fontWeight: FontWeight.w500)),
          Text(subtitle, style: TextStyle(color: Colors.grey[600], fontSize: 12)),
          Slider(
            value: value,
            max: max,
            divisions: 100,
            label: '${value.round()}%',
            onChanged: onChanged,
          ),
        ],
      ),
    );
  }
}
