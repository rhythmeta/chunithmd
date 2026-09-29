package org.rhythmeta.chunithmd.shared

/**
 * A chart result that can participate in the player's B30/N20 calculation.
 *
 * [chartId] must identify one concrete chart (song + difficulty + type). It is
 * intentionally separate from [songId], because different difficulties of the
 * same song may occupy different slots.
 */
data class RatingChartEntry(
    val chartId: String,
    val songId: String,
    val rating: Double,
    val isNew: Boolean,
)

data class PlayerRatingSummary(
    val best30: List<RatingChartEntry>,
    val new20: List<RatingChartEntry>,
    val bestSlotCount: Int = BEST_SLOT_COUNT.toInt(),
    val newSlotCount: Int = NEW_SLOT_COUNT.toInt(),
) {
    val best30Total: Double get() = best30.sumOf { it.rating }
    val new20Total: Double get() = new20.sumOf { it.rating }
    val total: Double get() = best30Total + new20Total

    /** Empty slots count as zero, so this remains a B50 rating while importing partial data. */
    val rating: Double get() = total / TOTAL_SLOT_COUNT
    val best30Average: Double get() = best30Total / BEST_SLOT_COUNT
    val new20Average: Double get() = new20Total / NEW_SLOT_COUNT

    companion object {
        const val BEST_SLOT_COUNT = 30.0
        const val NEW_SLOT_COUNT = 20.0
        const val TOTAL_SLOT_COUNT = 50.0
    }
}

/**
 * Calculates one chart's Rating from its internal constant and score.
 *
 * The score bands are the current post-VERSE formula. The returned value is
 * deliberately not rounded: values such as C - 3.333... are display-rounded
 * later, while the unrounded value is used by B30/N20 aggregation.
 */
fun calculateSingleRating(constant: Double, score: Long): Double {
    if (!constant.isFinite() || constant <= 0.0 || score < SCORE_CUTOFF) return 0.0

    val clampedScore = score.coerceAtMost(SCORE_SSS_PLUS)
    val value = when {
        clampedScore >= SCORE_SSS_PLUS -> constant + 2.15
        clampedScore >= 1_007_500L -> constant + 2.00 + (clampedScore - 1_007_500L) / 10_000.0
        clampedScore >= 1_005_000L -> constant + 1.50 + (clampedScore - 1_005_000L) / 5_000.0
        clampedScore >= 1_000_000L -> constant + 1.00 + (clampedScore - 1_000_000L) / 5_000.0
        clampedScore >= 975_000L -> constant + (clampedScore - 975_000L) / 25_000.0
        clampedScore >= 900_000L -> constant - 5.00 + (clampedScore - 900_000L) / 15_000.0
        clampedScore >= 800_000L -> (constant - 5.00) * (clampedScore - 700_000L) / 200_000.0
        else -> (constant - 5.00) * (clampedScore - 500_000L) / 600_000.0
    }
    return value.coerceAtLeast(0.0)
}

fun calculateSingleRating(constant: Double, score: Int): Double =
    calculateSingleRating(constant, score.toLong())

/**
 * Calculates B30/N20 from already calculated chart Ratings.
 *
 * Entries marked [RatingChartEntry.isNew] fill N20; the remaining entries fill
 * B30. N20 is reserved first so a malformed input containing both categories
 * for one chart still cannot double-count it.
 */
fun calculatePlayerRating(
    entries: Iterable<RatingChartEntry>,
    bestSlotCount: Int = PlayerRatingSummary.BEST_SLOT_COUNT.toInt(),
    newSlotCount: Int = PlayerRatingSummary.NEW_SLOT_COUNT.toInt(),
): PlayerRatingSummary {
    val candidates = entries
        .filter { it.chartId.isNotBlank() && it.rating.isFinite() && it.rating > 0.0 }
        .sortedWith(compareByDescending<RatingChartEntry> { it.rating }.thenBy { it.chartId })

    fun selectDistinct(source: Iterable<RatingChartEntry>, count: Int, excluded: Set<String>): List<RatingChartEntry> {
        if (count <= 0) return emptyList()
        val selected = ArrayList<RatingChartEntry>(count)
        val used = excluded.toMutableSet()
        for (entry in source) {
            if (entry.chartId in used) continue
            selected += entry
            used += entry.chartId
            if (selected.size == count) break
        }
        return selected
    }

    // Reserve N20 first. This prevents a new chart from being counted in both
    // groups while still allowing another difficulty of the same song to enter.
    val new20 = selectDistinct(candidates.filter { it.isNew }, newSlotCount, emptySet())
    val reservedChartIds = new20.mapTo(mutableSetOf(), RatingChartEntry::chartId)
    val best30 = selectDistinct(candidates.filter { !it.isNew }, bestSlotCount, reservedChartIds)
    return PlayerRatingSummary(
        best30 = best30,
        new20 = new20,
        bestSlotCount = bestSlotCount.coerceAtLeast(0),
        newSlotCount = newSlotCount.coerceAtLeast(0),
    )
}

private const val SCORE_CUTOFF = 500_000L
private const val SCORE_SSS_PLUS = 1_009_000L
