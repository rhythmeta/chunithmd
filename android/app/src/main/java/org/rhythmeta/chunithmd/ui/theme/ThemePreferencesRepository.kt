package org.rhythmeta.chunithmd.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal val Context.themePreferencesDataStore by preferencesDataStore(name = "theme_preferences")

class ThemePreferencesRepository(private val context: Context) {
    val settings: Flow<AppThemeSettings> = context.themePreferencesDataStore.data.map { values ->
        val style = values[PaletteStyleKey]?.let { runCatching { PaletteStyle.valueOf(it) }.getOrNull() }
            ?: DefaultAppThemeSettings.paletteStyle
        val spec = values[ColorSpecKey]?.let { runCatching { ColorSpec.SpecVersion.valueOf(it) }.getOrNull() }
            ?: DefaultAppThemeSettings.colorSpec
        AppThemeSettings(
            colorMode = ColorMode.fromValue(values[ColorModeKey] ?: ColorMode.SYSTEM.value),
            keyColor = values[KeyColorKey] ?: 0,
            paletteStyle = style,
            colorSpec = spec.effectiveFor(style),
            enableBlur = values[BlurKey] ?: true,
            enableFloatingBottomBar = values[FloatingBarKey] ?: true,
            enableFloatingBottomBarBlur = values[FloatingBarBlurKey] ?: true,
            enablePredictiveBack = values[PredictiveBackKey] ?: true,
            pageScale = ((values[PageScaleKey] ?: 100) / 100f).coerceIn(0.8f, 1.1f),
            showScannerBoundingBoxes = values[ScannerBoundingBoxesKey] ?: false,
        )
    }

    suspend fun setColorMode(value: ColorMode) = update { it.copy(colorMode = value) }
    suspend fun setKeyColor(value: Int) = update { it.copy(keyColor = value) }
    suspend fun setPaletteStyle(value: PaletteStyle) = update { it.copy(paletteStyle = value) }
    suspend fun setColorSpec(value: ColorSpec.SpecVersion) = update { it.copy(colorSpec = value) }
    suspend fun setBlur(value: Boolean) = update { it.copy(enableBlur = value) }
    suspend fun setFloatingBar(value: Boolean) = update {
        it.copy(enableFloatingBottomBar = value, enableFloatingBottomBarBlur = it.enableFloatingBottomBarBlur && value)
    }
    suspend fun setFloatingBarBlur(value: Boolean) = update { it.copy(enableFloatingBottomBarBlur = value && it.enableFloatingBottomBar) }
    suspend fun setPredictiveBack(value: Boolean) = update { it.copy(enablePredictiveBack = value) }
    suspend fun setPageScale(value: Float) = update { it.copy(pageScale = value.coerceIn(0.8f, 1.1f)) }
    suspend fun setShowScannerBoundingBoxes(value: Boolean) {
        context.themePreferencesDataStore.edit { it[ScannerBoundingBoxesKey] = value }
    }

    private suspend fun update(transform: (AppThemeSettings) -> AppThemeSettings) {
        val current = settings.first()
        context.themePreferencesDataStore.edit { values ->
            val next = transform(current)
            values[ColorModeKey] = next.colorMode.value
            values[KeyColorKey] = next.keyColor
            values[PaletteStyleKey] = next.paletteStyle.name
            values[ColorSpecKey] = next.colorSpec.effectiveFor(next.paletteStyle).name
            values[BlurKey] = next.enableBlur
            values[FloatingBarKey] = next.enableFloatingBottomBar
            values[FloatingBarBlurKey] = next.enableFloatingBottomBarBlur
            values[PredictiveBackKey] = next.enablePredictiveBack
            values[PageScaleKey] = (next.pageScale * 100f).toInt()
        }
    }

    private companion object {
        val ColorModeKey = intPreferencesKey("color_mode")
        val KeyColorKey = intPreferencesKey("key_color")
        val PaletteStyleKey = stringPreferencesKey("palette_style")
        val ColorSpecKey = stringPreferencesKey("color_spec")
        val BlurKey = booleanPreferencesKey("enable_blur")
        val FloatingBarKey = booleanPreferencesKey("enable_floating_bottom_bar")
        val FloatingBarBlurKey = booleanPreferencesKey("enable_floating_bottom_bar_blur")
        val PredictiveBackKey = booleanPreferencesKey("enable_predictive_back")
        val PageScaleKey = intPreferencesKey("page_scale")
        val ScannerBoundingBoxesKey = booleanPreferencesKey("show_scanner_bounding_boxes")
    }
}
