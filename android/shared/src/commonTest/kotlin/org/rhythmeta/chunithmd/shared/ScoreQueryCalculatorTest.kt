package org.rhythmeta.chunithmd.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class ScoreQueryCalculatorTest {
    @Test
    fun statusLabelsFollowScoreQueryMapping() {
        assertEquals("FC", displayFullCombo("fc+"))
        assertEquals("AJ", displayFullCombo("ap"))
        assertEquals("AJC", displayFullCombo("ap+"))
        assertEquals("铂 FULL CHAIN", displayFullChain("fs+"))
        assertEquals("金FULL CHAIN", displayFullChain("fdx+"))
    }

    @Test
    fun scoreQueryFilterMatchesCanonicalLabels() {
        val entry = ScoreQueryEntry(
            sheetKey = "song:std:master",
            songId = "song",
            title = "Song",
            artist = "Artist",
            aliases = emptyList(),
            imageName = "song.png",
            difficulty = "master",
            type = "std",
            level = 13.0,
            score = 1_009_000,
            rank = "SSS+",
            rating = 15.15,
            clear = "clear",
            fullCombo = "aj",
            fullChain = "fdx",
        )
        val result = filterAndSortScoreQueryEntries(
            entries = listOf(entry),
            searchText = "song",
            settings = ScoreQueryFilterSettings(
                difficulties = setOf("master"),
                ranks = setOf("SSS+"),
                fullCombos = setOf("AJ"),
                fullChains = setOf("金FULL CHAIN"),
            ),
            sortMode = ScoreQuerySortMode.Rating,
            ascending = false,
        )
        assertEquals(listOf(entry), result)
    }
}
