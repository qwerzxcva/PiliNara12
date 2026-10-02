/// Animeko subject detail page.
///
/// Shows anime info from Bangumi + episode list from Mikan/DMHY.
/// Clicking an episode opens the video player.
///
/// Mirrors animeko's SubjectDetailsPage + EpisodePage combined.

import 'package:PiliPlus/http/bangumi.dart';
import 'package:PiliPlus/http/dmhy.dart';
import 'package:PiliPlus/http/mikan.dart';
import 'package:PiliPlus/models_new/animeko/animeko_resource.dart';
import 'package:PiliPlus/utils/page_utils.dart';
import 'package:PiliPlus/utils/utils.dart';
import 'package:flutter/material.dart' as material;
import 'package:get/get.dart';
import 'package:material_ui/material_ui.dart';

class AnimekoSubjectDetailPage extends StatefulWidget {
  final String animeName;
  final String? bangumiSubjectId;

  const AnimekoSubjectDetailPage({
    super.key,
    required this.animeName,
    this.bangumiSubjectId,
  });

  @override
  State<AnimekoSubjectDetailPage> createState() =>
      _AnimekoSubjectDetailPageState();
}

class _AnimekoSubjectDetailPageState
    extends State<AnimekoSubjectDetailPage> {
  final BangumiHttp _bangumi = BangumiHttp();
  final MikanHttp _mikan = MikanHttp();
  final DmhyHttp _dmhy = DmhyHttp();

  BangumiSubject? _subject;
  List<MikanTopic> _mikanTopics = [];
  List<DmhyTopic> _dmhyTopics = [];
  bool _isLoading = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _fetchData();
  }

  Future<void> _fetchData() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      // Fetch Bangumi info
      if (widget.bangumiSubjectId != null) {
        _subject = await _bangumi.getSubject(
            int.tryParse(widget.bangumiSubjectId!) ?? 0);
      } else {
        final results =
            await _bangumi.search(keyword: widget.animeName, max: 3);
        if (results.isNotEmpty) {
          _subject = results.first;
        }
      }

      // Fetch Mikan resources via RSS
      if (_subject != null) {
        final topics = await _mikan.searchByBangumiId(
            _subject!.id.toString());
        if (topics != null) {
          setState(() => _mikanTopics = topics);
        }
      }

      // Fallback: search Mikan by keyword
      if (_mikanTopics.isEmpty) {
        final subjects = await _mikan.searchSubjects(widget.animeName);
        if (subjects.isNotEmpty) {
          final episodes =
              await _mikan.getEpisodeList(subjects.first.id);
          // Convert episodes to topics (simplified)
          for (final ep in episodes.take(20)) {
            for (final link in ep.links.take(3)) {
              _mikanTopics.add(MikanTopic(
                topicId: 'ep${ep.episode}',
                rawTitle:
                    '${_subject?.nameCN ?? widget.animeName} 第${ep.episode}集',
                alliance: '蜜柑计划',
                magnetUrl: link.url.startsWith('magnet')
                    ? link.url
                    : null,
                resolution: '1080P',
                subtitleLanguages: ['ZHO'],
              ));
            }
          }
        }
      }

      // Fetch DMHY resources
      final dmhyResult =
          await _dmhy.search(keyword: widget.animeName, page: 1);
      setState(() => _dmhyTopics = dmhyResult.topics);
    } catch (e) {
      setState(() => _error = '加载失败: $e');
    } finally {
      setState(() => _isLoading = false);
    }
  }

  void _playEpisode(MikanTopic topic, int episodeNum) {
    // For magnet links, offer to copy or open
    if (topic.magnetUrl != null) {
      material.showDialog(
        context: context,
        builder: (ctx) => material.AlertDialog(
          title: Text('第${episodeNum}集'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(topic.rawTitle),
              const SizedBox(height: 12),
              const Text('资源类型: BT 磁力链接',
                  style: TextStyle(color: Colors.grey)),
              const Text('点击复制磁力链，使用系统下载器或 BT 客户端播放',
                  style: TextStyle(color: Colors.grey, fontSize: 12)),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () {
                Utils.copyText(topic.magnetUrl!);
                Get.back();
              },
              child: const Text('复制磁力链'),
            ),
            ElevatedButton(
              onPressed: () {
                PageUtils.launchURL(topic.magnetUrl!);
                Get.back();
              },
              child: const Text('打开链接'),
            ),
          ],
        ),
      );
    } else if ((topic.downloadUrl ?? '').isNotEmpty) {
      if (topic.downloadUrl != null) PageUtils.launchURL(topic.downloadUrl!);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.animeName),
        centerTitle: true,
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_isLoading && _mikanTopics.isEmpty && _dmhyTopics.isEmpty) {
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
              onPressed: _fetchData,
              child: const Text('重试'),
            ),
          ],
        ),
      );
    }

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        // Bangumi info card
        if (_subject != null) _buildSubjectCard(_subject!),
        const SizedBox(height: 16),

        // Mikan episodes
        if (_mikanTopics.isNotEmpty) ...[
          _buildSectionHeader('蜜柑计划', _mikanTopics.length),
          const SizedBox(height: 8),
          ..._mikanTopics.map((t) => _buildTopicCard(t, source: 'mikan')),
          const SizedBox(height: 16),
        ],

        // DMHY topics
        if (_dmhyTopics.isNotEmpty) ...[
          _buildSectionHeader('动漫花园', _dmhyTopics.length),
          const SizedBox(height: 8),
          ..._dmhyTopics.map((t) => _buildDmhyCard(t)),
        ],

        // Empty state
        if (_mikanTopics.isEmpty && _dmhyTopics.isEmpty)
          const Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.search_off, size: 48, color: Colors.grey),
                SizedBox(height: 16),
                Text('未找到该番剧的资源',
                    style: TextStyle(color: Colors.grey)),
                SizedBox(height: 8),
                Text('尝试其他关键词搜索',
                    style: TextStyle(color: Colors.grey, fontSize: 12)),
              ],
            ),
          ),
      ],
    );
  }

  Widget _buildSubjectCard(BangumiSubject subject) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            // Cover image
            ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: Image.network(
                subject.imageUrl ?? '',
                width: 80,
                height: 110,
                fit: BoxFit.cover,
                errorBuilder: (_, __, ___) => Container(
                  width: 80,
                  height: 110,
                  color: Colors.grey[300],
                  child: const Icon(Icons.movie),
                ),
              ),
            ),
            const SizedBox(width: 16),
            // Info
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    subject.nameCN ?? subject.name,
                    style: const TextStyle(
                        fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  if (subject.rating != null)
                    Padding(
                      padding: const EdgeInsets.only(top: 4),
                      child: Row(
                        children: [
                          const Icon(Icons.star,
                              size: 16, color: Colors.amber),
                          const SizedBox(width: 4),
                          Text('${subject.rating}',
                              style: const TextStyle(fontSize: 14)),
                        ],
                      ),
                    ),
                  if (subject.totalEpisodes != null)
                    Padding(
                      padding: const EdgeInsets.only(top: 4),
                      child: Text(
                          '${subject.totalEpisodes} 集',
                          style: const TextStyle(fontSize: 14,
                              color: Colors.grey)),
                    ),
                  if (subject.tags.isNotEmpty)
                    Wrap(
                      spacing: 4,
                      runSpacing: 4,
                      children: subject.tags
                          .take(5)
                          .map((t) => Chip(
                                label: Text(t),
                                visualDensity: VisualDensity.compact,
                                padding: EdgeInsets.zero,
                                ))
                          .toList(),
                    ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSectionHeader(String title, int count) {
    return Row(
      children: [
        Text(title,
            style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
        const SizedBox(width: 8),
        Container(
          padding:
              const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
          decoration: BoxDecoration(
            color: Colors.grey[200],
            borderRadius: BorderRadius.circular(12),
          ),
          child: Text('$count',
              style: TextStyle(fontSize: 12, color: Colors.grey[700])),
        ),
      ],
    );
  }

  Widget _buildTopicCard(MikanTopic topic, {required String source}) {
    final epRange = topic.episodeRange;
    final epText = epRange != null ? ' ${epRange}' : '';

    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: source == 'mikan'
              ? Colors.green.withOpacity(0.1)
              : Colors.blue.withOpacity(0.1),
          child: Text(
            epText.isNotEmpty ? epText.split(' ').last : '?',
            style: TextStyle(
                fontSize: 12,
                color: source == 'mikan' ? Colors.green : Colors.blue),
          ),
        ),
        title: Text(topic.rawTitle,
            maxLines: 1, overflow: TextOverflow.ellipsis),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (topic.alliance.isNotEmpty)
              Text('字幕组: ${topic.alliance}',
                  style: const TextStyle(fontSize: 12)),
            if (topic.resolution != null)
              Text('分辨率: ${topic.resolution}',
                  style: const TextStyle(fontSize: 12)),
            if (topic.sizeBytes > 0)
              Text('大小: ${_formatSize(topic.sizeBytes)}',
                  style: const TextStyle(fontSize: 12)),
          ],
        ),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (topic.magnetUrl != null)
              IconButton(
                icon: const Icon(Icons.copy, size: 20),
                tooltip: '复制磁力链',
                onPressed: () => Utils.copyText(topic.magnetUrl!),
              ),
            IconButton(
              icon: const Icon(Icons.open_in_new, size: 20),
              tooltip: '打开',
              onPressed: () =>
                  PageUtils.launchURL(topic.magnetUrl ?? topic.originalLink),
            ),
          ],
        ),
        onTap: () => _playEpisode(topic, epRange?.start ?? 0),
      ),
    );
  }

  Widget _buildDmhyCard(DmhyTopic topic) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: Colors.blue.withOpacity(0.1),
          child: Icon(Icons.cloud, size: 20, color: Colors.blue),
        ),
        title: Text(topic.title,
            maxLines: 1, overflow: TextOverflow.ellipsis),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (topic.allianceName.isNotEmpty)
              Text('字幕组: ${topic.allianceName}',
                  style: const TextStyle(fontSize: 12)),
            Text('大小: ${topic.sizeText}',
                style: const TextStyle(fontSize: 12)),
          ],
        ),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (topic.magnetUrl.isNotEmpty)
              IconButton(
                icon: const Icon(Icons.copy, size: 20),
                tooltip: '复制磁力链',
                onPressed: () => Utils.copyText(topic.magnetUrl),
              ),
            IconButton(
              icon: const Icon(Icons.open_in_new, size: 20),
              tooltip: '打开',
              onPressed: () => PageUtils.launchURL(topic.detailUrl),
            ),
          ],
        ),
        onTap: () {
          if (topic.magnetUrl.isNotEmpty) {
            material.showDialog(
              context: context,
              builder: (ctx) => material.AlertDialog(
                title: const Text('磁力链接'),
                content: SelectableText(topic.magnetUrl,
                    style: const TextStyle(fontSize: 12)),
                actions: [
                  TextButton(
                    onPressed: () {
                      Utils.copyText(topic.magnetUrl);
                      Get.back();
                    },
                    child: const Text('复制'),
                  ),
                  ElevatedButton(
                    onPressed: () {
                      PageUtils.launchURL(topic.magnetUrl);
                      Get.back();
                    },
                    child: const Text('打开'),
                  ),
                ],
              ),
            );
          }
        },
      ),
    );
  }

  String _formatSize(int bytes) {
    if (bytes >= 1073741824) return '${(bytes / 1073741824).toStringAsFixed(1)} GB';
    if (bytes >= 1048576) return '${(bytes / 1048576).toStringAsFixed(1)} MB';
    if (bytes >= 1024) return '${(bytes / 1024).toStringAsFixed(1)} KB';
    return '$bytes B';
  }
}
