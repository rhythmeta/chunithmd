package org.rhythmeta.chunithmd.shared

/** Independent judgement limits; every other note must be JUSTICE CRITICAL. */
data class ScoreTolerance(
    val justice: Int,
    val attack: Int,
    val miss: Int,
)

object ScoreToleranceCalculator {
    /** Returns null when the chart has no usable note total or the target is invalid. */
    fun calculate(totalNotes: Int?, targetScore: Int): ScoreTolerance? {
        if (totalNotes == null || totalNotes <= 0 || !ChunithmScoreRules.isValid(targetScore)) return null

        // JC/J/ATTACK/MISS earn 101%/100%/50%/0% of each note's base score.
        // Keep the fraction exact: flooring each note's loss would overstate tolerance.
        val lossBudget = (ChunithmScoreRules.maximumScore - targetScore).toLong() * totalNotes
        fun limit(lossNumerator: Int): Int =
            (lossBudget / lossNumerator).coerceAtMost(totalNotes.toLong()).toInt()

        return ScoreTolerance(
            justice = limit(10_000),
            attack = limit(510_000),
            miss = limit(1_010_000),
        )
    }
}
