package com.example.xposuredetectorsmart.ui.theme

import android.app.Activity
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

private val DarkColorScheme = darkColorScheme(
    primary = SignalCyan,
    onPrimary = Color.Black,
    secondary = SignalCyanDim,
    onSecondary = Color.Black,
    tertiary = StatusWarning,
    onTertiary = Color.Black,
    error = StatusCritical,
    onError = Color.White,
    background = SurfaceDarkBg,
    onBackground = OnSurfaceDark,
    surface = SurfaceDarkCard,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceDarkCard,
    onSurfaceVariant = OnSurfaceDarkMuted,
    outline = SurfaceDarkOutline,
)

private val LightColorScheme = lightColorScheme(
    primary = SignalCyanDim,
    onPrimary = Color.White,
    secondary = SignalCyan,
    onSecondary = Color.Black,
    tertiary = StatusWarning,
    onTertiary = Color.Black,
    error = StatusCritical,
    onError = Color.White,
    background = SurfaceLightBg,
    onBackground = OnSurfaceLight,
    surface = SurfaceLightCard,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceLightCard,
    onSurfaceVariant = OnSurfaceLightMuted,
    outline = SurfaceLightOutline,
)

@Composable
fun XposureDetectorSmartTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is intentionally off by default: this app has its own brand
    // palette (Signal Cyan) and letting Android 12+ wallpaper-derived colors take
    // over would hide it on most devices.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
