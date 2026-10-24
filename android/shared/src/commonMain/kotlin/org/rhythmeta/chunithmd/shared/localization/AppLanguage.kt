package org.rhythmeta.chunithmd.shared.localization

enum class AppLanguage(val tag: String) {
    SimplifiedChinese("zh-Hans"),
    TraditionalChinese("zh-Hant"),
    English("en"),
    Japanese("ja");

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
