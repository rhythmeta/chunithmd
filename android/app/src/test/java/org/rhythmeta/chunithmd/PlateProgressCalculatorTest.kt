package org.rhythmeta.chunithmd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.rhythmeta.chunithmd.shared.Catalog
import org.rhythmeta.chunithmd.shared.CatalogBundle
import org.rhythmeta.chunithmd.shared.CatalogSheet
import org.rhythmeta.chunithmd.shared.CatalogSong
import org.rhythmeta.chunithmd.shared.CatalogVersion
import org.rhythmeta.chunithmd.shared.PlateProgressCalculator
import org.rhythmeta.chunithmd.shared.PlateType
import org.rhythmeta.chunithmd.shared.ProfileServer
import org.rhythmeta.chunithmd.shared.RegionChartOverride
import org.rhythmeta.chunithmd.shared.RegionOverride
import org.rhythmeta.chunithmd.shared.ScoreRecord
import org.rhythmeta.chunithmd.shared.sheetKey

class PlateProgressCalculatorTest {
    @Test
    fun titleNamesHandleOriginAndKeepPlusVersionsSeparate() {
        val names = mapOf(
            "CHUNITHM" to "ORIGIN",
            "CHUNITHM PLUS" to "ORIGIN PLUS",
            "CHUNITHM（初代） PLUS" to "ORIGIN PLUS",
            "CHUNITHM+" to "ORIGIN PLUS",
            "CHUNITHM AIR" to "AIR",
            "CHUNITHM AIR PLUS" to "AIR PLUS",
            "NEW+" to "NEW PLUS",
            "CHUNITHM PARADISE LOST" to "PARADISE LOST",
            "CHUNITHM X-VERSE" to "X-VERSE",
        )
        names.forEach { (version, expected) -> assertEquals(expected, PlateProgressCalculator.versionName(version)) }
        val bundle = bundle(song("origin"), song("plus", version = "CHUNITHM PLUS"), song("air", version = "CHUNITHM AIR"))
        val response = PlateProgressCalculator.calculate(bundle, emptyList(), ProfileServer.Jp, "CHUNITHM PLUS", PlateType.Tribute)
        assertEquals(listOf("ORIGIN", "ORIGIN PLUS", "AIR"), response.groups.map { it.name })
        assertEquals("Tribute of ORIGIN PLUS", response.title)
        assertEquals(listOf("plus"), response.charts.map { it.song.songId })
    }

    @Test
    fun scoreThresholdsIncludeHigherRanksAndRequireAjForLegend() {
        val song = song("thresholds")
        assertFalse(PlateType.Spirit.isAchieved(record(song, 974_999)))
        assertTrue(PlateType.Spirit.isAchieved(record(song, 975_000)))
        assertFalse(PlateType.Tribute.isAchieved(record(song, 1_007_499)))
        assertTrue(PlateType.Tribute.isAchieved(record(song, 1_007_500)))
        assertTrue(PlateType.Tribute.isAchieved(record(song, 1_009_000)))
        assertFalse(PlateType.Legend.isAchieved(record(song, 1_009_999, "fullcombo")))
        listOf("alljustice", "alljusticecritical", "AJ", "AJC").forEach {
            assertTrue(PlateType.Legend.isAchieved(record(song, 1_007_000, it)))
        }
    }

    @Test
    fun ignoresUltimaWorldsEndAndUnavailableCharts() {
        val song = song("types", sheets = listOf(
            sheet("basic"), sheet("advanced"), sheet("expert"), sheet("master"), sheet("ULTIMA"),
            sheet("master", type = "WE"), sheet("master", type = "utage"),
        ))
        val removed = song("removed", sheets = listOf(sheet("master").copy(regions = mapOf("jp" to false))))
        val response = PlateProgressCalculator.calculate(bundle(song, removed), emptyList(), ProfileServer.Jp)
        assertEquals(4, response.totalCount)
        assertEquals(PlateProgressCalculator.difficulties, response.charts.map { it.sheet.difficulty })
        assertEquals(4, response.remainingCount)
        assertFalse(response.achieved)
    }

    @Test
    fun anyHistoricalAjCountsEvenWhenHigherScoreHasNoAj() {
        val song = song("history")
        val records = listOf(
            record(song, 1_008_000, "alljustice"),
            record(song, 1_009_000, "fullcombo").copy(id = "newer", playedAt = 2),
        )
        val response = PlateProgressCalculator.calculate(bundle(song), records, ProfileServer.Jp, plateType = PlateType.Legend)
        assertTrue(response.achieved)
        assertEquals(1, response.completedCount)
        assertEquals("AJ", response.charts.single().achievementLabel)
    }

