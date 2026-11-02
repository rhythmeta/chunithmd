package org.rhythmeta.chunithmd.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScoreModelsTest {
    @Test
    fun statusTypesUseStableWireValues() {
        assertEquals(ClearType.Catastrophy, ClearType.fromWire("catastrophy"))
        assertEquals("CATASTROPHY", ClearType.displayName("catastrophy"))
        assertNull(ClearType.fromWire(null))
        assertNull(ClearType.displayName(null))
        assertNull(ClearType.displayName(""))
        assertEquals(FullComboType.AllJusticeCritical, FullComboType.fromWire("alljusticecritical"))
        assertEquals("AJC", FullComboType.displayName("alljusticecritical"))
        assertNull(FullComboType.fromWire(null))
        assertEquals(FullChainType.FullChain2, FullChainType.fromWire("fullchain2"))
        assertEquals("金 FC", FullChainType.displayName("fullchain2"))
        assertNull(FullChainType.displayName(null))
    }

    @Test
    fun preferencesNormalizeForSharedConsumers() {
        assertEquals(
            BestTablePreferences(1, 99, "version"),
            BestTablePreferences(0, 100, " version ").normalized(),
        )
    }

    @Test
    fun progressSheetsAndScoreProgressUseStableChartKeys() {
        val song = CatalogSong(
            songId = "song",
            title = "Song",
            sheets = listOf(
                CatalogSheet("std", "master", regions = mapOf("jp" to true)),
                CatalogSheet("std", "master", regions = mapOf("jp" to true)),
                CatalogSheet("we", "world's end", regions = mapOf("jp" to true)),
            ),
        )
        val sheets = song.progressSheets()
        assertEquals(listOf("we", "std"), sheets.map { it.type })
        assertEquals("song:std:master", song.sheetKey(sheets.last()))
        assertEquals(0f, scoreProgress(999_999))
        assertEquals(0.5f, scoreProgress(1_005_000))
        assertEquals(1f, scoreProgress(1_010_000))
        assertEquals(0f, scoreRingProgress(974_999))
        assertEquals(0.5f, scoreRingProgress(987_500))
        assertEquals(1f, scoreRingProgress(1_000_000))
        assertEquals(1f, scoreRingProgress(1_010_000))
        assertEquals(ChartProgress(ring = 0f, dot = 0f), chartProgress(null))
        assertEquals(ChartProgress(ring = 0.5f, dot = 0f), chartProgress(987_500))
        assertEquals(ChartProgress(ring = 1f, dot = 0.9f), chartProgress(1_009_000))
    }
}
