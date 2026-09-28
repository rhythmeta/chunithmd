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
import com.materialkolor.dynamiccolor.ColorSpec
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

val LocalEnableBlur = staticCompositionLocalOf { true }
val LocalEnableFloatingBottomBar = staticCompositionLocalOf { true }
val LocalEnableFloatingBottomBarBlur = staticCompositionLocalOf { true }
val LocalEnablePredictiveBack = staticCompositionLocalOf { true }
val LocalPageScale = staticCompositionLocalOf { 1f }

@SuppressLint("NewApi")
@Composable
fun ChunithmdTheme(
    settings: AppThemeSettings = DefaultAppThemeSettings,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val darkTheme = settings.colorMode.isDark || (settings.colorMode.isSystem && systemDark)
    val paletteStyle = runCatching { ThemePaletteStyle.valueOf(settings.paletteStyle.name) }
        .getOrDefault(ThemePaletteStyle.TonalSpot)
    val colorSpec = when (settings.colorSpec.effectiveFor(settings.paletteStyle)) {
        ColorSpec.SpecVersion.SPEC_2025 -> ThemeColorSpec.Spec2025
        ColorSpec.SpecVersion.SPEC_2021 -> ThemeColorSpec.Spec2021
    }
    val seedColor: Color? = when {
        settings.keyColor != 0 -> Color(settings.keyColor)
        settings.colorMode.isMonet && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context).primary else dynamicLightColorScheme(context).primary
        }
        settings.colorMode.isMonet -> Color(0xFF3F8C8E)
        else -> null
    }
    val schemeMode = when (settings.colorMode) {
        ColorMode.SYSTEM -> ColorSchemeMode.System
        ColorMode.LIGHT -> ColorSchemeMode.Light
        ColorMode.DARK -> ColorSchemeMode.Dark
        ColorMode.MONET_SYSTEM -> ColorSchemeMode.MonetSystem
        ColorMode.MONET_LIGHT -> ColorSchemeMode.MonetLight
        ColorMode.MONET_DARK, ColorMode.DARK_AMOLED -> ColorSchemeMode.MonetDark
    }
    val baseController = remember(settings, darkTheme, seedColor, paletteStyle, colorSpec) {
        ThemeController(
            colorSchemeMode = schemeMode,
            keyColor = seedColor,
            colorSpec = colorSpec,
            paletteStyle = paletteStyle,
            isDark = darkTheme,
        )
    }
    val generatedColors = baseController.currentColors()
    val staticLightColors = remember(generatedColors) {
        generatedColors.copy(background = Color(0xFFF5F5F5), surface = Color(0xFFF5F5F5))
    }
    val staticDarkColors = remember(generatedColors) {
        generatedColors.copy(background = Color.Black, surface = Color.Black)
    }
    val staticController = remember(baseController, staticLightColors, staticDarkColors, settings.colorMode) {
        ThemeController(
            colorSchemeMode = schemeMode,
            keyColor = seedColor,
            colorSpec = colorSpec,
            paletteStyle = paletteStyle,
            lightColors = staticLightColors,
            darkColors = staticDarkColors,
            isDark = darkTheme,
        )
    }
    val resolvedController = if (settings.colorMode.isMonet) baseController else staticController

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
