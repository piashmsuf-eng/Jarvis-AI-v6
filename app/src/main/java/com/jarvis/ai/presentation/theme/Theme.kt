package com.jarvis.ai.presentation.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Jarvis AI Dark Color Scheme
 * Arc Reactor inspired - Cyan glow on deep dark background
 */
private val JarvisDarkColorScheme = darkColorScheme(
    primary = PrimaryCyan,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryCyanDark,
    onPrimaryContainer = PrimaryCyanLight,
    
    secondary = SecondaryBlue,
    onSecondary = OnPrimaryLight,
    secondaryContainer = SecondaryBlueDark,
    onSecondaryContainer = SecondaryBlueLight,
    
    tertiary = AccentPurple,
    onTertiary = OnPrimaryLight,
    tertiaryContainer = AccentPurple.copy(alpha = 0.3f),
    onTertiaryContainer = AccentPurple,
    
    error = ErrorRed,
    onError = OnPrimaryLight,
    errorContainer = ErrorRed.copy(alpha = 0.3f),
    onErrorContainer = ErrorRed,
    
    background = BackgroundDark,
    onBackground = OnBackgroundLight,
    
    surface = SurfaceDark,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceLight,
    
    outline = PrimaryCyan.copy(alpha = 0.3f),
    outlineVariant = PrimaryCyan.copy(alpha = 0.1f),
    
    scrim = BackgroundDark.copy(alpha = 0.8f),
    
    inverseSurface = OnBackgroundLight,
    inverseOnSurface = BackgroundDark,
    inversePrimary = PrimaryCyanDark,
    
    surfaceTint = PrimaryCyan
)

/**
 * Iron Man Theme - Red & Gold
 */
private val IronManColorScheme = darkColorScheme(
    primary = IronManRed,
    onPrimary = OnPrimaryLight,
    primaryContainer = IronManRed.copy(alpha = 0.3f),
    onPrimaryContainer = IronManGold,
    
    secondary = IronManGold,
    onSecondary = OnPrimaryLight,
    secondaryContainer = IronManGold.copy(alpha = 0.3f),
    onSecondaryContainer = IronManGold,
    
    background = BackgroundDark,
    onBackground = OnBackgroundLight,
    
    surface = SurfaceDark,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceLight
)

/**
 * Hulk Theme - Green
 */
private val HulkColorScheme = darkColorScheme(
    primary = HulkGreen,
    onPrimary = OnPrimaryLight,
    primaryContainer = HulkDark,
    onPrimaryContainer = HulkGreen,
    
    secondary = HulkGreen.copy(alpha = 0.7f),
    onSecondary = OnPrimaryLight,
    
    background = BackgroundDark,
    onBackground = OnBackgroundLight,
    
    surface = SurfaceDark,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceLight
)

/**
 * Thanos Theme - Purple
 */
private val ThanosColorScheme = darkColorScheme(
    primary = ThanosPurple,
    onPrimary = OnPrimaryLight,
    primaryContainer = ThanosDark,
    onPrimaryContainer = ThanosPurple,
    
    secondary = ThanosPurple.copy(alpha = 0.7f),
    onSecondary = OnPrimaryLight,
    
    background = BackgroundDark,
    onBackground = OnBackgroundLight,
    
    surface = SurfaceDark,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceLight
)

enum class JarvisTheme {
    JARVIS_CYAN,
    IRON_MAN,
    HULK,
    THANOS,
    CUSTOM
}

@Composable
fun JarvisAITheme(
    theme: JarvisTheme = JarvisTheme.JARVIS_CYAN,
    customPrimaryColor: androidx.compose.ui.graphics.Color? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        JarvisTheme.JARVIS_CYAN -> JarvisDarkColorScheme
        JarvisTheme.IRON_MAN -> IronManColorScheme
        JarvisTheme.HULK -> HulkColorScheme
        JarvisTheme.THANOS -> ThanosColorScheme
        JarvisTheme.CUSTOM -> {
            if (customPrimaryColor != null) {
                JarvisDarkColorScheme.copy(
                    primary = customPrimaryColor,
                    primaryContainer = customPrimaryColor.copy(alpha = 0.3f),
                    surfaceTint = customPrimaryColor
                )
            } else {
                JarvisDarkColorScheme
            }
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
