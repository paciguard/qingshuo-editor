package com.qingshuo.editor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = BrandPink,
    onPrimary = OnSurfaceDark,
    secondary = BrandPinkDark,
    background = BgDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = OnSurfaceMuted
)

private val LightColors = lightColorScheme(
    primary = BrandPink,
    secondary = BrandPinkDark
)

@Composable
fun QingshuoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // The app is designed dark-first like CapCut. Force dark for now.
    val colors = DarkColors
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
