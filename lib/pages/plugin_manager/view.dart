import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:PiliPlus/plugins/plugin.dart';
import 'package:PiliPlus/services/plugin/plugin_search_module.dart';
import 'package:PiliPlus/utils/storage.dart';
import 'package:PiliPlus/utils/storage_key.dart';
import 'package:dio/dio.dart';

/// 规则管理器：导入/导出/编辑订阅规则（KazumiRules 兼容格式）
class PluginManagerPage extends StatefulWidget {
  const PluginManagerPage({super.key});

  @override
  State<PluginManagerPage> createState() => _PluginManagerPageState();
}

class _PluginManagerPageState extends State<PluginManagerPage> {
  List<Plugin> _plugins = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _loadPlugins();
  }

  void _loadPlugins() {
    final raw = GStorage.setting.get(SettingBoxKey.pluginList, defaultValue: <dynamic>[]);
    if (raw is List) {
      setState(() {
        _plugins = raw.map((e) => Plugin.fromJson(Map<String, dynamic>.from(e as Map))).toList();
        _loading = false;
      });
    } else {
      setState(() => _loading = false);
    }
  }

  void _savePlugins() {
    GStorage.setting.put(SettingBoxKey.pluginList, _plugins.map((p) => p.toJson()).toList());
  }

  Future<void> _importFromUrl(String url) async {
    try {
      final resp = await Dio().get(url);
      final data = resp.data;
      if (data is String) {
        final parsed = (pluginListFromJson(data) ?? <Map<String, dynamic>>[]);
        if (parsed.isEmpty) {
          if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('无法解析规则')));
          return;
        }
        setState(() {
          for (final item in parsed) {
            final p = Plugin.fromJson(item);
            if (!_plugins.any((e) => e.name == p.name)) _plugins.add(p);
          }
          _savePlugins();
        });
        if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('成功导入 ${parsed.length} 条规则')));
      }
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('导入失败: $e')));
    }
  }

  void _showImportDialog() {
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('导入规则'),
        content: const Text('输入 KazumiRules JSON 文件 URL 或本地文件路径'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('取消')),
          TextButton(
            onPressed: () async {
              final ctrl = TextEditingController();
              await showDialog(context: context, builder: (_) => AlertDialog(
                title: const Text('规则URL'),
                content: TextField(controller: ctrl, decoration: const InputDecoration(hintText: 'https://raw.githubusercontent.com/...')),
                actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('取消')),
                  TextButton(onPressed: () => Navigator.pop(context, ctrl.text), child: const Text('导入'))],
              ));
              if (context.mounted && ctrl.text.isNotEmpty) await _importFromUrl(ctrl.text.trim());
            },
            child: const Text('导入'),
          ),
        ],
      ),
    );
  }

  List<Map<String, dynamic>>? pluginListFromJson(String str) {
    try {
      final data = jsonDecode(str) as List;
      return data.map((e) => e as Map<String, dynamic>).toList();
    } catch (_) {
      return null;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('订阅规则管理')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              children: [
                ListTile(
                  leading: const Icon(Icons.add),
                  title: const Text('从 URL 导入'),
                  subtitle: const Text('输入 KazumiRules 仓库中的规则 JSON URL'),
                  onTap: _showImportDialog,
                ),
                const Divider(),
                if (_plugins.isEmpty)
                  const Padding(padding: EdgeInsets.all(32), child: Center(child: Text('暂无订阅规则，点击上方导入'))),
                ..._plugins.map((p) => ListTile(
                  leading: CircleAvatar(child: Text(p.name.isNotEmpty ? p.name[0].toUpperCase() : '?')),
                  title: Text(p.name.isEmpty ? '(无名规则)' : p.name),
                  subtitle: Text(p.baseUrl.isEmpty ? '' : p.baseUrl),
                  trailing: Row(mainAxisSize: MainAxisSize.min, children: [
                    IconButton(icon: const Icon(Icons.delete_outline), onPressed: () {
                      setState(() { _plugins.remove(p); _savePlugins(); });
                    }),
                  ]),
                )),
              ],
            ),
      floatingActionButton: FloatingActionButton(
        onPressed: _showImportDialog,
        child: const Icon(Icons.add),
      ),
    );
  }
}
