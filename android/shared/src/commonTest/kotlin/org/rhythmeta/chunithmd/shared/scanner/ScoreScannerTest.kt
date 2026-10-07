package org.rhythmeta.chunithmd.shared.scanner

import kotlin.test.*
import org.rhythmeta.chunithmd.shared.*

class ScoreScannerTest {
    @Test fun restoresLetterboxedCoordinatesAndRejectsPadding() {
        val raw = floatArrayOf(512f, 512f, 512f, 256f, 0f, 0f, 0f, .9f, 0f, 0f)
        val box = ScoreDetection.decode(raw, 10, 1, 1600, 800).single().box
        assertEquals(ScanBox(.25f, .25f, .5f, .5f), box)
        raw[1] = 20f; raw[3] = 10f
        assertTrue(ScoreDetection.decode(raw, 10, 1, 1600, 800).isEmpty())
        assertFailsWith<IllegalArgumentException> { ScoreDetection.decode(raw, 6, 1, 1600, 800) }
    }
    @Test fun parsesGroupedFullWidthScoreButRejectsAmbiguousAndOutOfRangeValues() {
        assertEquals(1_007_500, ScoreScanner.parseScore("１，００７，５００"))
        assertNull(ScoreScanner.parseScore("1,007,500\n1,009,999"))
        assertNull(ScoreScanner.parseScore("1,007,500\n1,999,999"))
        assertNull(ScoreScanner.parseScore("1,999,999"))
        assertNull(ScoreScanner.parseScore(""))
        assertEquals(0, ScoreScanner.parseScore("0"))
    }
    private val catalog = CatalogBundle(1, Catalog(songs = listOf(CatalogSong("song", "同一首歌", sheets = listOf(
        CatalogSheet("we", "狂", "☆☆☆", regions = mapOf("jp" to true)),
        CatalogSheet("we", "止", "☆☆", regions = mapOf("jp" to true)),
        CatalogSheet("std", "master", "14+", regions = mapOf("jp" to true)),
    )))))
    @Test fun worldsEndAttributeIsChartIdentityAndMissingAttributeStaysAmbiguous() {
        assertEquals("時", ScoreScanner.attribute("【时】"))
        val review = ScoreScanner.review(catalog, ScanFields("同一首歌", "WORLD'S END", "止", "1,007,500"), "jp")
        assertEquals("song:we:止", review.recommendedKey)
        assertEquals(1, review.candidates.size)
        assertNull(ScoreScanner.review(catalog, ScanFields("同一首歌", "WORLD'S END"), "jp").recommendedKey)
        assertTrue(ScoreScanner.review(catalog, ScanFields("同一首歌", "WORLD'S END", "狂"), "cn").candidates.isEmpty())
        assertEquals("song:we:止", ScoreScanner.prepareSave(catalog, "jp", "song:we:止", "1,007,500", "clear", "")?.sheetKey)
        assertNull(ScoreScanner.prepareSave(catalog, "jp", "song:we:？", "1,007,500", "clear", ""))
        assertNull(ScoreScanner.prepareSave(catalog, "jp", "song:we:止", "1,999,999", "clear", ""))
        assertNull(ScoreScanner.prepareSave(catalog, "cn", "song:we:止", "1,007,500", "clear", ""))
        assertNull(ScoreScanner.prepareSave(catalog, "jp", "song:we:止", "1,007,500", "unknown", ""))
    }

    @Test fun automaticallyMatchesOcrTypoWithoutRequiringAChartPicker() {
        val review = ScoreScanner.review(catalog, ScanFields("同一首哥", "MASTER", "14+", "1,007,500"), "jp")
        assertNull(review.recommendedKey)
        assertEquals("song:std:master", ScoreScanner.automaticMatch(review)?.key)
        assertNull(ScoreScanner.automaticMatch(review.copy(candidates = review.candidates.map { it.copy(similarity = .5) })))
        val rival = review.candidates.single().copy(songId = "other", similarity = .72)
        assertNull(ScoreScanner.automaticMatch(review.copy(candidates = review.candidates + rival)))
        assertNull(ScoreScanner.automaticMatch(review.copy(fields = review.fields.copy(difficulty = ""))))
    }

    @Test fun automaticWorldsEndMatchingRequiresItsAttributeAndRegion() {
        val fields = ScanFields("同一首歌", "WORLD'S END", "止", "1,007,500")
        assertEquals("song:we:止", ScoreScanner.automaticMatch(ScoreScanner.review(catalog, fields, "jp"))?.key)
        assertNull(ScoreScanner.automaticMatch(ScoreScanner.review(catalog, fields.copy(level = ""), "jp")))
        assertNull(ScoreScanner.automaticMatch(ScoreScanner.review(catalog, fields, "cn")))
    }
}
