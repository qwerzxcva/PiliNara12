/// Animeko 服务 - 整合 B 站 PGC
import 'package:PiliPlus/http/pgc.dart';
import 'package:PiliPlus/http/loading_state.dart';
import 'package:PiliPlus/models_new/pgc/pgc_index_result/list.dart';
import 'package:PiliPlus/models_new/pgc/pgc_timeline/result.dart';
import 'package:flutter/foundation.dart';
import 'package:get/get.dart';

class AnimekoService extends GetxController {
  final RxList<PgcIndexItem> subscriptions = RxList<PgcIndexItem>([]);
  final RxList<TimelineResult> timeline = RxList<TimelineResult>([]);
  final RxBool isLoading = false.obs;
  final RxString errorMsg = ''.obs;

  Future<void> init() async {
    isLoading.value = true;
    errorMsg.value = '';
    
    try {
      await _loadSubscriptions();
      await _loadTimeline();
    } catch (e) {
      errorMsg.value = '加载失败: $e';
      if (kDebugMode) debugPrint('[AnimekoService] Init error: $e');
    } finally {
      isLoading.value = false;
    }
  }

  Future<void> _loadSubscriptions() async {
    final res = await PgcHttp.pgcIndex(page: 1);
    if (res case Success(:final data)) {
      subscriptions.value = data ?? [];
    }
  }

  Future<void> _loadTimeline() async {
    final res = await PgcHttp.pgcTimeline(types: 1, before: 7, after: 7);
    if (res case Success(:final data)) {
      timeline.value = data ?? [];
    }
  }

  Future<void> addSubscription(PgcIndexItem item) async {
    if (!subscriptions.any((s) => s.seasonId == item.seasonId)) {
      subscriptions.add(item);
    }
  }

  Future<void> removeSubscription(int seasonId) async {
    subscriptions.removeWhere((s) => s.seasonId == seasonId);
  }

  bool isSubscribed(int seasonId) {
    return subscriptions.any((s) => s.seasonId == seasonId);
  }
}
