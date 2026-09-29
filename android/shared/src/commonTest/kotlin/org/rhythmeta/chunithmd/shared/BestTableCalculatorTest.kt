package org.rhythmeta.chunithmd.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class BestTableCalculatorTest {
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
