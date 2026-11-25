package org.rhythmeta.chunithmd.shared

/** A score target that can improve the player's B30/N20 table. */
data class RecommendationResult(
    val song: CatalogSong,
    val sheet: CatalogSheet,
    val constant: Double,
    val currentScore: Int?,
    val currentRating: Double,
    val targetRank: String,
    val targetScore: Int,
    val potentialRating: Double,
    /** Increase to the player's calculated B/N average rating, not the chart Rating. */
    val potentialGain: Double,
    val isNew: Boolean,
) {
    val chartId: String get() = song.sheetKey(sheet)
}

data class RecommendationResponse(
    val new: List<RecommendationResult> = emptyList(),
    val old: List<RecommendationResult> = emptyList(),
)

/**
 * Finds the next rank target that can replace a B30/N20 entry.
 *
 * Each target must improve on both the chart's current Rating and the lowest
 * occupied slot it would replace. Empty slots contribute zero to the average.
 */
object RecommendationCalculator {
    private val targetMilestones = ChunithmScoreRules.rankThresholds
        .filter { it.score >= SCORE_S }

    fun calculate(
        bundle: CatalogBundle,
        records: Iterable<ScoreRecord>,
        activeServer: ProfileServer,
        bestSlotCount: Int = PlayerRatingSummary.BEST_SLOT_COUNT.toInt(),
        newSlotCount: Int = PlayerRatingSummary.NEW_SLOT_COUNT.toInt(),
        candidateLimit: Int = DEFAULT_CANDIDATE_LIMIT,
    ): RecommendationResponse {
        val normalizedBestCount = bestSlotCount.coerceAtLeast(0)
        val normalizedNewCount = newSlotCount.coerceAtLeast(0)
        val totalSlotCount = (normalizedBestCount + normalizedNewCount).coerceAtLeast(1)
        val latestVersion = bundle.latestPlayableVersion(activeServer)
        val bestScores = records
            .groupBy(ScoreRecord::sheetKey)
            .mapValues { (_, values) -> values.maxByOrNull(ScoreRecord::score)!! }
        val charts = bundle.catalog.songs.flatMap { song ->
            song.sheets.mapNotNull { sheet ->
                if (!isPlayableChart(song, sheet, activeServer)) return@mapNotNull null
                if (sheet.type.equals("we", ignoreCase = true)) return@mapNotNull null
                val constant = chartConstant(song, sheet, activeServer) ?: return@mapNotNull null
                val key = song.sheetKey(sheet)
                val isNew = latestVersion != null && song.version.equals(latestVersion, ignoreCase = true)
                Chart(song, sheet, key, constant, isNew)
            }
        }

        // Calculate each chart's current score/rating once. The recommendation
        // pass below needs the same values for slot selection and target gains.
        val scoredCharts = charts.map { chart ->
            val score = bestScores[chart.chartId]?.score
            ScoredChart(
                chart = chart,
                score = score,
                currentRating = score?.let { calculateSingleRating(chart.constant, it) } ?: 0.0,
            )
        }
        val summary = calculatePlayerRating(scoredCharts.map { (chart, _, rating) ->
            RatingChartEntry(chart.chartId, chart.song.songId, rating, chart.isNew)
        }, normalizedBestCount, normalizedNewCount)
        val total = summary.total
        val currentDisplay = summary.displayedRating
        val newThreshold = replacementThreshold(summary.new20, normalizedNewCount)
        val oldThreshold = replacementThreshold(summary.best30, normalizedBestCount)

        val results = scoredCharts.mapNotNull { scoredChart ->
            val chart = scoredChart.chart
            val score = scoredChart.score
            if (score != null && score >= SCORE_SSS_PLUS) return@mapNotNull null
            val currentRating = scoredChart.currentRating
            val threshold = if (chart.isNew) newThreshold else oldThreshold
            val baseline = maxOf(currentRating, threshold)
            val target = targetMilestones.firstOrNull {
                if (it.score <= (score ?: SCORE_BELOW_CUTOFF)) return@firstOrNull false
                val gain = calculateSingleRating(chart.constant, it.score) - baseline
                gain > 0 && displayedPlayerRating(total + gain, totalSlotCount) > currentDisplay
            } ?: return@mapNotNull null
            val potentialRating = calculateSingleRating(chart.constant, target.score)
            RecommendationResult(
                song = chart.song,
                sheet = chart.sheet,
                constant = chart.constant,
                currentScore = score,
                currentRating = currentRating,
                targetRank = target.rank,
                targetScore = target.score,
                potentialRating = potentialRating,
                potentialGain = (potentialRating - baseline) / totalSlotCount,
                isNew = chart.isNew,
            )
        }

        val limit = candidateLimit.coerceAtLeast(0)
        return RecommendationResponse(
            new = results.filter(RecommendationResult::isNew).sortedWith(recommendationComparator).take(limit),
            old = results.filterNot(RecommendationResult::isNew).sortedWith(recommendationComparator).take(limit),
        )
    }

    internal val recommendationComparator: Comparator<RecommendationResult> = compareBy<RecommendationResult> { it.potentialGain }
        .thenByDescending { it.potentialRating }
        .thenBy { it.chartId }

    private fun replacementThreshold(
        selected: List<RatingChartEntry>,
        capacity: Int,
    ): Double = when {
        capacity == 0 -> Double.POSITIVE_INFINITY
        selected.size < capacity -> 0.0
        else -> selected.last().rating
    }

    private fun isPlayableChart(song: CatalogSong, sheet: CatalogSheet, server: ProfileServer): Boolean =
        song.isSheetPlayableIn(sheet, server.wireValue)

    private fun chartConstant(song: CatalogSong, sheet: CatalogSheet, server: ProfileServer): Double? =
        if (server == ProfileServer.Cn) {
            song.regionOverrides["cn"]?.charts?.get("${sheet.type}:${sheet.difficulty}")?.levelValue
                ?: sheet.internalLevelValue
                ?: sheet.levelValue
        } else {
            sheet.internalLevelValue ?: sheet.levelValue
        }?.takeIf { it.isFinite() && it > 0.0 }

    private data class Chart(
        val song: CatalogSong,
        val sheet: CatalogSheet,
        val chartId: String,
        val constant: Double,
        val isNew: Boolean,
    )

    private data class ScoredChart(
        val chart: Chart,
        val score: Int?,
        val currentRating: Double,
    )

    private const val SCORE_BELOW_CUTOFF = 0
    private const val SCORE_S = 975_000
    private const val SCORE_SSS_PLUS = 1_009_000
    private const val DEFAULT_CANDIDATE_LIMIT = 100
}
