package org.rhythmeta.chunithmd.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RatingCalculatorTest {
    @Test
    fun followsPostVerseScoreBandsAndCapsAtSssPlus() {
        val constant = 13.0

        assertEquals(0.0, calculateSingleRating(constant, 499_999L), absoluteTolerance = 0.0000001)
        assertEquals(0.0, calculateSingleRating(constant, 500_000L), absoluteTolerance = 0.0000001)
        assertEquals(4.0, calculateSingleRating(constant, 800_000L), absoluteTolerance = 0.0000001)
        assertEquals(8.0, calculateSingleRating(constant, 900_000L), absoluteTolerance = 0.0000001)
        assertEquals(13.0, calculateSingleRating(constant, 975_000L), absoluteTolerance = 0.0000001)
        assertEquals(14.0, calculateSingleRating(constant, 1_000_000L), absoluteTolerance = 0.0000001)
        assertEquals(14.5, calculateSingleRating(constant, 1_005_000L), absoluteTolerance = 0.0000001)
        assertEquals(15.0, calculateSingleRating(constant, 1_007_500L), absoluteTolerance = 0.0000001)
        assertEquals(15.15, calculateSingleRating(constant, 1_009_000L), absoluteTolerance = 0.0000001)
        assertEquals(15.15, calculateSingleRating(constant, 1_010_000L), absoluteTolerance = 0.0000001)
    }

    @Test
    fun lowerScoreBandsMeetAtTheirThresholds() {
        val constant = 13.0

        assertEquals(11.5, calculateSingleRating(constant, 950_000L), absoluteTolerance = 0.0000001)
        assertEquals(10.0, calculateSingleRating(constant, 925_000L), absoluteTolerance = 0.0000001)
        assertEquals(8.0, calculateSingleRating(constant, 900_000L), absoluteTolerance = 0.0000001)
    }

    @Test
    fun screenshotScoreMatchesLxnsAndTruncatesInsteadOfRounding() {
        assertEquals(15.19, calculateSingleRating(13.9, 1_002_990L))
        assertEquals(15.19, calculateSingleRating(13.9, 1_002_999))
        assertEquals(15.20, calculateSingleRating(13.9, 1_003_000))
        assertEquals(15.39, calculateSingleRating(13.9, 1_004_999))
        assertEquals(15.40, calculateSingleRating(13.9, 1_005_000))
    }

    @Test
    fun ratingNeverDropsAtAScoreBandBoundary() {
        val boundaries = listOf(500_000, 800_000, 900_000, 925_000, 975_000,
            1_000_000, 1_005_000, 1_007_500, 1_009_000)
        for (constant in listOf(1.0, 5.0, 13.9, 15.4)) {
            for (score in boundaries) {
                val before = calculateSingleRating(constant, score - 1)
                val at = calculateSingleRating(constant, score)
                val after = calculateSingleRating(constant, score + 1)
                assertTrue(before <= at && at <= after, "constant=$constant, score=$score")
            }
        }
    }

    @Test
    fun exactHundredthsAreNotLostToFloatingPointNoise() {
        for (tenths in 10..154) {
            val constant = tenths / 10.0
            assertEquals((tenths * 10 + 215) / 100.0, calculateSingleRating(constant, 1_009_000))
            assertEquals((tenths * 10 + 101) / 100.0, calculateSingleRating(constant, 1_000_100))
        }
    }

    @Test
    fun playerRatingUsesB30AndN20WeightsAndKeepsChartIdsUnique() {
        val summary = calculatePlayerRating(
            listOf(
                RatingChartEntry("old-high", "song-a", 10.0, isNew = false),
                RatingChartEntry("old-low", "song-b", 8.0, isNew = false),
                RatingChartEntry("new-high", "song-c", 9.0, isNew = true),
                RatingChartEntry("new-duplicate", "song-c", 7.0, isNew = true),
                RatingChartEntry("old-high", "song-a", 6.0, isNew = true),
            ),
            bestSlotCount = 2,
            newSlotCount = 2,
        )

        assertEquals(listOf("old-high", "old-low"), summary.best30.map { it.chartId })
        assertEquals(listOf("new-high", "new-duplicate"), summary.new20.map { it.chartId })
        assertEquals(34.0 / 4.0, summary.rating, absoluteTolerance = 0.0000001)
        assertEquals(18.0 / 2.0, summary.best30Average, absoluteTolerance = 0.0000001)
        assertEquals(16.0 / 2.0, summary.new20Average, absoluteTolerance = 0.0000001)
    }
}
