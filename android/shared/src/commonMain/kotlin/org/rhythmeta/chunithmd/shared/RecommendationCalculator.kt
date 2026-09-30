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
 * The calculation intentionally uses the same unrounded Rating values as the
 * Best table. The result gain is divided by the same total B/N slot count used
 * by the player's displayed rating, so it represents the actual player Rating
 * increase after the replacement rather than the chart's raw Rating increase.
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
        val selectedNew = scoredCharts
            .filter { it.chart.isNew && it.currentRating > 0.0 }
            .sortedByDescending(ScoredChart::currentRating)
            .take(normalizedNewCount)
        val selectedOld = scoredCharts
            .filter { !it.chart.isNew && it.currentRating > 0.0 }
            .sortedByDescending(ScoredChart::currentRating)
            .take(normalizedBestCount)
        val selectedNewIds = selectedNew.mapTo(mutableSetOf()) { it.chart.chartId }
        val selectedOldIds = selectedOld.mapTo(mutableSetOf()) { it.chart.chartId }
        val newThreshold = replacementThreshold(selectedNew, normalizedNewCount)
        val oldThreshold = replacementThreshold(selectedOld, normalizedBestCount)

        val results = scoredCharts.mapNotNull { scoredChart ->
            val chart = scoredChart.chart
            val score = scoredChart.score
            if (score != null && score >= SCORE_SSS_PLUS) return@mapNotNull null
            val currentRating = scoredChart.currentRating
            val target = targetMilestones.firstOrNull { it.score > (score ?: SCORE_BELOW_CUTOFF) }
                ?: return@mapNotNull null
            val potentialRating = calculateSingleRating(chart.constant, target.score)
            val selectedIds = if (chart.isNew) selectedNewIds else selectedOldIds
            val threshold = if (chart.isNew) newThreshold else oldThreshold
            val singleRatingGain = if (chart.chartId in selectedIds) {
                (potentialRating - currentRating).coerceAtLeast(0.0)
            } else {
                (potentialRating - threshold).takeIf { potentialRating > threshold } ?: 0.0
            }
            if (singleRatingGain <= 0.0) return@mapNotNull null
            val playerRatingGain = singleRatingGain / totalSlotCount
            if (playerRatingGain < MIN_DISPLAYED_GAIN) return@mapNotNull null
            RecommendationResult(
                song = chart.song,
                sheet = chart.sheet,
                constant = chart.constant,
                currentScore = score,
                currentRating = currentRating,
                targetRank = target.rank,
                targetScore = target.score,
                potentialRating = potentialRating,
                potentialGain = playerRatingGain,
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
        selected: List<ScoredChart>,
        capacity: Int,
    ): Double = if (capacity > 0 && selected.size >= capacity) selected.lastOrNull()?.currentRating ?: 0.0 else 0.0

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
    private const val SCORE_CUTOFF = 500_000
    private const val SCORE_S = 975_000
    private const val SCORE_SSS_PLUS = 1_009_000
    private const val MIN_DISPLAYED_GAIN = 0.005
    private const val DEFAULT_CANDIDATE_LIMIT = 100
}
