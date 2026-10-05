package org.rhythmeta.chunithmd.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CatalogRegionTest {
    private val sheet = CatalogSheet("std", "master", "13", 13.0, mapOf("jp" to true),
        noteDesigner = "Designer", noteCounts = CatalogNoteCounts(tap = 100), internalLevelValue = 13.4)
    private val song = CatalogSong("song", "Song", sheets = listOf(sheet), regionOverrides = mapOf(
        "cn" to RegionOverride(true, mapOf("std:master" to RegionChartOverride(true, "13+", 13.8))),
    ))

    @Test fun chineseDetailsUseRegionalLevelsAndMatchBestTableRating() {
        val resolved = song.sheetForServer(sheet, ProfileServer.Cn)
        assertEquals("13+", resolved.level)
        assertEquals(13.8, resolved.levelValue)
        assertEquals(13.8, resolved.internalLevelValue)
        assertEquals(song.sheetKey(sheet), song.sheetKey(resolved))
        assertEquals(sheet.noteCounts, resolved.noteCounts)
        assertEquals(sheet.noteDesigner, resolved.noteDesigner)
        assertEquals(sheet.regions, resolved.regions)
        val score = ScoreRecord("r", "p", song.songId, song.sheetKey(sheet), 1_009_000, "SSS+", 1)
        val best = buildBestTableEntries(CatalogBundle(1, Catalog(songs = listOf(song))), listOf(score), ProfileServer.Cn).single()
        assertEquals(best.constant, resolved.internalLevelValue)
        assertEquals(best.rating, calculateSingleRating(resolved.internalLevelValue!!, score.score))
    }

    @Test fun switchingBackToJapanOrInternationalUsesOriginalLevels() {
        song.sheetForServer(sheet, ProfileServer.Cn)
        assertSame(sheet, song.sheetForServer(sheet, ProfileServer.Jp))
        assertSame(sheet, song.sheetForServer(sheet, ProfileServer.Intl))
        assertEquals(13.4, sheet.internalLevelValue)
    }

    @Test fun missingChineseConstantsFallBackToPrimaryValues() {
        val unavailable = song.copy(regionOverrides = emptyMap())
        assertSame(sheet, unavailable.sheetForServer(sheet, ProfileServer.Cn))
        val labelOnly = song.copy(regionOverrides = mapOf(
            "cn" to RegionOverride(true, mapOf("std:master" to RegionChartOverride(true, "13+"))),
        ))
        val resolved = labelOnly.sheetForServer(sheet, ProfileServer.Cn)
        assertEquals("13+", resolved.level)
        assertEquals(13.4, resolved.internalLevelValue)
        assertEquals(13.0, resolved.levelValue)
        val noInternal = sheet.copy(internalLevelValue = null)
        val fallback = labelOnly.sheetForServer(noInternal, ProfileServer.Cn)
        assertEquals(13.0, fallback.internalLevelValue ?: fallback.levelValue)
    }
}
