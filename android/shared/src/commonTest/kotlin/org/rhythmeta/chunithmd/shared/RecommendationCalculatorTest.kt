package org.rhythmeta.chunithmd.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RecommendationCalculatorTest {
    @Test
    fun recommendsNextRankAndSeparatesNewAndOldCharts() {
        val bundle = bundle(
            CatalogSong("old", "Old", version = "old", sheets = listOf(sheet(12.0))),
            CatalogSong("tiny", "Tiny", version = "old", sheets = listOf(sheet(0.1))),
            CatalogSong("new", "New", version = "new", sheets = listOf(sheet(13.0))),
        )
        val response = RecommendationCalculator.calculate(
            bundle = bundle,
            records = listOf(
                ScoreRecord("1", "p", "new", "new:std:master", 1_000_000, "SS", 0),
            ),
            activeServer = ProfileServer.Jp,
            bestSlotCount = 30,
            newSlotCount = 20,
        )

        val newResult = assertNotNull(response.new.single())
        assertEquals("new", newResult.song.songId)
        assertEquals("SS+", newResult.targetRank)
        assertEquals(1_005_000, newResult.targetScore)
        assertEquals(0.01, newResult.potentialGain, absoluteTolerance = 0.000001)

        val oldResult = assertNotNull(response.old.single())
        assertEquals("old", oldResult.song.songId)
        assertEquals("S", oldResult.targetRank)
        assertEquals(0.24, oldResult.potentialGain, absoluteTolerance = 0.000001)
        assertTrue(oldResult.currentScore == null)
    }

    @Test
    fun filledSlotOnlyRecommendsResultsAboveItsBottomRating() {
        val bundle = bundle(
            CatalogSong("a", "A", version = "new", sheets = listOf(sheet(12.0))),
            CatalogSong("b", "B", version = "new", sheets = listOf(sheet(12.0))),
        )
        val response = RecommendationCalculator.calculate(
            bundle = bundle,
            records = listOf(
                ScoreRecord("1", "p", "a", "a:std:master", 975_000, "S", 0),
            ),
            activeServer = ProfileServer.Jp,
            bestSlotCount = 0,
            newSlotCount = 1,
        )

        assertEquals(listOf("a"), response.new.map { it.song.songId })
        assertTrue(response.old.isEmpty())
    }

    private fun bundle(vararg songs: CatalogSong): CatalogBundle = CatalogBundle(
        schemaVersion = 1,
        catalog = Catalog(
            versions = listOf(CatalogVersion("old"), CatalogVersion("new")),
            songs = songs.toList(),
        ),
    )

    private fun sheet(constant: Double): CatalogSheet = CatalogSheet(
        type = "std",
        difficulty = "master",
        levelValue = constant,
        regions = mapOf("jp" to true),
    )
}