    @Test
    fun ajcSatisfiesLegendAndShowsAjcMarker() {
        val song = song("ajc")
        val response = PlateProgressCalculator.calculate(
            bundle(song), listOf(record(song, 1_010_000, "alljusticecritical")), ProfileServer.Jp, plateType = PlateType.Legend,
        )
        assertTrue(response.achieved)
        assertEquals("AJC", response.charts.single().achievementLabel)
    }

    @Test
    fun filtersDoNotTurnPartialDifficultyCompletionIntoCompletedPlate() {
        val song = song("all", sheets = listOf(sheet("basic", "3"), sheet("master", "13+")))
        val response = PlateProgressCalculator.calculate(bundle(song), listOf(record(song, 975_000)), ProfileServer.Jp)
        assertEquals(2, response.totalCount)
        assertEquals(1, response.completedCount)
        assertEquals(0.5f, response.progress)
        assertFalse(response.achieved)
        assertEquals(listOf("13+", "3"), response.sections().map { it.level })
        assertTrue(response.sections("basic").single().charts.single().achieved)
        assertEquals("master", response.sections(remainingOnly = true).single().charts.single().sheet.difficulty)
        assertTrue(response.sections("basic", remainingOnly = true).isEmpty())
        assertEquals(2, response.totalCount)
    }

    @Test
    fun serverAvailabilityAndCnLevelOverridesApply() {
        val song = song("regions", sheets = listOf(sheet("basic", "3"), sheet("master", "13"))).copy(
            regionOverrides = mapOf("cn" to RegionOverride(true, mapOf(
                "std:basic" to RegionChartOverride(false),
                "std:master" to RegionChartOverride(true, level = "14+"),
            ))),
        )
        val cn = PlateProgressCalculator.calculate(bundle(song), emptyList(), ProfileServer.Cn)
        assertEquals(1, cn.totalCount)
        assertEquals("14+", cn.charts.single().level)
        val jp = PlateProgressCalculator.calculate(bundle(song), emptyList(), ProfileServer.Jp)
        assertEquals(2, jp.totalCount)
        val intl = PlateProgressCalculator.calculate(bundle(song), emptyList(), ProfileServer.Intl)
        assertEquals(0, intl.totalCount)
        assertTrue(intl.groups.isEmpty())
    }

    @Test
    fun emptyCatalogDoesNotReportAchievementAndInvalidVersionFallsBack() {
        val empty = PlateProgressCalculator.calculate(bundle(), emptyList(), ProfileServer.Jp)
        assertEquals(0f, empty.progress)
        assertFalse(empty.achieved)
        val fallback = PlateProgressCalculator.calculate(bundle(song("fallback")), emptyList(), ProfileServer.Jp, "missing")
        assertEquals("ORIGIN", fallback.selectedGroup?.name)
    }

    @Test
    fun bestHistoricalRankIsUsedAndMissingScoresRemainIncomplete() {
        val played = song("played")
        val unplayed = song("unplayed")
        val response = PlateProgressCalculator.calculate(
            bundle(played, unplayed), listOf(record(played, 1_009_000), record(played, 950_000).copy(id = "later", playedAt = 2)),
            ProfileServer.Jp, plateType = PlateType.Tribute,
        )
        assertEquals(1, response.completedCount)
        assertEquals("SSS+", response.charts.first { it.song.songId == "played" }.achievementLabel)
        assertFalse(response.charts.first { it.song.songId == "unplayed" }.achieved)
    }

    private fun sheet(difficulty: String, level: String = "13", type: String = "std") =
        CatalogSheet(type, difficulty, level, regions = mapOf("jp" to true))

    private fun song(id: String, version: String = "CHUNITHM", sheets: List<CatalogSheet> = listOf(sheet("master"))) =
        CatalogSong(songId = id, title = id, version = version, sheets = sheets)

    private fun bundle(vararg songs: CatalogSong) = CatalogBundle(1, Catalog(
        versions = listOf("CHUNITHM", "CHUNITHM PLUS", "CHUNITHM AIR").map { CatalogVersion(it) }, songs = songs.toList(),
    ))

    private fun record(song: CatalogSong, score: Int, combo: String? = null) = ScoreRecord(
        id = "record", profileId = "profile", songId = song.songId, sheetKey = song.sheetKey(song.sheets.first()),
        score = score, rank = "", playedAt = 1, fullCombo = combo,
    )
}
