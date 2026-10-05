package org.rhythmeta.chunithmd.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BestTableCalculatorTest {
    private val historyBundle = CatalogBundle(1, Catalog(songs = listOf(
        CatalogSong("song", "Song", sheets = listOf(
            CatalogSheet("std", "master", internalLevelValue = 13.4, regions = mapOf("jp" to true)),
            CatalogSheet("std", "ultima", internalLevelValue = 14.0, regions = mapOf("jp" to true)),
        )),
    )))

    @Test
    fun bestTableCombinesIndependentHistoricalBestsForEachChart() {
        val records = listOf(
            ScoreRecord("score", "p", "song", "song:std:master", 1_009_000, "SSS+", 4),
            ScoreRecord("clear", "p", "song", "song:std:master", 990_000, "S", 3, clear = "hard"),
            ScoreRecord("combo", "p", "song", "song:std:master", 1_005_000, "SS+", 2, fullCombo = "alljustice"),
            ScoreRecord("chain", "p", "song", "song:std:master", 1_000_000, "SS", 1, fullChain = "fullchain2"),
            ScoreRecord("other", "p", "song", "song:std:ultima", 1_010_000, "SSS+", 5,
                clear = "catastrophy", fullCombo = "alljusticecritical", fullChain = "fullchain"),
        )
        val entries = buildBestTableEntries(historyBundle, records, ProfileServer.Jp)
        val master = entries.single { it.difficulty == "master" }
        assertEquals(1_009_000, master.score)
        assertEquals("SSS+", master.rank)
        assertEquals(calculateSingleRating(13.4, 1_009_000), master.rating)
        assertEquals("hard", master.clear)
        assertEquals("alljustice", master.fullCombo)
        assertEquals("fullchain2", master.fullChain)
        assertEquals(entries, buildBestTableEntries(historyBundle, records.reversed(), ProfileServer.Jp))
    }

    @Test
    fun bestTableRecomputesAfterHistoryDeletionAndPreservesUnknownStatuses() {
        val score = ScoreRecord("score", "p", "song", "song:std:master", 1_009_000, "SSS+", 2)
        val lamps = score.copy(id = "lamps", score = 1_000_000, playedAt = 1,
            clear = "hard", fullCombo = "fullcombo", fullChain = "fullchain")
        val before = buildBestTableEntries(historyBundle, listOf(score, lamps), ProfileServer.Jp).single()
        assertEquals("fullcombo", before.fullCombo)
        val after = buildBestTableEntries(historyBundle, listOf(score), ProfileServer.Jp).single()
        assertEquals(before.score, after.score)
        assertEquals(before.rating, after.rating)
        assertNull(after.clear)
        assertNull(after.fullCombo)
        assertNull(after.fullChain)
    }

    @Test
    fun buildsBestEntriesWithRegionConstantAndLatestVersion() {
        val bundle = CatalogJson.decodeBundle(
            """
            {
              "schemaVersion": 1,
              "catalog": {
                "versions": [{"version":"old"},{"version":"new"}],
                "songs": [
                  {"songId":"song-old","title":"Old","version":"old","sheets":[{"type":"std","difficulty":"master","levelValue":12.0,"internalLevelValue":12.3,"regions":{"jp":true}}]},
                  {"songId":"song-new","title":"New","version":"new","sheets":[{"type":"std","difficulty":"master","levelValue":13.0,"internalLevelValue":13.4,"regions":{"jp":true}}],"regionOverrides":{"cn":{"available":true,"charts":{"std:master":{"available":true,"levelValue":13.8}}}}}
                ]
              }
            }
            """.trimIndent(),
        )
        val entries = buildBestTableEntries(
            bundle,
            listOf(
                ScoreRecord("old", "p", "song-old", "song-old:std:master", 1_009_000, "SSS+", 1),
                ScoreRecord("new", "p", "song-new", "song-new:std:master", 1_009_000, "SSS+", 2),
            ),
            ProfileServer.Cn,
        )

        assertEquals(listOf("song-new:std:master"), entries.map(BestTableEntry::chartId))
        assertEquals(13.8, entries.single().constant)
        assertEquals(true, entries.single().isNew)
    }

    @Test
    fun noteBreakdownUsesChunithmNoteWeights() {
        val rows = CatalogNoteCounts(tap = 1, hold = 1, slide = 1, air = 1, flick = 1).breakdown()
        assertEquals(listOf("TAP", "HOLD", "SLIDE", "AIR", "FLICK"), rows.map(NoteCountBreakdown::label))
        assertEquals(1.0 / 8.0, rows.first().fraction, absoluteTolerance = 0.0000001)
        assertEquals(1.0 / 8.0, rows.last().fraction, absoluteTolerance = 0.0000001)
    }

    @Test
    fun ratingTableAndHistoryHelpersAreShared() {
        val rows = buildRatingTable(13.0)
        assertEquals("SSS+", rows.first().rank)
        assertEquals(1_009_000, rows.first().score)

        val records = listOf(
            ScoreRecord("a", "p", "s", "k", 900_000, "A", 1),
            ScoreRecord("b", "p", "s", "k", 950_000, "AAA", 2),
        )
        assertEquals("b", records.bestScore()?.id)
        assertEquals(listOf("b", "a"), records.sortForHistory(ScoreHistorySort.Score).map(ScoreRecord::id))
        assertEquals(listOf("b"), records.sortForHistory(ScoreHistorySort.Time).page(1, 1).map(ScoreRecord::id))
    }
}
