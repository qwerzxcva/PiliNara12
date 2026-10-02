/// Animeko collection (订阅) page.
///
/// Mirrors animeko's CollectionPage - shows user's watched anime with
/// Bangumi sync status and progress tracking.
///
/// This is the "订阅" page the user asked about.

import 'package:PiliPlus/http/bangumi.dart';
import 'package:PiliPlus/models_new/animeko/animeko_resource.dart';
import 'package:PiliPlus/pages/animeko/subject_detail.dart';
import 'package:PiliPlus/utils/storage_pref.dart';
import 'package:flutter/material.dart' as material;
import 'package:get/get.dart';
import 'package:material_ui/material_ui.dart';

class AnimekoCollectionPage extends StatefulWidget {
  const AnimekoCollectionPage({super.key});

  @override
  State<AnimekoCollectionPage> createState() => _AnimekoCollectionPageState();
}

class _AnimekoCollectionPageState extends State<AnimekoCollectionPage> {
  final BangumiHttp _bangumi = BangumiHttp();
  final RxList<BangumiCollection> _collections = RxList<BangumiCollection>([]);
  final RxBool _isLoading = false.obs;
  final RxString _error = ''.obs;
  final RxInt _selectedIndex = 0.obs;

  // Collection types matching animeko's UnifiedCollectionType
  final List<_CollectionTab> _tabs = [
    _CollectionTab('全部', null),
    _CollectionTab('追番中', BangumiCollectionType.watching),
    _CollectionTab('想看', BangumiCollectionType.wantWatch),
    _CollectionTab('看过', BangumiCollectionType.watch),
    _CollectionTab('搁置', BangumiCollectionType.onHold),
    _CollectionTab('抛弃', BangumiCollectionType.dropped),
  ];

  @override
  void initState() {
    super.initState();
    _fetchCollections();
  }

  Future<void> _fetchCollections() async {
    final username = Pref.bangumiUsername;
    if (username == null || username.isEmpty) {
      setState(() => _error.value = '请先在设置中配置 Bangumi 用户名');
      return;
    }

    _isLoading.value = true;
    _error.value = '';

    try {
      final allCollections = await _bangumi.getHistory(username: username, max: 100);
      setState(() => _collections.value = allCollections);
    } catch (e) {
      setState(() => _error.value = '加载失败: $e');
    } finally {
      _isLoading.value = false;
    }
  }

  List<BangumiCollection> get _filteredCollections {
    final type = _tabs[_selectedIndex.value].type;
    if (type == null) return _collections;
    return _collections.where((c) => c.type == type).toList();
  }

  void _updateCollectionStatus(BangumiCollection collection, BangumiCollectionType newType) async {
    final username = Pref.bangumiUsername;
    if (username == null) return;

    final success = await _bangumi.updateCollection(
      username: username,
      subjectId: collection.subjectId,
      type: newType,
    );

    if (success && mounted) {
      // Refresh the list
      await _fetchCollections();
    }
  }

