package org.rhythmeta.chunithmd.shared.scanner

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.rhythmeta.chunithmd.shared.*
import kotlin.test.*

class NativeLiveScoreScannerTest {
    private val catalog = CatalogBundle(1, Catalog(songs = listOf(CatalogSong("song", "Test",
        sheets = listOf(CatalogSheet("std", "master", "15", regions = mapOf("jp" to true)))))))
    private fun observations(score: String) = CatalogJson.codec.encodeToString(listOf(
        ScanObservation("title", "Test", 1f, ScanBox(0f, 0f, 1f, 1f)),
        ScanObservation("difficulty", "MASTER", 1f, ScanBox(0f, 0f, 1f, 1f)),
        ScanObservation("score", score, 1f, ScanBox(0f, 0f, 1f, 1f))))
    private suspend fun NativeLiveScoreScanner.frame(score: String, session: String = "camera", live: Boolean = true) =
        CatalogJson.codec.parseToJsonElement(reviewJson(catalog, observations(score), "jp", live, session)).jsonObject

    @Test fun cameraConfirmsChangesButNewSessionsAndPhotosAcceptImmediately() = runTest {
        val scanner = NativeLiveScoreScanner()
        assertTrue(scanner.frame("1000000").getValue("accepted").jsonPrimitive.boolean)
        assertFalse(scanner.frame("1000001").getValue("accepted").jsonPrimitive.boolean)
        assertTrue(scanner.frame("1000001").getValue("accepted").jsonPrimitive.boolean)
        val fresh = scanner.frame("1000002", session = "new-profile")
        assertTrue(fresh.getValue("accepted").jsonPrimitive.boolean)
        assertEquals("song", fresh.getValue("match").jsonObject.getValue("songId").jsonPrimitive.content)
        assertTrue(scanner.frame("1000003", session = "new-profile", live = false).getValue("accepted").jsonPrimitive.boolean)
    }

    @Test fun emptyFramesExpireCardAndInvalidPhotoScoreCannotBeSaved() = runTest {
        val scanner = NativeLiveScoreScanner()
        scanner.frame("1000000")
        repeat(3) { index ->
            val frame = CatalogJson.codec.parseToJsonElement(scanner.reviewJson(catalog, "[]", "jp", true, "camera")).jsonObject
            assertEquals(index == 2, frame.getValue("shouldClear").jsonPrimitive.boolean)
        }
        assertFalse(scanner.frame("2000000", live = false).getValue("accepted").jsonPrimitive.boolean)
    }
}
