package org.rhythmeta.chunithmd.shared.backup

/** The native preference adapter must never receive a key with the wrong value type. */
object BackupSettings {
    private val kinds = mapOf(
        "theme.color_mode" to "int", "theme.key_color" to "int", "theme.page_scale" to "int",
        "theme.palette_style" to "string", "theme.color_spec" to "string",
        "theme.enable_blur" to "bool", "theme.enable_floating_bottom_bar" to "bool",
        "theme.enable_floating_bottom_bar_blur" to "bool", "theme.enable_predictive_back" to "bool",
        "catalog.sort" to "string", "catalog.ascending" to "bool",
        "catalog.categories" to "strings", "catalog.versions" to "strings",
        "catalog.difficulties" to "strings", "catalog.types" to "strings",
        "catalog.min_level" to "float", "catalog.max_level" to "float",
        "catalog.playable_only" to "bool", "catalog.hide_deleted" to "bool", "catalog.favorites_only" to "bool",
        "best.best_count" to "int", "best.new_count" to "int", "best.selected_version" to "string",
        "plate.selected_version" to "string", "plate.plate_type" to "string", "plate.difficulty" to "string",
        "plate.remaining_only" to "bool",
    )
    fun validate(setting: BackupSetting) {
        if (!setting.key.startsWith("android.chunithmd.")) return
        val kind = kinds[setting.key.removePrefix("android.chunithmd.")]
        require(kind != null && setting.kind == kind) { "Unsupported personal setting: ${setting.key}" }
        when (kind) {
            "int" -> require(setting.integerValue in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong())
            "float" -> require(setting.doubleValue.isFinite() && setting.doubleValue.toFloat().isFinite())
        }
    }
}
