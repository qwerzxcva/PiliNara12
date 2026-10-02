/// Animeko 资源模型 - 支持 B 站 PGC 和 BT 源
class AnimekoResource {
  final String id;
  final String source; // "bilibili" | "mikan" | "dmhy"
  final String title;
  final String? cover;
  final int? seasonId; // B 站 season ID
  final int? epId; // B 站 episode ID
  final String? magnetUrl; // BT 磁力链接
  final String? bangumiId; // Bangumi 条目 ID
  final double? rating; // Bangumi 评分
  final List<String>? tags; // 标签

  const AnimekoResource({
    required this.id,
    required this.source,
    required this.title,
    this.cover,
    this.seasonId,
    this.epId,
    this.magnetUrl,
    this.bangumiId,
    this.rating,
    this.tags,
  });

  /// 是否可播放（B 站资源）
  bool get canPlay => source == 'bilibili' && seasonId != null && epId != null;

  /// 是否为 BT 资源
  bool get isTorrent => magnetUrl != null;
}
