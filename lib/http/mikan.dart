/// Mikan (蜜柑计划) HTTP client.
/// Ported from animeko's MikanMediaSource.

import 'package:dio/dio.dart';
import 'package:html/parser.dart' show parse;

class MikanHttp {
  static const baseUrl = 'https://mikanani.me';
  final Dio dio;
  final Map<String, String> _indexCache = {};

  MikanHttp({Dio? dio}) : dio = dio ?? Dio();

  Future<List<MikanSubject>> searchSubjects(String keyword) async {
    final resp = await dio.get('$baseUrl/Home/BangumiSearch', queryParameters: {'searchstr': keyword});
    if (resp.data == null) return [];
    final doc = parse(resp.data.toString());
    final results = <MikanSubject>[];
    for (final item in doc.querySelectorAll('li.light')) {
      final linkEl = item.querySelector('a[href*="/Home/Bangumi/"]');
      if (linkEl == null) continue;
      final href = linkEl.attributes['href'] ?? '';
      final name = linkEl.text.trim();
      final idMatch = RegExp(r'/Bangumi/(\d+)').firstMatch(href);
      final id = idMatch?.group(1);
      if (name.isNotEmpty && id != null) results.add(MikanSubject(name: name, id: id));
    }
    return results;
  }

  Future<List<MikanEpisode>> getEpisodeList(String subjectId) async {
    final resp = await dio.get('$baseUrl/Home/Bangumi/$subjectId');
    if (resp.data == null) return [];
    final doc = parse(resp.data.toString());
    final episodes = <MikanEpisode>[];
    for (final column in doc.querySelectorAll('.bf-green')) {
      for (final row in column.querySelectorAll('tr')) {
        final cells = row.querySelectorAll('td');
        if (cells.length < 3) continue;
        final timeText = cells[0].text.trim();
        final episodeText = cells[1].text.trim();
        final linkCell = cells[2];
        final epMatch = RegExp(r'第?(\d+)集|EP(\d+)').firstMatch(episodeText);
        final epNum = epMatch != null ? (int.tryParse(epMatch.group(1) ?? epMatch.group(2) ?? '0') ?? 0) : 0;
        final links = <MikanDownloadLink>[];
        for (final a in linkCell.querySelectorAll('a')) {
          final href = a.attributes['href'] ?? '';
          final text = a.text.trim();
          if (href.isNotEmpty) links.add(MikanDownloadLink(url: href.startsWith('http') ? href : '$baseUrl$href', type: text.isEmpty ? 'link' : text));
        }
        if (epNum > 0) episodes.add(MikanEpisode(episode: epNum, name: episodeText, time: timeText, links: links));
      }
    }
    episodes.sort((a, b) => a.episode.compareTo(b.episode));
    return episodes;
  }

  Future<List<MikanTopic>?> searchByBangumiId(String bangumiSubjectId) async {
    String? mikanId = _indexCache[bangumiSubjectId];
    if (mikanId == null) return null;
    final resp = await dio.get('$baseUrl/RSS/Bangumi', queryParameters: {'bangumiId': mikanId});
    if (resp.data == null) return null;
    return _parseRssTopics(resp.data.toString(), bangumiSubjectId);
  }

  List<MikanTopic> _parseRssTopics(String rssXml, String bangumiId) {
    final topics = <MikanTopic>[];
    final doc = parse(rssXml);
    for (final item in doc.querySelectorAll('item')) {
      final title = item.querySelector('title')?.text.trim() ?? '';
      final guid = item.querySelector('guid')?.text.trim() ?? '';
      final pubDate = item.querySelector('pubDate')?.text ?? '';
      final enclosure = item.querySelector('enclosure');
      final contentLength = item.querySelector('contentLength')?.text ?? '';
      String? magnetUrl;
      String? torrentUrl;
      final linkEl = item.querySelector('link');
      final linkText = linkEl?.text?.trim();
      if (title.contains('magnet')) {
        final m = RegExp(r'magnet:\?xt=urn:btih:[a-zA-Z0-9]+').firstMatch(title);
        if (m != null) magnetUrl = m.group(0);
      }
      if (linkText != null && linkText.startsWith('magnet:')) magnetUrl = linkText;
      if (enclosure != null) {
        final url = enclosure.attributes['url'];
        if (url != null && (url.endsWith('.torrent') || url.contains('uploadbt'))) torrentUrl = url;
      }
      if (magnetUrl == null && torrentUrl == null) continue;
      final details = MikanTitleParser.parse(title);
      int sizeBytes = 0;
      final sizeMatch = RegExp(r'([\d.]+)\s*(GB|MB|KB)').firstMatch(contentLength);
      if (sizeMatch != null) {
        final val = double.tryParse(sizeMatch.group(1) ?? '0') ?? 0;
        final unit = (sizeMatch.group(2) ?? 'MB').toUpperCase();
        switch (unit) {
          case 'GB': sizeBytes = (val * 1073741824).toInt(); break;
          case 'MB': sizeBytes = (val * 1048576).toInt(); break;
          case 'KB': sizeBytes = (val * 1024).toInt(); break;
        }
      }
      DateTime? publishedAt;
      try { publishedAt = DateTime.tryParse(pubDate); } catch (_) {}
      topics.add(MikanTopic(topicId: guid.split('/').last, rawTitle: title, alliance: details.alliance, episodeRange: details.episodeRange, resolution: details.resolution, subtitleLanguages: details.subtitleLanguages, magnetUrl: magnetUrl, torrentUrl: torrentUrl, sizeBytes: sizeBytes, publishedAt: publishedAt, originalLink: linkText ?? ''));
    }
    return topics;
  }

