package org.rhythmeta.chunithmd.ui.theme

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

val LocalEnableBlur = staticCompositionLocalOf { true }
val LocalEnableFloatingBottomBar = staticCompositionLocalOf { true }
val LocalEnableFloatingBottomBarBlur = staticCompositionLocalOf { true }
val LocalEnablePredictiveBack = staticCompositionLocalOf { true }
val LocalPageScale = staticCompositionLocalOf { 1f }

/** Light and dark MIUIX color roles generated for one seed color. */
private class MiuixColorPair(val light: Colors, val dark: Colors)

@SuppressLint("NewApi")
@Composable
fun ChunithmdTheme(
    settings: AppThemeSettings = DefaultAppThemeSettings,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val darkTheme = settings.colorMode.isDark || (settings.colorMode.isSystem && systemDark)
    val effectiveColorSpec = settings.colorSpec.effectiveFor(settings.paletteStyle)
    val seedColor: Color? = when {
        settings.keyColor != 0 -> Color(settings.keyColor)
        settings.colorMode.isMonet && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context).primary else dynamicLightColorScheme(context).primary
        }
        settings.colorMode.isMonet -> Color(0xFF3F8C8E)
        else -> null
    }
    // MIUIX generates palettes only in its Monet modes, and those modes ignore explicitly supplied
    // colors, so the app uses the plain modes for the palettes it generates itself.
    val schemeMode = when (settings.colorMode) {
        ColorMode.SYSTEM, ColorMode.MONET_SYSTEM -> ColorSchemeMode.System
        ColorMode.LIGHT, ColorMode.MONET_LIGHT -> ColorSchemeMode.Light
        ColorMode.DARK, ColorMode.MONET_DARK, ColorMode.DARK_AMOLED -> ColorSchemeMode.Dark
    }
    // MIUIX 0.9.4 generates its Monet palettes against the 5.x material-color-utilities class
    // layout, which no longer matches the 6.x palette engine that provides the CMF style and the
    // 2026 spec. The app therefore generates every dynamic palette itself and hands the resulting
    // roles to the controller as explicit colors.
    val dynamicColors = if (settings.colorMode.isMonet && seedColor != null) {
        remember(seedColor, settings.paletteStyle, effectiveColorSpec) {
            MiuixColorPair(
                light = paletteColorScheme(
                    seedColor = seedColor,
                    isDark = false,
                    style = settings.paletteStyle,
                    spec = effectiveColorSpec,
                ).toMiuixColors(isDark = false),
                dark = paletteColorScheme(
                    seedColor = seedColor,
                    isDark = true,
                    style = settings.paletteStyle,
                    spec = effectiveColorSpec,
                ).toMiuixColors(isDark = true),
            )
        }
    } else {
        null
    }
    val monetController = if (dynamicColors != null) {
        remember(schemeMode, darkTheme, dynamicColors) {
            ThemeController(
                colorSchemeMode = schemeMode,
                lightColors = dynamicColors.light,
                darkColors = dynamicColors.dark,
                isDark = darkTheme,
            )
        }
    } else {
        null
    }
    // Without Monet the app keeps the brand look: the seed-less Miuix palette with the static
    // background and surface overrides.
    val baseColors = if (monetController != null) {
        monetController.currentColors()
    } else {
        remember(schemeMode, darkTheme) {
            ThemeController(
                colorSchemeMode = schemeMode,
                isDark = darkTheme,
            )
        }.currentColors()
    }
    val staticLightColors = remember(baseColors) {
        baseColors.copy(background = Color(0xFFF5F5F5), surface = Color(0xFFF5F5F5))
    }
    val staticDarkColors = remember(baseColors) {
        baseColors.copy(background = Color.Black, surface = Color.Black)
    }
    val staticController = remember(staticLightColors, staticDarkColors, schemeMode, darkTheme) {
        ThemeController(
            colorSchemeMode = schemeMode,
            lightColors = staticLightColors,
            darkColors = staticDarkColors,
            isDark = darkTheme,
        )
    }
    val resolvedController = monetController ?: staticController

    LaunchedEffect(darkTheme) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val materialColors = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF70D5C4), onPrimary = Color(0xFF003A34),
            background = Color.Black,
            surface = Color.Black,
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF16786E), onPrimary = Color.White,
            background = Color(0xFFF5F5F5), surface = Color(0xFFF5F5F5),
        )
    }
    MaterialTheme(colorScheme = materialColors, typography = Typography) {
        MiuixTheme(controller = resolvedController) {
            CompositionLocalProvider(
                LocalEnableBlur provides (settings.enableBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU),
                LocalEnableFloatingBottomBar provides settings.enableFloatingBottomBar,
                LocalEnableFloatingBottomBarBlur provides (
                    settings.enableFloatingBottomBarBlur && settings.enableFloatingBottomBar &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ),
                LocalEnablePredictiveBack provides settings.enablePredictiveBack,
                LocalPageScale provides settings.pageScale,
                content = content,
            )
        }
    }
}
