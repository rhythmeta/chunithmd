package org.rhythmeta.chunithmd.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChunithmScoreRulesTest {
    @Test
    fun parsesManualScoresWithoutSilentlyCorrectingInvalidInput() {
        mapOf("0" to 0, "1010000" to 1_010_000, "1,010,000" to 1_010_000,
            " １，００７，５００ " to 1_007_500).forEach { (text, expected) ->
            assertEquals(expected, ChunithmScoreRules.parseEntryScore(text), text)
        }
        listOf("", " ", "-1", "1.5", "1,00,000", "1 000 000", "1O00000", "score:1000000",
            "1010001", "99999999999999999999").forEach {
            assertNull(ChunithmScoreRules.parseEntryScore(it), it)
        }
    }

    @Test
    fun validatesTheChunithmScoreRange() {
        assertTrue(ChunithmScoreRules.isValid(0))
        assertTrue(ChunithmScoreRules.isValid(1_010_000))
        assertFalse(ChunithmScoreRules.isValid(-1))
        assertFalse(ChunithmScoreRules.isValid(1_010_001))
    }

    @Test
    fun mapsScoresToChunithmRanks() {
        assertEquals("D", ChunithmScoreRules.rank(499_999))
        assertEquals("C", ChunithmScoreRules.rank(500_000))
        assertEquals("AAA", ChunithmScoreRules.rank(950_000))
        assertEquals("S", ChunithmScoreRules.rank(975_000))
        assertEquals("SSS+", ChunithmScoreRules.rank(1_009_923))
        assertEquals("SSS+", ChunithmScoreRules.rank(1_010_000))
    }
}
