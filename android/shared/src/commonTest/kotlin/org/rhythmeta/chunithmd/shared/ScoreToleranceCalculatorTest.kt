package org.rhythmeta.chunithmd.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScoreToleranceCalculatorTest {
    @Test
    fun sssPlusLimitsUseChunithmJudgements() {
        assertEquals(ScoreTolerance(100, 1, 0), ScoreToleranceCalculator.calculate(1_000, 1_009_000))
        assertEquals(ScoreTolerance(200, 3, 1), ScoreToleranceCalculator.calculate(2_000, 1_009_000))
    }

    @Test
    fun exactBorderIsIncludedButFractionalLossCannotBeTruncated() {
        assertEquals(ScoreTolerance(101, 1, 1), ScoreToleranceCalculator.calculate(1_010, 1_009_000))
        assertEquals(ScoreTolerance(100, 1, 0), ScoreToleranceCalculator.calculate(1_009, 1_009_000))
    }

    @Test
    fun perfectScoreAndLowTargetsRespectChartSize() {
        assertEquals(ScoreTolerance(0, 0, 0), ScoreToleranceCalculator.calculate(2_000, 1_010_000))
        assertEquals(ScoreTolerance(2_000, 2_000, 2_000), ScoreToleranceCalculator.calculate(2_000, 0))
        assertEquals(2_000, ScoreToleranceCalculator.calculate(2_000, 1_000_000)?.justice)
    }

    @Test
    fun missingOrInvalidInputsHaveNoResult() {
        listOf(null, 0, -1).forEach { assertNull(ScoreToleranceCalculator.calculate(it, 1_009_000)) }
        listOf(-1, 1_010_001, Int.MAX_VALUE).forEach { assertNull(ScoreToleranceCalculator.calculate(2_000, it)) }
    }

    @Test
    fun everyLimitReachesTargetAndOneMoreFallsBelowIt() {
        for (total in listOf(1, 3, 999, 1_010, 2_731, Int.MAX_VALUE)) {
            for (target in ChunithmScoreRules.rankThresholds.map { it.score } + ChunithmScoreRules.maximumScore) {
                val result = assertNotNull(ScoreToleranceCalculator.calculate(total, target))
                // Independently reconstruct the displayed score from earned judgement points.
                for ((count, earned) in listOf(result.justice to 1_000_000, result.attack to 500_000, result.miss to 0)) {
                    fun score(judgements: Int): Long =
                        ((total - judgements).toLong() * 1_010_000 + judgements.toLong() * earned) / total
                    assertTrue(count in 0..total)
                    assertTrue(score(count) >= target, "total=$total target=$target earned=$earned count=$count")
                    if (count < total) assertTrue(score(count + 1) < target)
                }
            }
        }
    }
}
