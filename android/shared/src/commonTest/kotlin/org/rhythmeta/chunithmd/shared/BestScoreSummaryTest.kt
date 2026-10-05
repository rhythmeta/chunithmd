package org.rhythmeta.chunithmd.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BestScoreSummaryTest {
    @Test
    fun combinesHighestScoreWithComboFromAnotherPlay() {
        val highScore = record("high", 1_004_000)
        val fullCombo = record("fc", 1_002_000, combo = "fullcombo")
        val records = listOf(highScore, fullCombo)

        val summary = records.bestScoreSummary()!!
        assertEquals(1_004_000, summary.score)
        assertEquals("SS", summary.rank)
        assertEquals("fullcombo", summary.fullCombo)
        // A summary must not replace or mutate the actual highest-scoring history record.
        assertEquals(highScore, records.bestScore())
        assertNull(highScore.fullCombo)
    }

    @Test
    fun takesEachStatusIndependentlyAndNormalizesImportedAliases() {
        val records = listOf(
            record("score", 1_009_500),
            record("clear", 1_000_000, clear = "CATASTROPHY"),
            record("combo", 1_007_000, combo = " AJ "),
            record("chain", 1_005_000, chain = "fs"),
            record("weaker", 1_006_000, clear = "hard", combo = "fc", chain = "fdx"),
        )
        val expected = BestScoreSummary(1_009_500, "catastrophy", "alljustice", "fullchain")
        assertEquals(expected, records.bestScoreSummary())
        assertEquals(expected, records.reversed().bestScoreSummary())
    }

    @Test
    fun strongestStatusesWinEvenWhenOlder() {
        val records = listOf(
            record("ajc", 1_010_000, combo = "alljusticecritical", chain = "fullchain"),
            record("new", 1_010_000, combo = "fullcombo", chain = "fullchain2").copy(playedAt = 2),
        )
        assertEquals("alljusticecritical", records.bestScoreSummary()?.fullCombo)
        assertEquals("fullchain", records.bestScoreSummary()?.fullChain)
        assertEquals("new", records.bestScore()?.id)
    }

    @Test
    fun deletingStatusSourceRemovesItFromSummary() {
        val records = listOf(record("high", 1_004_000), record("fc", 1_002_000, combo = "fullcombo"))
        assertEquals("fullcombo", records.bestScoreSummary()?.fullCombo)
        val afterDeletion = records.filterNot { it.id == "fc" }.bestScoreSummary()!!
        assertEquals(1_004_000, afterDeletion.score)
        assertNull(afterDeletion.fullCombo)
        assertNull(afterDeletion.fullChain)
        assertEquals("clear", afterDeletion.clear)
    }

    @Test
    fun emptyHistoryAndFailedPlayDoNotInventAchievements() {
        assertNull(emptyList<ScoreRecord>().bestScoreSummary())
        assertEquals(
            BestScoreSummary(500_000, "failed", null, null),
            listOf(record("failed", 500_000, clear = "failed")).bestScoreSummary(),
        )
    }

    @Test
    fun missingClearStaysNullAndCannotReplaceAKnownLamp() {
        val unknown = record("imported", 1_009_500, clear = null)
        assertNull(listOf(unknown).bestScoreSummary()?.clear)
        val known = record("manual", 1_000_000, clear = "hard")
        assertEquals("hard", listOf(known, unknown).bestScoreSummary()?.clear)
        assertEquals("hard", listOf(unknown, known).bestScoreSummary()?.clear)
    }

    private fun record(
        id: String,
        score: Int,
        clear: String? = "clear",
        combo: String? = null,
        chain: String? = null,
    ) = ScoreRecord(
        id = id,
        profileId = "profile",
        songId = "song",
        sheetKey = "song:std:master",
        score = score,
        rank = ChunithmScoreRules.rank(score),
        playedAt = 1,
        clear = clear,
        fullCombo = combo,
        fullChain = chain,
    )
}
