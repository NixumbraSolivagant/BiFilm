package com.bifilm.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity

/**
 * 浅色: 暖奶白纸 + 金琥珀 accent + 深色文字.
 * "暗房透出暖光"的氛围.
 */
private val LightColors = lightColorScheme(
    primary = FilmGold,                  // 主交互色: 琥珀金
    onPrimary = InkBlack,                // 主交互上的文字: 黑
    primaryContainer = PaperDim,         // 主容器: 米色
    onPrimaryContainer = InkPrimary,

    secondary = FilmAmber,
    onSecondary = InkBlack,
    secondaryContainer = FilmGlow,
    onSecondaryContainer = FilmShadow,

    tertiary = InkSecondary,
    onTertiary = Paper,

    background = Paper,
    onBackground = InkPrimary,
    surface = SurfaceLight,              // #FCFAF1
    onSurface = InkPrimary,
    surfaceVariant = PaperDim,           // #E2DCC8  (卡片底 / chip 底)
    onSurfaceVariant = InkSecondary,

    outline = PaperShadow,               // #C9C2AC (淡描边)
    outlineVariant = PaperDim,
    error = ErrorRed,
    onError = Paper
)

/**
 * 暗色: 深炭底 + 金琥珀 accent + 米白文字.
 * "冲洗暗房"的氛围.
 */
private val DarkColors = darkColorScheme(
    primary = FilmGold,                  // 琥珀金依然是主交互
    onPrimary = InkBlack,
    primaryContainer = FilmShadow,
    onPrimaryContainer = FilmGlow,

    secondary = FilmGlow,
    onSecondary = InkBlack,
    secondaryContainer = FilmShadow,
    onSecondaryContainer = FilmGlow,

    tertiary = PaperDim,
    onTertiary = InkPrimary,

    background = InkBlack,               // #0B0B0E 极深
    onBackground = OnDarkPrimary,
    surface = SurfaceDark,               // #0F1116 主 surface
    onSurface = OnDarkPrimary,
    surfaceVariant = SurfaceDarkVariant, // #252934 卡片底 / chip
    onSurfaceVariant = OnDarkSecondary,

    outline = InkSecondary,
    outlineVariant = SurfaceDarkHigh,    // #22262E (卡片之间描边)
    error = ErrorRed,
    onError = Paper
)

@Composable
fun BiFilmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    // 状态栏配色: 跟随主题. 浅色用深色状态栏字, 暗色用浅色状态栏字.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = BiFilmTypography,
        content = content
    )
}