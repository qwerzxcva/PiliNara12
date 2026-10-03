package com.example.pilinara.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// PiliNara brand colors (pink accent, Bilibili-like)
val PinkPrimary = Color(0xFFFB7299)
val PinkPrimaryDark = Color(0xFFD6497A)

private val LightColors = lightColorScheme(
    primary = PinkPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE1EB),
    onPrimaryContainer = Color(0xFF3E001D),
    secondary = Color(0xFF74565F),
    surface = Color(0xFFFFF8F8),
    background = Color(0xFFFFF8F8),
)

private val DarkColors = darkColorScheme(
    primary = PinkPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5C1133),
    onPrimaryContainer = Color(0xFFFFD9E4),
    secondary = Color(0xFFE3BDC7),
    surface = Color(0xFF171215),
    background = Color(0xFF171215),
)

@Composable
fun PiliNaraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
