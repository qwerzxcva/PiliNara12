import 'dart:async';
import 'package:flutter/material.dart';
import 'package:PiliPlus/common/widgets/image/network_img_layer.dart';

/// 全景背景组件，对标 Kototoro AnimatedPanoramaBackdrop
/// 随滚动产生视差、模糊、渐变效果
class PanoramaBackdrop extends StatefulWidget {
  final String imageUrl;
  final double height;
  final ScrollController scrollController;
  final double blurAmount;
  final double opacity;
  final bool enableAnimation;

  const PanoramaBackdrop({
    super.key,
    required this.imageUrl,
    required this.scrollController,
    this.height = 300,
    this.blurAmount = 35,
    this.opacity = 0.9,
    this.enableAnimation = true,
  });

  @override
  State<PanoramaBackdrop> createState() => _PanoramaBackdropState();
}

class _PanoramaBackdropState extends State<PanoramaBackdrop>
    with SingleTickerProviderStateMixin {
  late AnimationController _animationController;
  late Animation<double> _blurAnimation;
  late Animation<double> _opacityAnimation;
  double _lastScrollOffset = 0;
  Timer? _animationTimer;

  @override
  void initState() {
    super.initState();
    _animationController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 300),
    );
    _blurAnimation = Tween<double>(
      begin: widget.blurAmount,
      end: widget.blurAmount * 0.5,
    ).animate(_animationController);
    _opacityAnimation = Tween<double>(
      begin: widget.opacity,
      end: widget.opacity * 0.7,
    ).animate(_animationController);
    
    widget.scrollController.addListener(_onScroll);
  }

  void _onScroll() {
    final currentOffset = widget.scrollController.offset;
    final delta = currentOffset - _lastScrollOffset;
    
    if (widget.enableAnimation && delta.abs() > 1) {
      _animationController.forward().then((_) {
        _animationController.reverse();
      });
    }
    
    _lastScrollOffset = currentOffset;
    setState(() {});
  }

  @override
  void dispose() {
    widget.scrollController.removeListener(_onScroll);
    _animationTimer?.cancel();
    _animationController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    
    return ClipRect(
      child: SizedBox(
        height: widget.height,
        child: Stack(
          fit: StackFit.expand,
          children: [
            // 背景图片（带视差）
            Transform.translate(
              offset: Offset(0, widget.scrollController.offset * 0.3),
              child: NetworkImgLayer(
                type: NetworkImgType.video,
                src: widget.imageUrl,
                width: double.infinity,
                height: widget.height * 1.5,
                fit: BoxFit.cover,
              ),
            ),
            // 模糊效果
            BackdropFilter(
              filter: ImageFilter.blur(
                sigmaX: _blurAnimation.value,
                sigmaY: _blurAnimation.value,
              ),
              child: Container(
                color: Colors.black.withOpacity(0.3),
              ),
            ),
            // 渐变遮罩
            Positioned(
              bottom: 0,
              left: 0,
              right: 0,
              child: Container(
                height: 120,
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      Colors.transparent,
                      isDark ? Colors.black : Colors.white,
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
