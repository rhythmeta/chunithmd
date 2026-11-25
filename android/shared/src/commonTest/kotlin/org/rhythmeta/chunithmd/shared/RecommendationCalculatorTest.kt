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

        val oldResult = response.old.single { it.song.songId == "old" }
        assertEquals("old", oldResult.song.songId)
        assertEquals("S", oldResult.targetRank)
        assertEquals(0.24, oldResult.potentialGain, absoluteTolerance = 0.000001)
        assertTrue(oldResult.currentScore == null)
        val tiny = response.old.single { it.song.songId == "tiny" }
        assertEquals("S+", tiny.targetRank)
        assertEquals(0.014, tiny.potentialGain, absoluteTolerance = 0.000001)
    }

    @Test
    fun filledSlotOnlyRecommendsResultsAboveItsBottomRating() {
        val bundle = bundle(
            CatalogSong("a", "A", version = "new", sheets = listOf(sheet(12.0))),
            CatalogSong("b", "B", version = "new", sheets = listOf(sheet(12.0))),
            CatalogSong("low", "Low", version = "new", sheets = listOf(sheet(9.0))),
            CatalogSong("disabled", "Disabled", version = "old", sheets = listOf(sheet(15.0))),
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

        assertEquals(listOf("a", "b"), response.new.map { it.song.songId })
        assertTrue(response.new.all { it.targetRank == "S+" })
        assertTrue(response.old.isEmpty())
    }

    @Test
    fun sameSmallGainOnlyQualifiesWhenItChangesTheDisplayedAverage() {
        val bundle = bundle(
            CatalogSong("floor", "Floor", version = "old", sheets = listOf(sheet(13.0))),
            CatalogSong("target", "Target", version = "old", sheets = listOf(sheet(12.9))),
            CatalogSong("new", "New", version = "new", sheets = listOf(sheet(14.2))),
        )
        val old = ScoreRecord("1", "p", "floor", "floor:std:master", 1_007_500, "SSS", 0)
        val new = ScoreRecord("2", "p", "new", "new:std:master", 975_000, "S", 0)
        fun recommend(vararg records: ScoreRecord) = RecommendationCalculator.calculate(
            bundle, records.toList(), ProfileServer.Jp, bestSlotCount = 1, newSlotCount = 49)

        // 0.300 -> 0.301 still displays 0.30. With the new chart, 0.584 -> 0.585 displays 0.59.
        assertTrue(recommend(old).old.none { it.song.songId == "target" })
        val response = recommend(old, new)
        val target = response.old.single { it.song.songId == "target" }
        assertEquals("SSS+", target.targetRank)
        assertEquals(15.05, target.potentialRating)
        assertEquals(0.001, target.potentialGain, absoluteTolerance = 0.000001)
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
