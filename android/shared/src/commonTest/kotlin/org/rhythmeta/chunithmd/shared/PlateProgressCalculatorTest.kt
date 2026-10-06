package org.rhythmeta.chunithmd.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class PlateProgressCalculatorTest {
    @Test
    fun difficultyAndRemainingFiltersPreserveTheirProgressDenominators() {
        val song = CatalogSong("song", "Song")
        fun chart(difficulty: String, achieved: Boolean) = PlateChartEntry(
            song, CatalogSheet("std", difficulty), "14", achieved, if (achieved) "SSS" else null,
        )
        val response = PlateProgressResponse(charts = listOf(
            chart("master", true), chart("master", false), chart("expert", false),
        ))
        val scoped = response.forDifficulty("MASTER")
        assertEquals(2, scoped.totalCount)
        assertEquals(1, scoped.completedCount)
        assertEquals(0.5f, scoped.progress)
        val section = scoped.sections().single()
        assertEquals(1, section.visibleCharts(remainingOnly = true).size)
        assertEquals(0.5f, section.progress)
        assertEquals(2, section.charts.size)
        assertEquals(3, response.totalCount)
        assertEquals(response, response.forDifficulty(null))
        assertEquals(0f, response.forDifficulty("basic").progress)
    }

    @Test
    fun legendUsesEarlierAjEvenWhenHighestScoreIsNotAj() {
        val song = CatalogSong("song", "Song", version = "CHUNITHM VERSE", sheets = listOf(
            CatalogSheet("std", "master", level = "14", regions = mapOf("jp" to true)),
        ))
        val bundle = CatalogBundle(1, Catalog(songs = listOf(song)))
        val records = listOf(
            ScoreRecord("aj", "p", "song", "song:std:master", 1_000_000, "SS", 1, fullCombo = "alljustice"),
            ScoreRecord("highest", "p", "song", "song:std:master", 1_008_000, "SSS", 2),
        )
        val result = PlateProgressCalculator.calculate(bundle, records, ProfileServer.Jp, plateType = PlateType.Legend)
        assertEquals("Legend of VERSE", result.title)
        assertEquals(1, result.completedCount)
        assertEquals("AJ", result.charts.single().achievementLabel)
    }
}
