package org.rhythmeta.chunithmd.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OverPowerCalculatorTest {
    private fun earned(constant: Double, score: Int, lamp: String? = null) =
        OverPowerCalculator.chartPoints(constant, BestScoreSummary(score, null, lamp, null)).earned

    @Test
    fun wikiExamplesAndFractionalScoreBands() {
        assertEquals(91.06, earned(15.4, 1_009_540, "alljustice"))
        assertEquals(92.0, earned(15.4, 1_010_000))
        assertEquals(87.0, earned(15.4, 1_007_500))
        assertEquals(89.25, earned(15.4, 1_009_000))
        assertEquals(78.985, earned(15.6, 979_949))
        assertEquals(81.53, earned(15.3, 1_000_063))
        assertEquals(83.535, earned(15.1, 1_005_535))
        assertEquals(0.0, earned(13.0, 499_999))
        assertEquals(20.0, earned(13.0, 800_000))
        assertEquals(40.0, earned(13.0, 900_000))
        assertEquals(3.0, earned(1.0, 945_000))
        assertEquals(77.5, earned(15.4, 975_000, "fullcombo"))
    }

    private fun sheet(difficulty: String, constant: Double, level: String) =
        CatalogSheet("std", difficulty, level, constant, regions = mapOf("jp" to true))

    private val expert = sheet("expert", 14.1, "14")
    private val master = sheet("master", 15.3, "15")
    private val lamia = CatalogSong("a", "LAMIA", sheets = listOf(expert, master), version = "old",
        regionOverrides = mapOf("cn" to RegionOverride(true, mapOf("std:master" to RegionChartOverride(true, "14+", 14.9)))))
    private val newer = CatalogSong("b", "New", sheets = listOf(expert, expert.copy(difficulty = "ultima")), version = "new")
    private val we = CatalogSong("w", "WE", sheets = listOf(master.copy(type = "we")))
    private val bundle = CatalogBundle(1, Catalog(versions = listOf(CatalogVersion("old"), CatalogVersion("new")),
        songs = listOf(lamia, newer, we)))

    private fun record(sheet: CatalogSheet, score: Int, lamp: String? = null, profile: String = "p") =
        ScoreRecord("id-$score", profile, lamia.songId, lamia.sheetKey(sheet), score, "", 0, fullCombo = lamp)

    private fun report(records: List<ScoreRecord> = emptyList(), server: ProfileServer = ProfileServer.Jp) =
        OverPowerCalculator.calculate(bundle, records, "p", server)

    @Test
    fun aggregatesSongsAndLevelsIndependentlyAndCombinesBestLampWithBestScore() {
        val result = report(listOf(record(master, 1_006_000), record(expert, 1_009_900),
            record(expert, 1_007_501, "alljustice"), record(master, 1_010_000, profile = "another")))
        val song = result.all.songs.single { it.songId == "a" }
        assertEquals("expert", song.contribution?.difficulty)
        assertEquals(85.10, song.points.earned)
        assertEquals(91.50, song.points.maximum)
        assertEquals(6.40, song.points.remaining)
        assertEquals(1, result.all.recordedCount)
        assertEquals(85.10, result.all.points.earned)
        assertEquals(177.0, result.all.points.maximum)
        assertEquals(3, result.all.levels.single { it.level == "14" }.charts.size)
        assertEquals(2, result.hot.levels.single().charts.size)
        assertEquals(85.50, result.hot.points.maximum)
        assertEquals(0, result.hot.recordedCount)
    }

    @Test
    fun emptyProfileThenImportAndSwitchRegion() {
        val empty = report()
        assertEquals(0.0, empty.all.points.percent)
        assertEquals(2, empty.all.songs.size)
        assertEquals(70, empty.all.points.nextPercent)
        assertNull(empty.all.songs.first().contribution)
        val history = listOf(record(master, 1_010_000))
        assertEquals(91.50, report(history).all.points.earned)
        val cn = report(history, ProfileServer.Cn)
        assertEquals("old", cn.hotVersion)
        assertEquals("14+", cn.all.levels.single().level)
        assertEquals(14.9, cn.all.songs.single().displayChart.constant)
        assertEquals(89.50, cn.hot.points.maximum)
        assertEquals(100.0, cn.all.points.percent)
        assertEquals(0.0, cn.all.points.pointsToNext)
        assertEquals(0, report(history, ProfileServer.Intl).all.songs.size)
        assertEquals(1, report(listOf(record(master, 0))).all.recordedCount)
    }

    @Test
    fun sortingUsesTheDisplayedScopeAndOrdersDifficultyByConstant() {
        val tracks = listOf(
            OverPowerChart("a", "a", "Alpha", "", "ultima", "14", 14.3, null, OverPowerPoints(180, 200)),
            OverPowerChart("b", "b", "Beta", "", "master", "15", 15.6, null, OverPowerPoints(270, 400)),
            OverPowerChart("c", "c", "gamma", "", "master", "14", 14.4, null, OverPowerPoints(180, 300)),
        ).map { it.copy(best = BestScoreSummary(975_000, null, null, null)) }
        val scope = OverPowerScope(tracks.map { OverPowerSong(listOf(it), null) })
        fun names(sort: OverPowerSort, ascending: Boolean = false) =
            OverPowerOrder(sort, ascending).songs(scope, recorded = true).map { it.songId }
        assertEquals(listOf("b", "c", "a"), names(OverPowerSort.Remaining))
        assertEquals(listOf("b", "c", "a"), names(OverPowerSort.Difficulty))
        assertEquals(listOf("a", "c", "b"), names(OverPowerSort.Difficulty, ascending = true))
        assertEquals(listOf("a", "b", "c"), names(OverPowerSort.Rate))
        assertEquals(listOf("c", "b", "a"), names(OverPowerSort.Rate, ascending = true))
        assertEquals(listOf("a", "b", "c"), names(OverPowerSort.Name, ascending = true))
        assertEquals(emptyList(), OverPowerOrder(OverPowerSort.Remaining, false).songs(scope, recorded = false))
        assertEquals(listOf("15", "14"), OverPowerOrder(OverPowerSort.Level, false).levels(scope).map { it.level })
        val group = scope.levels.single { it.level == "14" }
        assertEquals(listOf("c", "a"), OverPowerOrder(OverPowerSort.Difficulty, false).charts(group).map { it.sheetKey })
        assertEquals(listOf("a", "c"), OverPowerOrder(OverPowerSort.Difficulty, true).charts(group).map { it.sheetKey })
        assertEquals(listOf("c", "a"), OverPowerOrder(OverPowerSort.Name, false).charts(group).map { it.sheetKey })
    }

    @Test
    fun songDifficultyFollowsItsContributionOrHighestPlayableChart() {
        val scope = report(listOf(record(expert, 1_009_900))).all
        assertEquals("expert", scope.songs.single { it.songId == "a" }.displayChart.difficulty)
        assertEquals("ultima", scope.songs.single { it.songId == "b" }.displayChart.difficulty)
        assertEquals(listOf("a", "b"), OverPowerOrder(OverPowerSort.Difficulty, false)
            .songs(report().all, recorded = false).map { it.songId })
    }

    @Test
    fun colorBoundariesUseExactTotals() {
        val tiers = listOf(70 to OverPowerTier.Silver, 80 to OverPowerTier.Gold,
            90 to OverPowerTier.Platinum, 95 to OverPowerTier.Rainbow)
        for ((percent, tier) in tiers) {
            val threshold = percent * 200L
            val below = OverPowerPoints(threshold - 1, 20_000)
            assertEquals(percent, below.nextPercent)
            assertEquals(0.005, below.pointsToNext)
            assertEquals(percent - 0.01, below.percent)
            assertEquals(tier, OverPowerPoints(threshold, 20_000).tier)
        }
    }
}
