package org.rhythmeta.chunithmd.shared.importing

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.rhythmeta.chunithmd.shared.ProfileServer
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class OtogameClientTest {
    private fun TestScope.http(handler: MockRequestHandler) = HttpClient(MockEngine(MockEngineConfig().apply {
        dispatcher = StandardTestDispatcher(testScheduler)
        addHandler(handler)
    })) { followRedirects = false }

    @Test fun fetchesOnlyFourPagesWithAuthorizationAndReportsProgress() = runTest {
        val pages = mutableListOf<Int>()
        val progress = mutableListOf<Pair<Int, Int>>()
        http { request ->
            assertEquals("u.otogame.net", request.url.host)
            assertEquals("/api/game/chunithm/playlog", request.url.encodedPath)
            assertEquals("Bearer test-session", request.headers[HttpHeaders.Authorization])
            assertEquals(OtogameClient.MUSIC_URL, request.headers[HttpHeaders.Referrer])
            val page = request.url.parameters["page"]!!.toInt()
            pages += page
            respond(otogamePage(page))
        }.use { http ->
            val result = OtogameClient(http).records("Bearer test-session") { page, total -> progress += page to total }
            assertEquals(listOf(1, 2, 3, 4), pages)
            assertEquals(listOf(1 to 4, 2 to 4, 3 to 4, 4 to 4), progress)
            assertEquals(4, result.fetched)
        }
    }

    @Test fun stopsAtLastPageAndAtEmptyPages() = runTest {
        for (empty in listOf(false, true)) {
            var requests = 0
            http {
                requests++
                respond(otogamePage(requests, total = if (empty) 12 else 2, rows = if (empty && requests == 2) "" else otogameRow()))
            }.use { http ->
                val result = OtogameClient(http).records("Bearer test")
                assertEquals(2, requests)
                assertEquals(if (empty) 1 else 2, result.fetched)
            }
        }
    }

    @Test fun invalidHeadersUnauthorizedAndFailedEnvelopesNeverReturnPartialData() = runTest {
        http { error("No request for invalid session") }.use { http ->
            for (value in listOf("", "Bearer ", "Basic secret", "Bearer token\r\nOther: value"))
                assertEquals("unauthorized", assertFailsWith<OtogameException> { OtogameClient(http).records(value) }.code)
        }
        for (status in listOf(HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden)) {
            http { respond("{}", status) }.use { http ->
                assertEquals("unauthorized", assertFailsWith<OtogameException> { OtogameClient(http).records("Bearer test") }.code)
            }
        }
        for (body in listOf("<html>login</html>", "{}", otogamePage(page = 1))) {
            var requests = 0
            http { requests++; respond(if (requests == 1) otogamePage() else body) }.use { http ->
                assertEquals("invalid_response", assertFailsWith<OtogameException> { OtogameClient(http).records("Bearer test") }.code)
            }
        }
    }

    @Test fun redirectsDoNotForwardSessionToAnotherHost() = runTest {
        var requests = 0
        http {
            requests++
            respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "https://other.example/"))
        }.use { http ->
            assertEquals("http_error", assertFailsWith<OtogameException> { OtogameClient(http).records("Bearer test") }.code)
            assertEquals(1, requests)
        }
    }

    @Test fun switchingProfilesCancelsPaginationAndDropsSessionAndLateCapture() = runTest {
        var imports = 0
        var requests = 0
        http { requests++; delay(1000); respond(otogamePage()) }.use { http ->
            val controller = OtogameImportController(OtogameClient(http), this) { _, _ -> imports++; ScoreImportResult(1, 1, 0) }
            controller.selectProfile("first", ProfileServer.Jp)
            controller.captureAuthorization("first", "Bearer first")
            controller.importScores()
            runCurrent()
            controller.selectProfile("second", ProfileServer.Jp)
            controller.captureAuthorization("first", "Bearer late")
            advanceUntilIdle()
            assertEquals(1, requests)
            assertEquals(0, imports)
            assertFalse(controller.state.value.connected)
            assertFalse(controller.state.value.busy)
            assertNull(controller.state.value.result)
            assertNull(controller.state.value.error)
        }
    }

    @Test fun unauthorizedClearsSessionAndNonJapaneseProfilesCannotImport() = runTest {
        var requests = 0
        http { requests++; respond("{}", HttpStatusCode.Unauthorized) }.use { http ->
            val controller = OtogameImportController(OtogameClient(http), this) { _, _ -> error("No imports") }
            controller.selectProfile("p", ProfileServer.Jp)
            controller.captureAuthorization("p", "Bearer test")
            controller.importScores()
            advanceUntilIdle()
            assertFalse(controller.state.value.connected)
            assertNotNull(controller.state.value.error)
            controller.selectProfile("p", ProfileServer.Cn)
            controller.captureAuthorization("p", "Bearer test")
            controller.importScores()
            advanceUntilIdle()
            assertEquals(1, requests)
            assertFalse(controller.state.value.eligible)
        }
    }

    @Test fun successfulImportUsesOriginalProfileAndRejectsDuplicateStarts() = runTest {
        var imports = 0
        http { respond(otogamePage(total = 1)) }.use { http ->
            val controller = OtogameImportController(OtogameClient(http), this) { id, payload ->
                assertEquals("p", id)
                imports++
                ScoreImportResult(payload.fetched, 1, 0)
            }
            controller.selectProfile("p", ProfileServer.Jp)
            controller.captureAuthorization("p", "Bearer test")
            controller.importScores()
            controller.importScores()
            advanceUntilIdle()
            assertEquals(1, imports)
            assertEquals(ScoreImportResult(1, 1, 0), controller.state.value.result)
            assertFalse(controller.state.value.busy)
            assertTrue(controller.state.value.connected)
            controller.disconnect()
            assertFalse(controller.state.value.connected)
        }
    }
}
