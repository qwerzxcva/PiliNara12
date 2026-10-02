/// Bangumi API 客户端
/// 基于 Kototoro 的实现，支持 OAuth 认证和多个镜像
import 'package:dio/dio.dart';

class BangumiHttp {
  static const String officialApiUrl = 'https://api.bgm.tv';
  static const String proApiUrl = 'https://api.bangumi.pro';
  
  final String baseUrl;
  final Dio dio;
  String? _accessToken;
  String? _refreshToken;

  BangumiHttp({this.baseUrl = officialApiUrl, Dio? dio}) : dio = dio ?? Dio();

  /// 设置访问令牌
  void setAccessToken(String token) {
    _accessToken = token;
    dio.options.headers['Authorization'] = 'Bearer $token';
  }

  /// 搜索番剧
  Future<List<BangumiSubject>> search(String keyword, {int page = 1, int max = 20}) async {
    try {
      final resp = await dio.get('$baseUrl/v0/search/subject', queryParameters: {
        'keyword': keyword,
        'target_type': 2,
        'page': page,
        'max': max,
      });
      if (resp.data['code'] == 0) {
        final items = resp.data['data'] as List? ?? [];
        return items.map((e) => BangumiSubject.fromJson(e)).toList();
      }
    } catch (e) {
      print('[Bangumi] Search error: $e');
    }
    return [];
  }

  /// 获取番剧详情
  Future<BangumiSubject?> getSubject(int subjectId) async {
    try {
      final resp = await dio.get('$baseUrl/v0/subject/$subjectId');
      if (resp.data['code'] == 0) {
        return BangumiSubject.fromJson(resp.data['data']);
      }
    } catch (e) {
      print('[Bangumi] Get subject error: $e');
    }
    return null;
  }

  /// 获取用户收藏列表
  Future<List<BangumiCollection>> getCollection({required String username, int page = 1}) async {
    try {
      final resp = await dio.get('$baseUrl/v0/user/$username/collection', queryParameters: {'page': page});
      if (resp.data['code'] == 0) {
        final items = resp.data['data'] as List? ?? [];
        return items.map((e) => BangumiCollection.fromJson(e)).toList();
      }
    } catch (e) {
      print('[Bangumi] Get collection error: $e');
    }
    return [];
  }

  /// 更新收藏状态
  Future<bool> updateCollection({required String username, required int subjectId, required int type, String? comment}) async {
    try {
      final resp = await dio.put('$baseUrl/v0/user/$username/collection/$subjectId', data: {
        'type': type,
        if (comment != null) 'comment': comment,
      });
      return resp.data['code'] == 0;
    } catch (e) {
      print('[Bangumi] Update collection error: $e');
      return false;
    }
  }
}

class BangumiSubject {
  final int id;
  final String name;
  final String nameCN;
  final String? summary;
  final String? imageUrl;
  final double? rating;
  final int? eps;

  const BangumiSubject({
    required this.id,
    required this.name,
    required this.nameCN,
    this.summary,
    this.imageUrl,
    this.rating,
    this.eps,
  });

  factory BangumiSubject.fromJson(Map<String, dynamic> json) {
    return BangumiSubject(
      id: json['id'] ?? 0,
      name: json['name'] ?? '',
      nameCN: json['name_cn'] ?? json['name'] ?? '',
      summary: json['summary'],
      imageUrl: json['images']?['large'] ?? json['images']?['medium'],
      rating: json['rating']?['rank']?.toDouble(),
      eps: json['eps'],
    );
  }
}

class BangumiCollection {
  final int subjectId;
  final int type; // 1=想看, 2=在看, 3=看过, 4=搁置, 5=抛弃
  final String? comment;
  final BangumiSubject? subject;

  const BangumiCollection({
    required this.subjectId,
    required this.type,
    this.comment,
    this.subject,
  });

  factory BangumiCollection.fromJson(Map<String, dynamic> json) {
    return BangumiCollection(
      subjectId: json['subject_id'] ?? 0,
      type: json['type'] ?? 2,
      comment: json['comment'],
      subject: json['subject'] != null ? BangumiSubject.fromJson(json['subject']) : null,
    );
  }
}