  void _openSubjectDetail(BangumiSubject subject) {
    Get.to(() => AnimekoSubjectDetailPage(
          animeName: subject.nameCN ?? subject.name,
          bangumiSubjectId: subject.id.toString(),
        ));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('番剧订阅'),
        centerTitle: true,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _isLoading.value ? null : _fetchCollections,
          ),
          IconButton(
            icon: const Icon(Icons.settings),
            onPressed: () => Get.toNamed('/animekoSettings'),
          ),
        ],
      ),
      body: Column(
        children: [
          // Tab bar for collection types
          material.TabBar(
            labelColor: Theme.of(context).primaryColor,
            unselectedLabelColor: Colors.grey,
            tabs: _tabs.map((tab) => Tab(text: tab.label)).toList(),
            onTap: (index) => _selectedIndex.value = index,
          ),
          const SizedBox(height: 8),

          // Content
          Expanded(
            child: Obx(() {
              if (_isLoading.value && _collections.isEmpty) {
                return const Center(child: CircularProgressIndicator());
              }
              if (_error.value.isNotEmpty) {
                return Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(Icons.error_outline, size: 48, color: Colors.red),
                      const SizedBox(height: 16),
                      Text(_error.value),
                      const SizedBox(height: 16),
                      ElevatedButton(
                        onPressed: _fetchCollections,
                        child: const Text('重试'),
                      ),
                      const SizedBox(height: 16),
                      if (Pref.bangumiUsername == null)
                        ElevatedButton(
                          onPressed: () => Get.toNamed('/animekoSettings'),
                          child: const Text('配置 Bangumi'),
                        ),
                    ],
                  ),
                );
              }

              final filtered = _filteredCollections;
              if (filtered.isEmpty) {
                return const Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.library_books, size: 64, color: Colors.grey),
                      SizedBox(height: 16),
                      Text('暂无订阅', style: TextStyle(color: Colors.grey)),
                      SizedBox(height: 8),
                      Text('在 Bangumi 收藏番剧后，这里会显示在这里',
                          style: TextStyle(color: Colors.grey, fontSize: 12)),
                    ],
                  ),
                );
              }

              return ListView.builder(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                itemCount: filtered.length,
                itemBuilder: (context, index) {
                  final collection = filtered[index];
                  final subject = collection.subject;
                  if (subject == null) return const SizedBox.shrink();

                  return _CollectionCard(
                    subject: subject,
                    collectionType: collection.type,
                    comment: collection.comment,
                    onPlay: () => _openSubjectDetail(subject),
                    onStatusChange: (newType) => _updateCollectionStatus(collection, newType),
                  );
                },
              );
            }),
          ),
        ],
      ),
    );
  }
}

class _CollectionTab {
  final String label;
  final BangumiCollectionType? type;
  const _CollectionTab(this.label, this.type);
}

class _CollectionCard extends StatelessWidget {
  final BangumiSubject subject;
  final BangumiCollectionType collectionType;
  final String? comment;
  final VoidCallback onPlay;
  final Function(BangumiCollectionType) onStatusChange;

  const _CollectionCard({
    required this.subject,
    required this.collectionType,
    this.comment,
    required this.onPlay,
    required this.onStatusChange,
  });

  @override
  Widget build(BuildContext context) {
    final typeColors = {
      BangumiCollectionType.watching: Colors.green,
      BangumiCollectionType.wantWatch: Colors.blue,
      BangumiCollectionType.watch: Colors.grey,
      BangumiCollectionType.onHold: Colors.orange,
      BangumiCollectionType.dropped: Colors.red,
    };
    final typeLabels = {
      BangumiCollectionType.watching: '追番中',
      BangumiCollectionType.wantWatch: '想看',
      BangumiCollectionType.watch: '看过',
      BangumiCollectionType.onHold: '搁置',
      BangumiCollectionType.dropped: '抛弃',
    };

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: InkWell(
        onTap: onPlay,
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Row(
            children: [
              // Cover
              ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: Image.network(
                  subject.imageUrl ?? '',
                  width: 60,
                  height: 80,
                  fit: BoxFit.cover,
                  errorBuilder: (_, __, ___) => Container(
                    width: 60,
                    height: 80,
                    color: Colors.grey[300],
                    child: const Icon(Icons.movie),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              // Info
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      subject.nameCN ?? subject.name,
                      style: const TextStyle(
                          fontSize: 16, fontWeight: FontWeight.bold),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    if (subject.rating != null)
                      Padding(
                        padding: const EdgeInsets.only(top: 4),
                        child: Row(
                          children: [
                            const Icon(Icons.star, size: 14, color: Colors.amber),
                            const SizedBox(width: 2),
                            Text('${subject.rating}',
                                style: const TextStyle(fontSize: 12)),
                            const SizedBox(width: 8),
                            if (subject.totalEpisodes != null)
                              Text('${subject.totalEpisodes}集',
                                  style: const TextStyle(fontSize: 12, color: Colors.grey)),
                          ],
                        ),
                      ),
                    if ((comment ?? '').isNotEmpty)
                      Padding(
                        padding: const EdgeInsets.only(top: 4),
                        child: Text(
                          comment!,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(fontSize: 12, color: Colors.grey),
                        ),
                      ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              // Status badge
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: typeColors[collectionType]?.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text(
                  typeLabels[collectionType] ?? '',
                  style: TextStyle(
                    fontSize: 12,
                    color: typeColors[collectionType],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
