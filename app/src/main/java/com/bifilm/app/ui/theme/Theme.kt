package com.bifilm.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = InkPrimary,
    onPrimary = Paper,
    primaryContainer = PaperDim,
    onPrimaryContainer = InkPrimary,

    secondary = FilmWarm,
    onSecondary = Paper,
    secondaryContainer = FilmGlow,
    onSecondaryContainer = FilmShadow,

    tertiary = InkSecondary,
    onTertiary = Paper,

    background = Paper,
    onBackground = InkPrimary,
    surface = SurfaceLight,
    onSurface = InkPrimary,
    surfaceVariant = PaperDim,
    onSurfaceVariant = InkSecondary,

    outline = PaperShadow,
    outlineVariant = PaperDim,
    error = ErrorRed,
    onError = Paper
)

private val DarkColors = darkColorScheme(
    primary = FilmGlow,
    onPrimary = InkPrimary,
    primaryContainer = FilmShadow,
    onPrimaryContainer = Paper,

    secondary = FilmWarm,
    onSecondary = InkPrimary,
    secondaryContainer = FilmShadow,
    onSecondaryContainer = FilmGlow,

    tertiary = PaperDim,
    onTertiary = InkPrimary,

    background = InkPrimary,
    onBackground = Paper,
    surface = SurfaceDark,
    onSurface = Paper,
    surfaceVariant = InkSecondary,
    onSurfaceVariant = PaperDim,

    outline = InkSecondary,
    outlineVariant = InkSecondary,
    error = ErrorRed,
    onError = Paper
)

@Composable
fun BiFilmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = BiFilmTypography,
        content = content
    )
}
