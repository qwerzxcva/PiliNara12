import 'package:PiliPlus/models/common/enum_with_label.dart';

/// HDR 处理模式
enum HdrMode with EnumWithLabel {
  /// 自动模式：检测视频是否为 HDR，是则直通，否则不进行转换
  auto('自动'),

  /// 强制 SDR 转 HDR：对所有视频进行色调映射到 HDR
  sdrToHdr('SDR→HDR'),

  /// 禁用 HDR 处理：保持原始输出
  disabled('禁用'),
  ;

  @override
  final String label;
  const HdrMode(this.label);
}

/// 色调映射算法
enum ToneMappingAlgorithm with EnumWithLabel {
  /// 默认算法（Media3 内置）
  default_('默认'),

  /// Reinhard 曲线（保守，保留更多细节）
  reinhard('Reinhard'),

  /// Mobius 曲线（平衡）
  mobius('Mobius'),

  /// Custom 曲线（通过参数自定义）
  custom('自定义'),
  ;

  @override
  final String label;
  const ToneMappingAlgorithm(this.label);
}

/// 抖动算法
enum DitherAlgorithm with EnumWithLabel {
  /// 禁用抖动
  disabled('禁用'),

  /// Floyd-Steinberg 误差扩散（质量最好）
  floydSteinberg('Floyd-Steinberg'),

  /// Ordered dithering（性能最好）
  ordered('Ordered'),
  ;

  @override
  final String label;
  const DitherAlgorithm(this.label);
}
