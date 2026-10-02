/// Animeko service - core orchestration layer.
/// Ported from animeko's MediaFetcher + MediaSelector.

import 'package:PiliPlus/http/bangumi.dart';
import 'package:PiliPlus/http/dmhy.dart';
import 'package:PiliPlus/http/mikan.dart';
import 'package:PiliPlus/models_new/animeko/animeko_resource.dart';
import 'package:flutter/foundation.dart';
import 'package:get/get.dart';

class AnimekoService extends GetxController {
  final BangumiHttp bangumi = BangumiHttp();
  final MikanHttp mikan = MikanHttp();
  final DmhyHttp dmhy = DmhyHttp();

  final RxString searchQuery = ''.obs;
  final RxBool isLoading = false.obs;
  final RxString errorMsg = ''.obs;
  final RxList<AnimekoResource> allResources = RxList<AnimekoResource>([]);
  final RxInt totalMikan = 0.obs;
  final RxInt totalDmhy = 0.obs;
  final RxString preferredSource = '全部'.obs;
  final RxString preferredResolution = '全部'.obs;
  final RxBool showTorrentOnly = false.obs;

  List<AnimekoResource> get filteredResources {
    var resources = allResources.toList();
    if (preferredSource.value == '蜜柑计划') resources = resources.where((r) => r.sourceId == 'mikan').toList();
    else if (preferredSource.value == '动漫花园') resources = resources.where((r) => r.sourceId == 'dmhy').toList();
    if (preferredResolution.value != '全部') resources = resources.where((r) => r.resolution == preferredResolution.value).toList();
    if (showTorrentOnly.value) resources = resources.where((r) => r.type == AnimekoResourceType.bittorrent).toList();
    resources.sort((a, b) {
      final aEp = a.episodeRange?.start ?? 0;
      final bEp = b.episodeRange?.start ?? 0;
      if (aEp != bEp) return aEp.compareTo(bEp);
      return a.sourceId.compareTo(b.sourceId);
    });
    return resources;
  }

  Future<void> search(String query) async {
    if (query.trim().isEmpty) { allResources.clear(); totalMikan.value = 0; totalDmhy.value = 0; errorMsg.value = ''; return; }
    isLoading.value = true; errorMsg.value = '';
    try {
      final mikanResults = await _searchMikan(query);
      final dmhyResults = await _searchDmhy(query);
      totalMikan.value = mikanResults.length;
      totalDmhy.value = dmhyResults.length;
      allResources.value = [...mikanResults, ...dmhyResults];
    } catch (e, stackTrace) {
      if (kDebugMode) debugPrint('[AnimekoService] Search error: $e\n$stackTrace');
      errorMsg.value = '搜索失败: $e';
    } finally { isLoading.value = false; }
  }

  Future<List<AnimekoResource>> _searchMikan(String query) async {
    try {
      final subjects = await mikan.searchSubjects(query);
      if (subjects.isEmpty) return [];
      final resources = <AnimekoResource>[];
      for (final subject in subjects.take(3)) {
        try {
          final episodes = await mikan.getEpisodeList(subject.id);
          for (final ep in episodes.take(20)) {
            for (final link in ep.links.take(3)) {
              resources.add(AnimekoResource(id: 'mikan.${subject.id}-ep${ep.episode}', sourceId: 'mikan', title: '${subject.name} 第${ep.episode}集 ${ep.name}', episodeRange: AnimekoEpisodeRange.single(start: ep.episode), resolution: '1080P', alliance: '蜜柑计划', downloadUrl: link.url, originalUrl: '', type: link.url.startsWith('magnet') ? AnimekoResourceType.bittorrent : AnimekoResourceType.streaming));
            }
          }
        } catch (e) { if (kDebugMode) debugPrint('[Mikan] Error: $e'); }
      }
      return resources;
    } catch (e) { if (kDebugMode) debugPrint('[Mikan] Search error: $e'); return []; }
  }

  Future<List<AnimekoResource>> _searchDmhy(String query) async {
    try {
      final result = await dmhy.search(keyword: query, page: 1);
      if (result.topics.isEmpty) return [];
      return result.topics.map((topic) {
        AnimekoEpisodeRange? episodeRange;
        final epMatch = RegExp(r'(?:第|EP|ep|Ep)?(\d+(?:\.\d+)?)?(?:[-—~](\d+(?:\.\d+)?))?(?:集|话|episode|Episode)?').firstMatch(topic.title);
        if (epMatch != null) {
          final start = double.tryParse(epMatch.group(1) ?? '');
          if (start != null) episodeRange = AnimekoEpisodeRange.single(start: start.toInt());
        }
        String? resolution;
        if (topic.title.contains('1080') || topic.title.contains('1080p')) resolution = '1080P';
        else if (topic.title.contains('720') || topic.title.contains('720p')) resolution = '720P';
        else if (topic.title.contains('480')) resolution = '480P';
        else if (topic.title.contains('2160') || topic.title.contains('4K')) resolution = '4K';
        return AnimekoResource(id: 'dmhy.${topic.id}', sourceId: 'dmhy', title: topic.title, episodeRange: episodeRange, resolution: resolution, alliance: topic.allianceName, downloadUrl: topic.magnetUrl, originalUrl: topic.detailUrl, type: topic.isTorrent ? AnimekoResourceType.bittorrent : AnimekoResourceType.streaming, sizeBytes: topic.sizeBytes);
      }).toList();
    } catch (e) { if (kDebugMode) debugPrint('[DMHY] Search error: $e'); return []; }
  }

  void setSourceFilter(String source) { preferredSource.value = source; }
  void setResolutionFilter(String resolution) { preferredResolution.value = resolution; }
  void toggleTorrentOnly(bool value) { showTorrentOnly.value = value; }
}
