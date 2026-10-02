/// 番剧详情页
import 'package:PiliPlus/http/loading_state.dart';
import 'package:PiliPlus/models_new/pgc/pgc_info_model/result.dart';
import 'package:PiliPlus/models_new/pgc/pgc_info_model/episode.dart';
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
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      // 使用已有的 PGC 接口获取详情
      final res = await _getPgcDetail(widget.seasonId);
      if (res != null) {
        setState(() {
          _detail = res;
          _isLoading = false;
        });
      } else {
        setState(() {
          _error = '获取详情失败';
          _isLoading = false;
        });
      }
    } catch (e) {
      setState(() {
        _error = '加载失败: $e';
        _isLoading = false;
      });
    }
  }

  // TODO: 实现获取 PGC 详情的逻辑
  Future<PgcInfoModel?> _getPgcDetail(int seasonId) async {
    // 这里需要调用实际的 PGC API
    // 暂时返回 null
    return null;
  }

  void _playEpisode(int epId, int seasonId) {
    PageUtils.viewPgc(seasonId: seasonId, epId: epId);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('番剧详情'),
        centerTitle: true,
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_error != null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, size: 48, color: Colors.red),
            const SizedBox(height: 16),
            Text(_error!),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _fetchDetail,
              child: const Text('重试'),
            ),
          ],
        ),
      );
    }

    final detail = _detail;
    if (detail == null) {
      return const Center(child: Text('暂无详情'));
    }

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _buildHeader(detail),
        const SizedBox(height: 16),
        if ((detail.evaluate?.isNotEmpty ?? false)) ...[
          _buildSection('简介', detail.evaluate!),
          const SizedBox(height: 16),
        ],
        if ((detail.episodes?.isNotEmpty ?? false)) ...[
          _buildEpisodeList(detail.episodes!),
        ],
      ],
    );
  }

  Widget _buildHeader(PgcInfoModel detail) {
    return Row(
      children: [
        ClipRRect(
          borderRadius: BorderRadius.circular(8),
          child: Image.network(
            detail.cover ?? '',
            width: 120,
            height: 160,
            fit: BoxFit.cover,
            errorBuilder: (_, __, ___) => Container(
              width: 120,
              height: 160,
              color: Colors.grey[300],
              child: const Icon(Icons.movie),
            ),
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                detail.title ?? '',
                style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: 8),
              if (detail.seasonTitle?.isNotEmpty ?? false)
                Text(
                  detail.seasonTitle!,
                  style: const TextStyle(fontSize: 14, color: Colors.grey),
                ),
              const SizedBox(height: 8),
              if (detail.rating?.score != null)
                Row(
                  children: [
                    const Icon(Icons.star, size: 16, color: Colors.amber),
                    const SizedBox(width: 4),
                    Text('${detail.rating?.score}'),
                  ],
                ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildSection(String title, String content) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 8),
        Text(content),
      ],
    );
  }

  Widget _buildEpisodeList(List<EpisodeItem> episodes) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '剧集',
          style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: episodes.map((ep) {
            return material.OutlinedButton(
              onPressed: () => _playEpisode(ep.epId ?? 0, widget.seasonId),
              child: Text(ep.title ?? '第${ep.id}集'),
            );
          }).toList(),
        ),
      ],
    );
  }
}
