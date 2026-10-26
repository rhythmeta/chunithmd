package org.rhythmeta.chunithmd.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class SearchTextNormalizerTest {
    @Test
    fun normalizesJapaneseAndChineseVariantFormsToTheSameValue() {
        val pairs = listOf(
            "櫻" to "樱",
            "竜" to "龙",
            "龍" to "龙",
            "國" to "国",
            "髙" to "高",
            "邉" to "边",
            "邊" to "边",
            "神" to "神",
            "福" to "福",
            "宿星審判" to "宿星审判",
            "並" to "并",
            "穎" to "颖",
        )

        pairs.forEach { (variant, canonical) ->
            assertEquals(
                SearchTextNormalizer.normalize(canonical),
                SearchTextNormalizer.normalize(variant),
                "$variant should match $canonical",
            )
        }
    }

    @Test
    fun foldsFullWidthCharactersAndIdeographicSpaces() {
        assertEquals("stage 1", SearchTextNormalizer.normalize("ＳＴＡＧＥ　１"))
    }
}
