package org.rhythmeta.chunithmd.shared.importing

import kotlin.test.*
import kotlinx.serialization.json.*
import org.rhythmeta.chunithmd.shared.*

class DivingFishImportPolicyTest {
    private val song = CatalogSong("local-id", "Song", sheets = listOf(CatalogSheet("std", "master"), CatalogSheet("std", "ultima")))
    private val we = CatalogSong("we-id", "Song", sheets = listOf(CatalogSheet("we", "【止】")))
    private val catalog = CatalogBundle(1, Catalog(songs = listOf(song, we)))
    private fun payload(rows: String) = DivingFishImportPolicy.decode(Json.parseToJsonElement("""{"records":{"best":[$rows],"r10":[]}}""").jsonObject)
    private fun row(score: Int = 1_000_000, difficulty: Int = 3, fc: String = "") =
        """{"mid":999,"cid":"123","title":"Song","level_index":$difficulty,"score":$score,"fc":"$fc"}"""

    @Test fun readsNestedBestAndMatchesLocalTitlesInsteadOfForeignIds() {
        val input = payload(listOf(row(), row(difficulty = 4), row(difficulty = 5, fc = "alljustice")).joinToString())
        val plan = DivingFishImportPolicy.plan(input, catalog, "p", emptyList(), 10)
        assertEquals(3, plan.records.size)
        assertEquals(listOf("local-id:std:master", "local-id:std:ultima", "we-id:we:【止】"), plan.records.map { it.sheetKey })
        assertEquals("alljustice", plan.records.last().fullCombo)
        assertTrue(plan.records.all { it.clear == null && it.fullChain == null && it.playedAt == 10L })
    }

    @Test fun skipsMalformedUnknownAndAmbiguousChartsButCountsThem() {
        val input = payload(listOf(row(-1), row(1_010_001), row(difficulty = 6), row(fc = "unknown"), "{}", row().replace("Song", "Missing"), row()).joinToString())
        val ambiguous = catalog.copy(catalog = Catalog(songs = listOf(song, song.copy(songId = "duplicate"))))
        val plan = DivingFishImportPolicy.plan(input, ambiguous, "p", emptyList(), 10)
        assertEquals(ScoreImportResult(7, 0, 7), plan.result)
        assertFailsWith<DivingFishException> { payload("").let { DivingFishImportPolicy.plan(it, CatalogBundle(1, Catalog(songs = emptyList())), "p", emptyList(), 10) } }
        assertFailsWith<DivingFishException> { DivingFishImportPolicy.decode(Json.parseToJsonElement("""{"records":[]}""").jsonObject) }
    }

    @Test fun repeatedImportsAreIdempotentAndNeverLowerPersonalBests() {
        val input = payload(row(fc = "fullcombo"))
        val first = DivingFishImportPolicy.plan(input, catalog, "p", emptyList(), 10)
        val second = DivingFishImportPolicy.plan(input, catalog, "p", first.records, 20)
        assertEquals(ScoreImportResult(1, 0, 0), second.result)
        val existing = first.records.single().copy(score = 1_008_000, clear = "hard", fullChain = "fullchain")
        val improvedCombo = DivingFishImportPolicy.plan(payload(row(fc = "alljustice")), catalog, "p", listOf(existing), 20)
        val best = (listOf(existing) + improvedCombo.records).bestScoreSummary()!!
        assertEquals(1_008_000, best.score)
        assertEquals("hard", best.clear)
        assertEquals("fullchain", best.fullChain)
        assertEquals("alljustice", best.fullCombo)
        assertEquals(1, improvedCombo.records.size)
        assertTrue(DivingFishImportPolicy.plan(input, catalog, "p", listOf(existing), 20).records.isEmpty())
        assertEquals(1, DivingFishImportPolicy.plan(input, catalog, "other", first.records, 20).records.size)
    }

    @Test fun maximumScoreInfersAjcAndDuplicateRowsDoNotAddHistory() {
        val input = payload("${row(1_010_000, fc = "alljustice")},${row(1_010_000, fc = "alljustice")}")
        val plan = DivingFishImportPolicy.plan(input, catalog, "p", emptyList(), 10)
        assertEquals(1, plan.records.size)
        assertEquals("alljusticecritical", plan.records.single().fullCombo)
    }

    @Test fun preservesCaseSensitiveSongNamesAndMatchesWorldEndAttributePrefixes() {
        val flowers = listOf(
            song.copy(songId = "upper", title = "FLOWER"),
            song.copy(songId = "mixed", title = "Flower"),
            we,
            we.copy(songId = "other-we", sheets = listOf(CatalogSheet("we", "【狂】"))),
        )
        val input = payload(listOf(
            row().replace("Song", "FLOWER"),
            row().replace("Song", "Flower"),
            row().replace("Song", "flower"), // Ambiguous case-insensitive fallback.
            row(difficulty = 5).replace("Song", "[止]Song"),
            row(difficulty = 5).replace("Song", "【狂】Song"),
            row(difficulty = 5).replace("Song", "[戻]Song"),
        ).joinToString())
        val plan = DivingFishImportPolicy.plan(input, CatalogBundle(1, Catalog(songs = flowers)), "p", emptyList(), 10)
        assertEquals(listOf("upper", "mixed", "we-id", "other-we"), plan.records.map { it.songId })
        assertEquals(ScoreImportResult(6, 4, 2), plan.result)
    }

}
