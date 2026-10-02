/// DMHY (动漫花园) HTTP client.
/// Ported from animeko's DmhyMediaSource.

import 'package:dio/dio.dart';
import 'package:html/parser.dart' show parse;

class DmhyHttp {
  static const baseUrl = 'https://www.dmhy.org';
  final Dio dio;
  DmhyHttp({Dio? dio}) : dio = dio ?? Dio();

  Future<DmhySearchResult> search({required String keyword, int page = 1, String? orderId, String? sortId}) async {
    final resp = await dio.get('$baseUrl/topics/list', queryParameters: {'keyword': keyword, if (page > 1) 'page': page, if (orderId != null) 'order': orderId, if (sortId != null) 'sort_id': sortId});
    if (resp.data == null) return DmhySearchResult(topics: [], hasNextPage: false, currentPage: page);
    final doc = parse(resp.data.toString());
    final topics = <DmhyTopic>[];
    for (final item in doc.querySelectorAll('li.topic_description')) {
      final topic = _parseTopic(item);
      if (topic != null) topics.add(topic);
    }
    return DmhySearchResult(topics: topics, hasNextPage: doc.querySelectorAll('a[href*="page="]').isNotEmpty && topics.isNotEmpty, currentPage: page);
  }

  DmhyTopic? _parseTopic(dynamic element) {
    final cells = element.querySelectorAll('td');
    if (cells.length < 6) return null;
    final dateText = cells[0].text.trim();
    final categoryLink = cells[1].querySelector('a')?.attributes['href'] ?? '';
    final categoryId = categoryLink.split('/').lastOrNull;
    final categoryName = cells[1].text.trim();
    final allianceLinks = cells[2].querySelectorAll('span.tag a');
    String? allianceId; String? allianceName; String title = ''; int commentCount = 0;
    for (int i = 0; i < allianceLinks.length; i++) {
      final link = allianceLinks[i]; final linkHref = link.attributes['href'] ?? ''; final linkText = link.text.trim();
      if (linkHref.contains('/alliance/')) { allianceId = linkHref.split('/').last; allianceName = linkText; }
      else if (i == allianceLinks.length - 1) title = linkText;
    }
    final commentMatch = RegExp(r'(\d+)\s*评论').firstMatch(cells[2].text);
    if (commentMatch != null) commentCount = int.tryParse(commentMatch.group(1) ?? '0') ?? 0;
    final titleLink = cells[2].querySelector('a[href*="/topics/"]');
    final topicLink = titleLink?.attributes['href'] ?? '';
    final topicId = topicLink.split('/').lastOrNull;
    final magnetLink = cells[3].querySelector('a[href*="magnet"]')?.attributes['href'] ?? '';
    final sizeText = cells[4].text.trim();
    final authorCell = cells[5];
    final authorLink = authorCell.querySelector('a')?.attributes['href'] ?? '';
    final authorId = authorLink.split('/').lastOrNull;
    final authorName = authorCell.text.trim();
    if (topicId == null || topicId.isEmpty) return null;
    return DmhyTopic(id: topicId, date: dateText, categoryId: categoryId ?? '', categoryName: categoryName, allianceId: allianceId, allianceName: allianceName ?? '', title: title, commentCount: commentCount, magnetUrl: magnetLink, sizeText: sizeText, authorId: authorId, authorName: authorName, detailUrl: topicLink.startsWith('http') ? topicLink : '$baseUrl$topicLink');
  }
}

class DmhyTopic {
  final String id; final String date; final String categoryId; final String categoryName;
  final String? allianceId; final String allianceName; final String title; final int commentCount;
  final String magnetUrl; final String sizeText; final String? authorId; final String authorName; final String detailUrl;
  const DmhyTopic({required this.id, required this.date, required this.categoryId, required this.categoryName, this.allianceId, required this.allianceName, required this.title, required this.commentCount, required this.magnetUrl, required this.sizeText, this.authorId, required this.authorName, required this.detailUrl});
  String? get downloadUrl => magnetUrl.isNotEmpty ? magnetUrl : null;
  bool get isTorrent => magnetUrl.isNotEmpty;
  int get sizeBytes {
    final match = RegExp(r'([\d.]+)\s*(GB|MB|KB)').firstMatch(sizeText);
    if (match == null) return 0;
    final val = double.tryParse(match.group(1) ?? '0') ?? 0;
    final unit = (match.group(2) ?? 'MB').toUpperCase();
    switch (unit) { case 'GB': return (val * 1073741824).toInt(); case 'MB': return (val * 1048576).toInt(); case 'KB': return (val * 1024).toInt(); default: return 0; }
  }
}

class DmhySearchResult {
  final List<DmhyTopic> topics; final bool hasNextPage; final int currentPage;
  const DmhySearchResult({required this.topics, required this.hasNextPage, required this.currentPage});
}
