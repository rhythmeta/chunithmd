package org.rhythmeta.chunithmd.shared.localization

enum class AppLanguage(val tag: String, val nativeName: String) {
    SimplifiedChinese("zh-Hans", "简体中文"),
    TraditionalChinese("zh-Hant", "繁體中文"),
    English("en", "English"),
    Japanese("ja", "日本語");

    companion object {
        fun fromTag(tag: String): AppLanguage? {
            val parts = tag.lowercase().replace('_', '-').split('-')
            return when (parts.firstOrNull()) {
                "zh" -> when {
                    "hant" in parts -> TraditionalChinese
                    "hans" in parts -> SimplifiedChinese
                    parts.any { it in setOf("tw", "hk", "mo") } -> TraditionalChinese
                    else -> SimplifiedChinese
                }
                "en" -> English
                "ja" -> Japanese
                else -> null
            }
        }

        fun resolve(systemTags: List<String>): AppLanguage =
            systemTags.firstNotNullOfOrNull(::fromTag)
                ?: English
    }
}
