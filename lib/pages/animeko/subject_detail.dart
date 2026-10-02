/// 番剧详情页
import 'package:PiliPlus/http/pgc.dart';
import 'package:PiliPlus/http/loading_state.dart';
import 'package:PiliPlus/models_new/pgc/pgc_info_model/result.dart';
import 'package:PiliPlus/utils/page_utils.dart';
import 'package:flutter/material.dart' as material;
import 'package:get/get.dart';
import 'package:material_ui/material_ui.dart';

class AnimekoSubjectDetailPage extends StatefulWidget {
  final int seasonId;
  const AnimekoSubjectDetailPage({super.key, required this.seasonId});
  @override
  State<AnimekoSubjectDetailPage> createState() => _AnimekoSubjectDetailPageState();
}

class _AnimekoSubjectDetailPageState extends State<AnimekoSubjectDetailPage> {
  PgcInfoModel? _detail;
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _fetchDetail();
  }

  Future<void> _fetchDetail() async {
    setState(() { _isLoading = true; _error = null; });
    try {
      // TODO: 调用 PGC 详情 API
      // 暂时显示占位内容
      setState(() { _isLoading = false; });
    } catch (e) {
      setState(() { _error = '加载失败: $e'; _isLoading = false; });
    }
  }

  void _playEpisode(int epId) {
    // 使用 PiliNara 内置播放器
    PageUtils.viewPgc(seasonId: widget.seasonId, epId: epId);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('番剧详情'), centerTitle: true),
      body: _isLoading ? const Center(child: CircularProgressIndicator()) : _buildContent(),
    );
  }

  Widget _buildContent() {
    if (_error != null) {
      return Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
        const Icon(Icons.error_outline, size: 48, color: Colors.red),
        const SizedBox(height: 16),
        Text(_error!),
        const SizedBox(height: 16),
        ElevatedButton(onPressed: _fetchDetail, child: const Text('重试')),
      ]));
    }
    return const Center(child: Text('番剧详情页面（待对接 PGC API）'));
  }
}
