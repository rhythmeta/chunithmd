package org.rhythmeta.chunithmd.shared.scanner

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveScanStabilizerTest {
    private fun review(title: String = "ナイト・オブ・ナイツ", attribute: String = "時", score: Int? = 969524) = ScanReview(
        fields = ScanFields(title, "WORLD'S END", attribute, score?.toString().orEmpty()),
        parsedScore = score, clear = "clear", combo = "",
        candidates = listOf(ScanChartCandidate("song", "ナイト・オブ・ナイツ", "we", attribute, "☆☆", .9)),
        recommendedKey = null,
    )

    @Test fun firstValidFrameAppearsImmediatelyAndOcrSpellingDoesNotBlockIt() {
        val stabilizer = LiveScanStabilizer()
        assertTrue(stabilizer.accept(review()))
        assertTrue(stabilizer.accept(review(title = "ナイト・オブ・ナイツ!")))
        stabilizer.reset()
        assertTrue(stabilizer.accept(review()))
    }

    @Test fun changesToScoreOrWorldsEndChartRequireConfirmation() {
        val stabilizer = LiveScanStabilizer()
        assertTrue(stabilizer.accept(review()))
        assertFalse(stabilizer.accept(review(attribute = "狂")))
        assertTrue(stabilizer.accept(review(attribute = "狂")))
        assertFalse(stabilizer.accept(review(attribute = "狂", score = 969525)))
        assertTrue(stabilizer.accept(review(attribute = "狂", score = 969525)))
    }

    @Test fun invalidFramesExpireResultAndDoNotPreventNextValidFrame() {
        val stabilizer = LiveScanStabilizer()
        assertFalse(stabilizer.accept(review(score = null)))
        assertFalse(stabilizer.accept(review(attribute = "")))
        assertFalse(stabilizer.accept(review().copy(candidates = emptyList())))
        assertTrue(stabilizer.shouldClear)
        assertTrue(stabilizer.accept(review()))
        assertFalse(stabilizer.shouldClear)
    }
}
