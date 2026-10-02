import 'package:PiliPlus/common/widgets/image/network_img_layer.dart';
import 'package:PiliPlus/pages/home_hero/controller.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:material_ui/material_ui.dart';

class HomeHeroSection extends StatelessWidget {
  const HomeHeroSection({super.key});

  @override
  Widget build(BuildContext context) {
    final controller = Get.put(HomeHeroController());
    final size = MediaQuery.sizeOf(context);
    final isDark = Theme.of(context).brightness == Brightness.dark;
    
    return Obx(() {
      if (controller.entries.isEmpty) return const SizedBox.shrink();
      
      return SizedBox(
        height: 220,
        child: Stack(
          children: [
            // Hero 轮播
            PageView(
              onPageChanged: controller.setIndex,
              children: controller.entries.map((entry) => _HeroCard(entry)).toList(),
            ),
            // 底部渐变遮罩
            Positioned(
              bottom: 0,
              left: 0,
              right: 0,
              child: Container(
                height: 80,
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      Colors.transparent,
                      isDark ? Colors.black.withOpacity(0.8) : Colors.white.withOpacity(0.8),
                    ],
                  ),
                ),
              ),
            ),
            // 指示器
            if (controller.entries.length > 1)
              Positioned(
                bottom: 12,
                right: 16,
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: List.generate(
                    controller.entries.length,
                    (index) => Container(
                      margin: const EdgeInsets.symmetric(horizontal: 2),
                      width: 6,
                      height: 6,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: index == controller.currentIndex.value
                            ? (isDark ? Colors.white : Colors.black)
                            : (isDark ? Colors.white.withOpacity(0.4) : Colors.black.withOpacity(0.4)),
                      ),
                    ),
                  ),
                ),
              ),
          ],
        ),
      );
    });
  }
}

class _HeroCard extends StatelessWidget {
  final HomeHeroEntry entry;
  const _HeroCard(this.entry);

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => Get.toNamed(entry.route),
      child: Stack(
        fit: StackFit.expand,
        children: [
          NetworkImgLayer(
            type: NetworkImgType.video,
            src: entry.coverUrl,
            width: double.infinity,
            height: double.infinity,
            fit: BoxFit.cover,
          ),
          // 标题叠加
          Positioned(
            bottom: 16,
            left: 16,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  entry.title,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                    shadows: [Shadow(color: Colors.black45, blurRadius: 4)],
                  ),
                ),
                if (entry.subtitle.isNotEmpty)
                  Text(
                    entry.subtitle,
                    style: const TextStyle(
                      color: Colors.white70,
                      fontSize: 12,
                      shadows: [Shadow(color: Colors.black45, blurRadius: 2)],
                    ),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
