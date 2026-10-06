package org.rhythmeta.chunithmd.shared

import kotlin.test.*

class BestTableResponseTest {
    private val bundle = CatalogJson.decodeBundle("""
        {"schemaVersion":1,"catalog":{"versions":[{"version":"old"},{"version":"new"}],"songs":[
          {"songId":"old","title":"Old","version":"old","sheets":[
            {"type":"std","difficulty":"master","internalLevelValue":13.0,"regions":{"jp":true}},
            {"type":"std","difficulty":"expert","internalLevelValue":12.0,"regions":{"jp":true}}]},
          {"songId":"new","title":"New","version":"new","sheets":[
            {"type":"std","difficulty":"master","internalLevelValue":14.0,"regions":{"jp":true}}]}
        ]}}
    """.trimIndent())
    private val records = listOf(
        ScoreRecord("a", "p", "old", "old:std:master", 1_009_000, "SSS+", 1),
        ScoreRecord("b", "p", "old", "old:std:expert", 1_009_000, "SSS+", 1),
        ScoreRecord("c", "p", "new", "new:std:master", 1_009_000, "SSS+", 1),
    )

    @Test fun capacitySelectsSharedRatingEntriesAndCountsEmptySlots() {
        val result = buildBestTableResponse(bundle, records, ProfileServer.Jp, BestTablePreferences(1, 2))
        assertEquals(listOf("old:std:master"), result.bestEntries.map { it.chartId })
        assertEquals(listOf("new:std:master"), result.newEntries.map { it.chartId })
        assertEquals(3, result.totalCapacity)
        assertEquals((15.15 + 16.15) / 3, result.summary.rating, 0.00001)
        assertEquals(16.15 / 2, result.summary.new20Average, 0.00001)
    }

    @Test fun versionOverrideExcludesFutureSongsAndReclassifiesOldSongs() {
        val result = buildBestTableResponse(bundle, records, ProfileServer.Jp, BestTablePreferences(selectedVersion = "old"))
        assertEquals("new", result.serverVersion)
        assertEquals("old", result.effectiveVersion)
        assertTrue(result.bestEntries.isEmpty())
        assertEquals(listOf("old:std:master", "old:std:expert"), result.newEntries.map { it.chartId })
        val automatic = buildBestTableResponse(bundle, records, ProfileServer.Jp, BestTablePreferences(-1, 300, "missing"))
        assertNull(automatic.preferences.selectedVersion)
        assertEquals("new", automatic.effectiveVersion)
        assertEquals(1, automatic.preferences.bestCount)
        assertEquals(99, automatic.preferences.newCount)
    }
}
