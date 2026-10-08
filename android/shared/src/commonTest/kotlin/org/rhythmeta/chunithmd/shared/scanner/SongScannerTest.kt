package org.rhythmeta.chunithmd.shared.scanner

import kotlin.test.*
import org.rhythmeta.chunithmd.shared.*

class SongScannerTest {
    private fun song(id: String, title: String, region: String = "jp") = CatalogSong(id, title, sheets = listOf(
        CatalogSheet("std", "basic", regions = mapOf(region to true)),
        CatalogSheet("std", "master", regions = mapOf(region to true)),
        CatalogSheet("we", "狂", regions = mapOf(region to true)),
    ))
    @Test fun titleOnlyMatchingUsesSongIdentityAndServerWithoutGuessingAChart() {
        val catalog = CatalogBundle(1, Catalog(songs = listOf(song("1", "Candyland Symphony"), song("2", "Other Song", "cn"))),
            aliases = mapOf("1" to listOf("糖果交响曲")))
        assertEquals("1", SongScanner.match(catalog, "Candyland Symph0ny", "jp")?.songId)
        assertEquals("1", SongScanner.match(catalog, "糖果交响曲", "jp")?.songId)
        assertNull(SongScanner.match(catalog, "Candyland Symphony", "cn"))
        assertNull(SongScanner.match(catalog, "", "jp"))
        assertNull(SongScanner.match(catalog, "MASTER", "jp"))
    }
    @Test fun ambiguousSameTitlesAreNotSilentlyChosen() {
        val catalog = CatalogBundle(1, Catalog(songs = listOf(song("1", "Song"), song("2", "Song"))))
        assertNull(SongScanner.match(catalog, "Song", "jp"))
    }
    @Test fun consecutiveFramesAreRequiredAndOldCardsExpire() {
        val s = LiveSongStabilizer(); val a = ScanSongMatch("a", "A", 1.0); val b = ScanSongMatch("b", "B", 1.0)
        assertFalse(s.accept(a)); assertTrue(s.accept(a))
        assertFalse(s.accept(b)); assertTrue(s.accept(b))
        repeat(3) { assertFalse(s.accept(null)) }; assertTrue(s.shouldClear)
        s.reset(); assertFalse(s.shouldClear); assertFalse(s.accept(a))
    }
    @Test fun titleDetectorContractAndThresholdAreIndependentFromScoreDetector() {
        val raw = floatArrayOf(512f, 512f, 512f, 256f, .9f)
        assertEquals(ScanBox(.25f, .25f, .5f, .5f), ScoreDetection.decodeTitle(raw, 5, 1, 1600, 800).single().box)
        assertFailsWith<IllegalArgumentException> { ScoreDetection.decode(raw, 5, 1, 1600, 800) }
        raw[4] = .3f; assertTrue(ScoreDetection.decodeTitle(raw, 5, 1, 1600, 800).isEmpty())
    }
}
