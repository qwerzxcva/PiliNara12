/// 番剧详情页 - 显示详情 + 剧集列表 + 播放
import 'package:PiliPlus/http/pgc.dart';
import 'package:PiliPlus/http/loading_state.dart';
import 'package:PiliPlus/models_new/pgc/pgc_info_model/result.dart';
import 'package:PiliPlus/models_new/pgc/pgc_info_model/rating.dart';
import 'package:PiliPlus/models_new/pgc/pgc_info_model/episode.dart';
import 'package:PiliPlus/utils/page_utils.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

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
      // 调用 PGC API 获取详情
      // TODO: 找到正确的 PGC 详情接口
      // 目前使用占位数据
      await Future.delayed(const Duration(milliseconds: 500));
      
      // 模拟数据
      _detail = PgcInfoModel(
        title: '示例番剧',
        seasonTitle: '第一季',
        cover: 'https://via.placeholder.com/300x400',
        evaluate: '这是一部精彩的番剧...',
        rating: Rating(score: 9.5),
        episodes: List.generate(12, (i) => EpisodeItem(id: i + 1, epId: i + 1000, title: '第${i + 1}集')),
      );
      
      setState(() => _isLoading = false);
    } catch (e) {
      setState(() {
        _error = '加载失败: $e';
        _isLoading = false;
      });
    }
  }

  void _playEpisode(int epId) {
    if (widget.seasonId > 0 && epId > 0) {
      PageUtils.viewPgc(seasonId: widget.seasonId, epId: epId);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('番剧详情')),
      body: _isLoading 
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? _buildErrorView()
              : _detail != null ? _buildContent() : _buildEmptyView(),
    );
  }

  Widget _buildErrorView() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.error_outline, size: 48, color: Colors.red),
          const SizedBox(height: 16),
          Text(_error!),
          const SizedBox(height: 16),
          ElevatedButton(onPressed: _fetchDetail, child: const Text('重试')),
        ],
      ),
    );
  }

  Widget _buildEmptyView() {
    return const Center(child: Text('暂无详情'));
  }

  Widget _buildContent() {
    final detail = _detail!;
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
                    Text('${detail.rating!.score}'),
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
        Text(title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
        const SizedBox(height: 8),
        Text(content),
      ],
    );
  }

  Widget _buildEpisodeList(List<EpisodeItem> episodes) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('剧集', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: episodes.map((ep) {
            return OutlinedButton(
              onPressed: () => _playEpisode(ep.epId ?? 0),
              child: Text('E${ep.id ?? ep.epId ?? 0}'),
            );
          }).toList(),
        ),
      ],
    );
  }
}
