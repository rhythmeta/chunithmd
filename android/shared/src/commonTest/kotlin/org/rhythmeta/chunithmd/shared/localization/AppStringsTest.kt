package org.rhythmeta.chunithmd.shared.localization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppStringsTest {
    @Test fun matchesChineseScriptsBeforeRegions() {
        listOf("zh-TW", "zh-HK", "zh-MO", "zh_Hant", "zh-Hant-CN").forEach {
            assertEquals(AppLanguage.TraditionalChinese, AppLanguage.fromTag(it), it)
        }
        listOf("zh", "zh-CN", "zh-SG", "zh-Hans-TW").forEach {
            assertEquals(AppLanguage.SimplifiedChinese, AppLanguage.fromTag(it), it)
        }
    }

    @Test fun respectsSystemLanguagePriorityAndFallsBackToEnglish() {
        assertEquals(AppLanguage.Japanese, AppLanguage.resolve(listOf("fr-FR", "ja-JP", "en-US")))
        assertEquals(AppLanguage.English, AppLanguage.resolve(listOf("en-GB", "zh-TW")))
        assertEquals(AppLanguage.English, AppLanguage.resolve(listOf("de-DE")))
        assertEquals(AppLanguage.English, AppLanguage.resolve(emptyList()))
    }

    @Test fun catalogHasIdenticalKeysAndPlaceholdersInEveryLanguage() {
        val source = translations.getValue(AppLanguage.SimplifiedChinese)
        val placeholder = Regex("\\{\\d+\\}")
        AppLanguage.entries.forEach { language ->
            val target = translations.getValue(language)
            assertEquals(source.keys, target.keys)
            source.forEach { (key, text) ->
                assertTrue(target.getValue(key).isNotBlank())
                assertEquals(
                    placeholder.findAll(text).map { it.value }.sorted().toList(),
                    placeholder.findAll(target.getValue(key)).map { it.value }.sorted().toList(),
                    "$language: $key",
                )
            }
        }
    }

    @Test fun formatsArgumentsWithoutTranslatingOrReinterpretingUserContent() {
        assertEquals("3 charts", AppStrings.textFor(AppLanguage.English, "{0} 张谱面", listOf("3")))
        assertEquals("候補の別名 2 件", AppStrings.textFor(AppLanguage.Japanese, "{0} 个候选别名", listOf("2")))
        assertEquals("版本：{1} · 建置時間：x", AppStrings.textFor(AppLanguage.TraditionalChinese, "版本：{0} · 构建：{1}", listOf("{1}", "x")))
        assertEquals("My song {1}", AppStrings.textFor(AppLanguage.Japanese, "My song {1}"))
        assertEquals("Working in progress", AppStrings.textFor(AppLanguage.Japanese, "Working in progress"))
    }

    @Test fun displayLocalizationDoesNotChangeStoredChainAndFilterValues() {
        assertEquals("Gold FC", AppStrings.textFor(AppLanguage.English, "金 FC"))
        assertEquals("金 FC", org.rhythmeta.chunithmd.shared.displayFullChain("fullchain2"))
        assertEquals("fullchain2", org.rhythmeta.chunithmd.shared.FullChainType.FullChain2.wireValue)
    }
}
