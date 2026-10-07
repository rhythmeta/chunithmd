package org.rhythmeta.chunithmd.shared.scanner

import kotlin.test.*

class PaddleTextRecognitionTest {
    @Test fun ctcPreservesAsciiCjkSurrogatesSpacesAndBlankSeparatedRepeats() {
        val vocabulary = MutableList(PaddleTextRecognition.classes) { "" }
        val tokens = (32..126).map { it.toChar().toString() } + listOf("中", "國", "日", "あ", "ア", "時", "𠮷")
        tokens.forEachIndexed { index, token -> vocabulary[index + 1] = token }
        val ids = tokens.indices.flatMap { listOf(it + 1, it + 1, 0) }.toIntArray()
        val decoded = PaddleTextRecognition.decode(ids, FloatArray(ids.size) { .9f }, vocabulary)
        assertEquals(tokens.joinToString(""), decoded.text)
        assertEquals(.9f, decoded.confidence, .0001f)
        assertEquals("!!", PaddleTextRecognition.decode(intArrayOf(2, 2, 0, 2), floatArrayOf(1f, 1f, 1f, 1f), vocabulary).text)
        assertEquals("", PaddleTextRecognition.decode(intArrayOf(0, 0), floatArrayOf(1f, 1f), vocabulary).text)
    }

    @Test fun bucketsPreserveAspectRatioAndBoundMemory() {
        assertEquals(PaddleTextSize(320, 48), PaddleTextRecognition.size(100, 100))
        assertEquals(PaddleTextSize(640, 480), PaddleTextRecognition.size(1000, 100))
        assertEquals(PaddleTextSize(2560, 2560), PaddleTextRecognition.size(10000, 10))
        assertFails { PaddleTextRecognition.size(1, 0) }
    }
}
