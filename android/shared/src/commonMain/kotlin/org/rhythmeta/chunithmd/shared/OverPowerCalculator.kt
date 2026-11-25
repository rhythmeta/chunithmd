package org.rhythmeta.chunithmd.shared

import kotlin.math.roundToLong

// Integer units preserve half-hundredths through chart, song, and catalog aggregation.
private const val UnitsPerPoint = 200
private val regularTypes = setOf("std", "standard")
private val regularDifficulties = setOf("basic", "advanced", "expert", "master", "ultima")

class OverPowerPoints internal constructor(internal val earnedUnits: Long, internal val maximumUnits: Long) {
    val earned: Double get() = earnedUnits.toDouble() / UnitsPerPoint
    val maximum: Double get() = maximumUnits.toDouble() / UnitsPerPoint
    val remaining: Double get() = (maximumUnits - earnedUnits).toDouble() / UnitsPerPoint
    val percent: Double get() = if (maximumUnits == 0L) 0.0 else (earnedUnits * 10_000 / maximumUnits) / 100.0

    val tier: OverPowerTier get() = OverPowerTier.entries.last { percent >= it.percent }
    val nextTier: OverPowerTier? get() = OverPowerTier.entries.getOrNull(tier.ordinal + 1)
    val nextPercent: Int get() = nextTier?.percent ?: 100
    val pointsToNext: Double get() = ((maximumUnits * nextPercent + 99) / 100 - earnedUnits).toDouble() / UnitsPerPoint
}

enum class OverPowerTier(val percent: Int) { White(0), Silver(70), Gold(80), Platinum(90), Rainbow(95) }

data class OverPowerChart(
    val sheetKey: String,
    val songId: String,
    val title: String,
    val imageName: String,
    val difficulty: String,
    val level: String,
    val constant: Double,
    val best: BestScoreSummary?,
    val points: OverPowerPoints,
) {
    val recorded: Boolean get() = best != null
}

class OverPowerSong internal constructor(val charts: List<OverPowerChart>, val version: String?) {
    val songId = charts.first().songId
    val title = charts.first().title
    val contribution = charts.filter { it.recorded }.maxByOrNull { it.points.earnedUnits }
    val displayChart = contribution ?: charts.maxWith(overPowerDifficultyOrder)
    val points = OverPowerPoints(contribution?.points?.earnedUnits ?: 0L, charts.maxOf { it.points.maximumUnits })
}

private fun Iterable<OverPowerPoints>.total() = OverPowerPoints(sumOf { it.earnedUnits }, sumOf { it.maximumUnits })

class OverPowerLevel internal constructor(val level: String, val charts: List<OverPowerChart>) {
    val points = charts.map { it.points }.total()
    val recordedCount = charts.count { it.recorded }
}

class OverPowerScope internal constructor(val songs: List<OverPowerSong>) {
    val points = songs.map { it.points }.total()
    val recordedCount = songs.count { it.contribution != null }
    val levels = songs.flatMap { it.charts }.groupBy { it.level }.map { (level, charts) ->
        OverPowerLevel(level, charts.sortedWith(compareBy({ it.title }, { it.sheetKey })))
    }.sortedByDescending { overPowerLevelOrder(it.level) }
}

class OverPowerReport(val all: OverPowerScope, val hot: OverPowerScope, val hotVersion: String?) {
    private val bySongId by lazy { all.songs.associateBy { it.songId } }
    fun song(songId: String): OverPowerSong? = bySongId[songId]
}

object OverPowerCalculator {
    fun calculate(bundle: CatalogBundle, records: List<ScoreRecord>, profileId: String, server: ProfileServer): OverPowerReport {
        val bestByChart = records.asSequence().filter { it.profileId == profileId && ChunithmScoreRules.isValid(it.score) }
            .groupBy(ScoreRecord::sheetKey).mapValues { (_, history) -> history.bestScoreSummary() }
        val hotVersion = bundle.latestPlayableVersion(server)
        val songs = bundle.catalog.songs.mapNotNull { song ->
            val title = CatalogSongFormatter.displayTitle(song)
            val charts = song.sheets.mapNotNull chart@{ sheet ->
                if (sheet.normalizedType() !in regularTypes || sheet.normalizedDifficulty() !in regularDifficulties ||
                    !song.isSheetPlayableIn(sheet, server.wireValue)) return@chart null
                val regional = song.sheetForServer(sheet, server)
                val constant = (regional.internalLevelValue ?: regional.levelValue)
                    ?.takeIf { it.isFinite() && it > 0 } ?: return@chart null
                val key = song.sheetKey(sheet)
                val best = bestByChart[key]
                OverPowerChart(
                    sheetKey = key,
                    songId = song.songId,
                    title = title,
                    imageName = song.imageName,
                    difficulty = sheet.difficulty,
                    level = regional.level.ifBlank { "?" },
                    constant = constant,
                    best = best,
                    points = chartPoints(constant, best),
                )
            }.distinctBy { it.sheetKey }
            if (charts.isEmpty()) null else OverPowerSong(charts, song.version)
        }.sortedWith(compareByDescending<OverPowerSong> { it.points.remaining }.thenBy { it.title }.thenBy { it.songId })
        val hot = songs.filter { hotVersion != null && hotVersion.equals(it.version, ignoreCase = true) }
        return OverPowerReport(OverPowerScope(songs), OverPowerScope(hot), hotVersion)
    }

    // S and above advance in steps of 0.005 OP. Keep the score precision that displayed Rating discards.
    internal fun chartPoints(constant: Double, best: BestScoreSummary?): OverPowerPoints {
        val base = (constant * 10).roundToLong() * 100
        val maximum = base + 3_000
        if (best == null) return OverPowerPoints(0L, maximum)
        val score = best.score
        if (score == ChunithmScoreRules.maximumScore) return OverPowerPoints(maximum, maximum)
        val scoreUnits = when {
            score >= 1_007_500 -> base + 2_000 + (score - 1_007_500) * 3 / 10
            score >= 1_005_000 -> base + 1_500 + (score - 1_005_000) / 5
            score >= 1_000_000 -> base + 1_000 + (score - 1_000_000) / 10
            score >= 975_000 -> base + (score - 975_000) / 25
            else -> belowS(base, score)
        }
        val lampUnits = when (displayFullCombo(best.fullCombo)) {
            "AJC" -> 250
            "AJ" -> 200
            "FC" -> 100
            else -> 0
        }
        return OverPowerPoints(scoreUnits + lampUnits, maximum)
    }

    // Below S, interpolate from the effective A border and truncate to 0.05 OP.
    private fun belowS(base: Long, score: Int): Long {
        val atA = (base - 5_000).coerceAtLeast(0)
        val units = when {
            score >= 900_000 -> atA + (base - atA) * (score - 900_000) / 75_000
            score >= 800_000 -> atA * (score - 700_000) / 200_000
            score >= 500_000 -> atA * (score - 500_000) / 600_000
            else -> 0L
        }
        return units / 10 * 10
    }
}