  Future<String?> findMikanIdByBangumi({required String animeName, required String bangumiSubjectId}) async {
    final subjects = await searchSubjects(animeName.trim().split(' ').first);
    for (final subject in subjects) {
      try {
        final resp = await dio.get('$baseUrl/Home/Bangumi/${subject.id}');
        if (resp.data == null) continue;
        final doc = parse(resp.data.toString());
        for (final el in doc.querySelectorAll('.bangumi-info')) {
          if (el.text.contains('Bangumi番组计划链接')) {
            final link = el.querySelector('a')?.attributes['href'];
            if (link != null) {
              final idMatch = RegExp(r'subject/(\d+)').firstMatch(link);
              if (idMatch != null && idMatch.group(1) == bangumiSubjectId) {
                _indexCache[bangumiSubjectId] = subject.id;
                return subject.id;
              }
            }
          }
        }
      } catch (_) {}
    }
    return null;
  }
}

class MikanSubject {
  final String name;
  final String id;
  const MikanSubject({required this.name, required this.id});
}

class MikanEpisode {
  final int episode;
  final String name;
  final String time;
  final List<MikanDownloadLink> links;
  const MikanEpisode({required this.episode, required this.name, required this.time, required this.links});
}

class MikanDownloadLink {
  final String url;
  final String type;
  const MikanDownloadLink({required this.url, required this.type});
}

class MikanTopic {
  final String topicId;
  final String rawTitle;
  final String alliance;
  final MikanEpisodeRange? episodeRange;
  final String? resolution;
  final List<String> subtitleLanguages;
  final String? magnetUrl;
  final String? torrentUrl;
  final int sizeBytes;
  final DateTime? publishedAt;
  final String originalLink;
  const MikanTopic({required this.topicId, required this.rawTitle, required this.alliance, this.episodeRange, this.resolution, this.subtitleLanguages = const [], this.magnetUrl, this.torrentUrl, this.sizeBytes = 0, this.publishedAt, this.originalLink = ''});
  String? get downloadUrl => magnetUrl ?? torrentUrl;
  bool get isTorrent => magnetUrl != null || torrentUrl != null;
}

class MikanEpisodeRange {
  final int? start;
  final int? end;
  final bool isSeason;
  final int? seasonNumber;
  const MikanEpisodeRange._({this.start, this.end, this.isSeason = false, this.seasonNumber});
  const MikanEpisodeRange.single({required int start, int? end}) : this._(start: start, end: end ?? start);
  const MikanEpisodeRange.range({required int start, required int end}) : this._(start: start, end: end);
  const MikanEpisodeRange.season(int number) : this._(isSeason: true, seasonNumber: number);
  bool contains(int episode) { if (isSeason) return true; if (start == null) return false; return end != null ? episode >= start! && episode <= end! : episode == start; }
  @override String toString() { if (isSeason) return 'S$seasonNumber'; if (start != null && end != null && start == end) return 'Ep$start'; if (start != null && end != null) return '$start-$end'; return '?'; }
}

class MikanTitleDetails {
  final String alliance;
  final MikanEpisodeRange? episodeRange;
  final String? resolution;
  final List<String> subtitleLanguages;
  const MikanTitleDetails({this.alliance = '', this.episodeRange, this.resolution, this.subtitleLanguages = const []});
}

class MikanTitleParser {
  const MikanTitleParser._();
  static MikanTitleDetails parse(String title) {
    String alliance = '';
    MikanEpisodeRange? episodeRange;
    String? resolution;
    final subtitleLanguages = <String>[];
    final allianceMatch = RegExp(r'[\[【](.+?)[\]】]').firstMatch(title);
    if (allianceMatch != null) alliance = allianceMatch.group(1) ?? '';
    final epMatch = RegExp(r'(?:第|EP|ep|Ep)?(\d+(?:\.\d+)?)?(?:[-—~](\d+(?:\.\d+)?))?(?:集|话|episode|Episode)?').firstMatch(title);
    if (epMatch != null) {
      final start = double.tryParse(epMatch.group(1) ?? '');
      final endStr = epMatch.group(2);
      if (start != null) {
        final startInt = start.toInt();
        if (endStr != null) {
          final end = double.tryParse(endStr);
          if (end != null) episodeRange = MikanEpisodeRange.range(start: startInt, end: end.toInt());
          else episodeRange = MikanEpisodeRange.single(start: startInt);
        } else {
          episodeRange = MikanEpisodeRange.single(start: startInt);
        }
      }
    }
    final seasonMatch = RegExp(r'S(\d+)|第(\d+)季').firstMatch(title);
    if (seasonMatch != null) {
      final seasonNum = int.tryParse(seasonMatch.group(1) ?? seasonMatch.group(2) ?? '');
      if (seasonNum != null) episodeRange = MikanEpisodeRange.season(seasonNum);
    }
    if (title.contains('1080') || title.contains('1080p')) resolution = '1080P';
    else if (title.contains('720') || title.contains('720p')) resolution = '720P';
    else if (title.contains('480') || title.contains('480p')) resolution = '480P';
    else if (title.contains('2160') || title.contains('4K')) resolution = '4K';
    if (title.contains('简中') || title.contains('国语') || title.contains('China')) subtitleLanguages.add('ZHO');
    if (title.contains('繁中') || title.contains('粤语')) subtitleLanguages.add('ZHT');
    if (title.contains('日语') || title.contains('JP')) subtitleLanguages.add('JPN');
    if (title.contains('英语') || title.contains('EN')) subtitleLanguages.add('ENG');
    if (subtitleLanguages.isEmpty) subtitleLanguages.add('ZHO');
    return MikanTitleDetails(alliance: alliance, episodeRange: episodeRange, resolution: resolution, subtitleLanguages: subtitleLanguages);
  }
}
