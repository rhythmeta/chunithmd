package org.rhythmeta.chunithmd.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChunithmScoreRulesTest {
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
