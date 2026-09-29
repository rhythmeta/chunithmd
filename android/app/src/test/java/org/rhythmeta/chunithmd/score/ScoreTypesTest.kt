package org.rhythmeta.chunithmd.score

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScoreTypesTest {
    @Test
    fun clearTypesUseApiValuesAndLabels() {
        assertEquals(ClearType.Catastrophy, ClearType.fromWire("catastrophy"))
        assertEquals("CATASTROPHY", ClearType.displayName("catastrophy"))
        assertEquals("CLEAR", ClearType.displayName(null))
    }

    @Test
    fun comboAndChainTypesAreNullable() {
        assertEquals(FullComboType.AllJusticeCritical, FullComboType.fromWire("alljusticecritical"))
        assertEquals("AJC", FullComboType.displayName("alljusticecritical"))
        assertNull(FullComboType.fromWire(null))
        assertEquals(FullChainType.FullChain2, FullChainType.fromWire("fullchain2"))
        assertEquals("金 FULL CHAIN", FullChainType.displayName("fullchain2"))
        assertNull(FullChainType.displayName(null))
    }
}
