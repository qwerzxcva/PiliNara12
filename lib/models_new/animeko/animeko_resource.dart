/// Unified resource model for animeko-style data sources.

class AnimekoResource {
  final String id;
  final String sourceId;
  final String title;
  final AnimekoEpisodeRange? episodeRange;
  final String? resolution;
  final String alliance;
  final List<String> subtitleLanguages;
  final String downloadUrl;
  final String originalUrl;
  final AnimekoResourceType type;
  final int sizeBytes;
  final DateTime? publishedAt;
  final String? bangumiSubjectId;

  const AnimekoResource({
    required this.id, required this.sourceId, required this.title,
    this.episodeRange, this.resolution, this.alliance = '',
    this.subtitleLanguages = const [], required this.downloadUrl,
    required this.originalUrl, this.type = AnimekoResourceType.bittorrent,
    this.sizeBytes = 0, this.publishedAt, this.bangumiSubjectId,
  });

  bool matchesEpisode(int episode) {
    if (episodeRange == null) return false;
    return episodeRange!.contains(episode);
  }

  String get sizeString {
    if (sizeBytes == 0) return '未知';
    if (sizeBytes >= 1073741824) return '${(sizeBytes / 1073741824).toStringAsFixed(1)} GB';
    if (sizeBytes >= 1048576) return '${(sizeBytes / 1048576).toStringAsFixed(1)} MB';
    if (sizeBytes >= 1024) return '${(sizeBytes / 1024).toStringAsFixed(1)} KB';
    return '$sizeBytes B';
  }

  @override String toString() => 'AnimekoResource($sourceId, ${episodeRange}, $resolution, $title)';
}

/// Episode range: Single/Range/Season
class AnimekoEpisodeRange {
  final int? start;
  final int? end;
  final bool isSeason;
  final int? seasonNumber;
  const AnimekoEpisodeRange._({this.start, this.end, this.isSeason = false, this.seasonNumber});
  const AnimekoEpisodeRange.single({required int start, int? end}) : this._(start: start, end: end ?? start);
  const AnimekoEpisodeRange.range({required int start, required int end}) : this._(start: start, end: end);
  const AnimekoEpisodeRange.season(int number) : this._(isSeason: true, seasonNumber: number);
  bool contains(int episode) {
    if (isSeason) return true;
    if (start == null) return false;
    return end != null ? episode >= start! && episode <= end! : episode == start;
  }
  bool get isSingle => start != null && end != null && start == end;
  List<int> get episodes {
    if (isSeason || start == null) return [];
    if (end != null) return List.generate(end! - start! + 1, (i) => start! + i);
    return [start!];
  }
  @override String toString() {
    if (isSeason) return 'S$seasonNumber';
    if (isSingle && start != null) return 'Ep$start';
    if (start != null && end != null) return '$start-$end';
    return '?';
  }
}

enum AnimekoResourceType { bittorrent, streaming, webvideo, localcache }
