package com.example.pilinara.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.pilinara.utils.StorageManager

// ==================== 批次L：Kototoro 风格主题 ====================

// PiliNara brand colors（保留旧引用兼容）
val PinkPrimary = Color(0xFFFB7299)
val PinkPrimaryDark = Color(0xFFD6497A)

/** 主题强调色选项（对齐 Flutter colorThemeTypes） */
data class AccentOption(val color: Color, val label: String, val hex: String)

val ACCENT_OPTIONS: List<AccentOption> = listOf(
    AccentOption(Color(0xFF5CB67B), "默认绿", "#5CB67B"),
    AccentOption(Color(0xFFFB7299), "哔哩粉", "#FB7299"),
    AccentOption(Color(0xFFF44336), "红色", "#F44336"),
    AccentOption(Color(0xFFFF9800), "橙色", "#FF9800"),
    AccentOption(Color(0xFFFFC107), "琥珀色", "#FFC107"),
    AccentOption(Color(0xFFFFEB3B), "黄色", "#FFEB3B"),
    AccentOption(Color(0xFFCDDC39), "酸橙色", "#CDDC39"),
    AccentOption(Color(0xFF8BC34A), "浅绿色", "#8BC34A"),
    AccentOption(Color(0xFF4CAF50), "绿色", "#4CAF50"),
    AccentOption(Color(0xFF009688), "青色", "#009688"),
    AccentOption(Color(0xFF00BCD4), "蓝绿色", "#00BCD4"),
    AccentOption(Color(0xFF03A9F4), "浅蓝色", "#03A9F4"),
    AccentOption(Color(0xFF2196F3), "蓝色", "#2196F3"),
    AccentOption(Color(0xFF3F51B5), "靛蓝色", "#3F51B5"),
    AccentOption(Color(0xFF9C27B0), "紫色", "#9C27B0"),
    AccentOption(Color(0xFF673AB7), "深紫色", "#673AB7"),
    AccentOption(Color(0xFF607D8B), "蓝灰色", "#607D8B"),
    AccentOption(Color(0xFF795548), "棕色", "#795548"),
    AccentOption(Color(0xFF9E9E9E), "灰色", "#9E9E9E"),
)

/** 解析存储的十六进制强调色（空/未知 = 默认） */
fun accentFromHex(hex: String): AccentOption =
    ACCENT_OPTIONS.firstOrNull { it.hex.equals(hex, ignoreCase = true) } ?: ACCENT_OPTIONS[0]

private fun Color.darken(f: Float = 0.25f) = Color(
    red = (red * (1 - f)).coerceIn(0f, 1f),
    green = (green * (1 - f)).coerceIn(0f, 1f),
    blue = (blue * (1 - f)).coerceIn(0f, 1f),
    alpha = alpha
)

private fun Color.lighten(f: Float = 0.25f) = Color(
    red = (red + (1 - red) * f).coerceIn(0f, 1f),
    green = (green + (1 - green) * f).coerceIn(0f, 1f),
    blue = (blue + (1 - blue) * f).coerceIn(0f, 1f),
    alpha = alpha
)

private fun Color.compositeOver(background: Color): Color {
    val a = alpha + background.alpha * (1 - alpha)
    if (a == 0f) return Color.Transparent
    return Color(
        red = (red * alpha + background.red * background.alpha * (1 - alpha)) / a,
        green = (green * alpha + background.green * background.alpha * (1 - alpha)) / a,
        blue = (blue * alpha + background.blue * background.alpha * (1 - alpha)) / a,
        alpha = a
    )
}

/** 由强调色派生 Material3 scheme（Kototoro 风格：柔和 surface + 鲜明 primary） */
private fun lightSchemeOf(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.16f).compositeOver(Color.White),
    onPrimaryContainer = accent.darken(),
    secondary = accent.darken(0.15f),
    surface = Color(0xFFFFFBFF),
    background = Color(0xFFFFFBFF),
)

private fun darkSchemeOf(accent: Color) = darkColorScheme(
    primary = accent.lighten(),
    onPrimary = Color(0xFF16121A),
    primaryContainer = accent.copy(alpha = 0.32f).compositeOver(Color(0xFF121212)),
    onPrimaryContainer = accent.lighten(0.4f),
    secondary = accent.lighten(0.2f),
    surface = Color(0xFF16121A),
    background = Color(0xFF16121A),
)

/** Kototoro 风格形状 token：大圆角 */
val PiliShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** 品牌渐变 token（头图/封面遮罩/横幅） */
object PiliGradients {
    fun brand(accent: Color) = Brush.verticalGradient(
        colors = listOf(accent.copy(alpha = 0.85f), accent.copy(alpha = 0.15f))
    )

    fun coverScrim() = Brush.verticalGradient(
        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
    )

    val bilibili = Brush.linearGradient(
        colors = listOf(Color(0xFFFB7299), Color(0xFF23ADE5))
    )
}

@Composable
fun PiliNaraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // DataStore 消费：themeMode（system/light/dark）+ accentColor 真实作用于全局主题
    val context = LocalContext.current
    val storage = remember(context) { StorageManager(context) }
    val themeMode by storage.themeModeFlow.collectAsState(initial = "system")
    val accentHex by storage.accentColorFlow.collectAsState(initial = "")

    val isDark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> darkTheme
    }
    val accent = accentFromHex(accentHex).color

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> darkSchemeOf(accent)
        else -> lightSchemeOf(accent)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = PiliShapes,
        content = content
    )
}
