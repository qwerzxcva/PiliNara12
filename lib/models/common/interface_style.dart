import 'package:PiliPlus/models/common/enum_with_label.dart';

/// 界面风格，对标 Kototoro InterfaceStyle
/// MATERIAL_3_EXPRESSIVE: 圆角更大，Material You 风格
/// IOS_STYLE: iOS 风格，玻璃态效果
enum InterfaceStyle with EnumWithLabel {
  material3Expressive('Material 3 表现'),
  ios('iOS 风格'),
  ;

  @override
  final String label;
  const InterfaceStyle(this.label);

  /// 圆角大小（dp）
  double get groupCornerRadius => switch (this) {
        InterfaceStyle.material3Expressive => 18.0,
        InterfaceStyle.ios => 28.0,
      };

  /// 控件圆角（dp）
  double get controlCornerRadius => switch (this) {
        InterfaceStyle.material3Expressive => 12.0,
        InterfaceStyle.ios => 20.0,
      };

  /// 是否使用 iOS 风格
  bool get isIosStyle => this == InterfaceStyle.ios;

  /// 是否使用 Material 3 Expressive
  bool get isMaterial3Expressive => this == InterfaceStyle.material3Expressive;
}
