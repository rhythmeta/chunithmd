package org.rhythmeta.chunithmd.shared.localization

import kotlinx.coroutines.flow.MutableStateFlow

/** Shared source-key catalog. Native hosts set the language before creating their UI. */
object AppStrings {
    private val currentLanguage = MutableStateFlow(AppLanguage.SimplifiedChinese)
    var language: AppLanguage
        get() = currentLanguage.value
        set(value) { currentLanguage.value = value }

    private val placeholder = Regex("\\{(\\d+)\\}")

    fun text(key: String, arguments: List<String> = emptyList()): String =
        textFor(language, key, arguments)

    fun textFor(language: AppLanguage, key: String, arguments: List<String> = emptyList()): String {
        val template = translations[language]?.get(key) ?: key
        // Replace in one pass so user data containing placeholders is never reinterpreted.
        return placeholder.replace(template) { match ->
            arguments.getOrNull(match.groupValues[1].toInt()) ?: match.value
        }
    }
}

fun tr(key: String, vararg arguments: Any?): String = AppStrings.text(key, arguments.map { it?.toString().orEmpty() })
