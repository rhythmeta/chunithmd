package org.rhythmeta.chunithmd.shared.importing

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import okio.ByteString.Companion.encodeUtf8
import kotlin.test.*
import org.rhythmeta.chunithmd.shared.account.*

@OptIn(ExperimentalCoroutinesApi::class)
class LxnsClientTest {
    private class Secrets : RhythmetaSecretStore {
        val values = mutableMapOf<String, String>()
        override fun read(key: String) = StoredSecret(values[key])
        override fun write(key: String, value: String?) { if (value == null) values.remove(key) else values[key] = value }
    }
    private fun TestScope.http(handler: MockRequestHandler) = HttpClient(MockEngine(MockEngineConfig().apply {
        dispatcher = StandardTestDispatcher(testScheduler)
        addHandler(handler)
    })) { followRedirects = false }
    private fun token(refresh: String) = """{"access_token":"access","refresh_token":"$refresh","token_type":"Bearer"}"""

    @Test fun oobAuthorizationUsesPkceAndResumesWithSameVerifierAfterRecreation() = runTest {
        val secrets = Secrets()
        var authorization: Url? = null
        http { request ->
            assertEquals(LxnsClient.TOKEN_URL, request.url.toString())
            val form = parseQueryString(request.body.toByteArray().decodeToString())
            assertEquals(LxnsClient.CLIENT_ID, form["client_id"])
            assertEquals(LxnsClient.REDIRECT_URI, form["redirect_uri"])
            assertEquals("authorization_code", form["grant_type"])
            assertEquals("ABCD-EFGH-IJKL", form["code"])
            assertNull(form["client_secret"])
            val verifier = assertNotNull(form["code_verifier"])
            assertEquals(authorization!!.parameters["code_challenge"], verifier.encodeUtf8().sha256().base64Url().trimEnd('='))
            respond(token("refresh"))
        }.use { http ->
            val client = LxnsClient(secrets, http) { 1L }
            authorization = Url(client.beginAuthorization("p"))
            val params = authorization.parameters
            assertEquals("read_player", params["scope"])
            assertEquals("S256", params["code_challenge_method"])
            assertEquals("code", params["response_type"])
            assertEquals(LxnsClient.CLIENT_ID, params["client_id"])
            assertEquals(LxnsClient.REDIRECT_URI, params["redirect_uri"])
            assertNotNull(params["state"])
            assertNull(params["code_verifier"])
            assertNull(params["client_secret"])
            val restarted = LxnsClient(secrets, http) { 2L }
            assertEquals(authorization.toString(), restarted.authorizationUrl("p"))
            assertNull(restarted.authorizationUrl("other"))
            restarted.exchange("p", " ABCD-EFGH-IJKL ")
            assertTrue(restarted.isConnected("p"))
            assertNull(restarted.authorizationUrl("p"))
            assertFalse(restarted.isConnected("other"))
        }
    }

    @Test fun repeatedAuthorizationReplacesChallengeAndExpiredPendingNeverExchanges() = runTest {
        var now = 0L
        http { error("Expired or unrelated profiles must not exchange codes") }.use { http ->
            val client = LxnsClient(Secrets(), http) { now }
            val first = Url(client.beginAuthorization("p"))
            val second = Url(client.beginAuthorization("p"))
            assertNotEquals(first.parameters["code_challenge"], second.parameters["code_challenge"])
            assertEquals("missing_pkce", assertFailsWith<LxnsException> { client.exchange("other", "code") }.code)
            now = 1_800_001
            assertNull(client.authorizationUrl("p"))
            assertEquals("missing_pkce", assertFailsWith<LxnsException> { client.exchange("p", "code") }.code)
        }
    }

    @Test fun refreshRotationIsSerializedAndSavedBeforeUsingChunithmBearerEndpoint() = runTest {
        val secrets = Secrets().apply { values["lxns.refresh.p"] = "old" }
        var count = 0
        http { request ->
            when (request.url.toString()) {
                LxnsClient.TOKEN_URL -> {
                    val form = parseQueryString(request.body.toByteArray().decodeToString())
                    assertEquals("refresh_token", form["grant_type"])
                    assertNull(form["client_secret"])
                    assertEquals(if (count == 0) "old" else "new$count", form["refresh_token"])
                    delay(100)
                    count++
                    respond(token("new$count"))
                }
                LxnsClient.RECORDS_URL -> {
                    assertEquals("Bearer access", request.headers[HttpHeaders.Authorization])
                    assertTrue(secrets.values["lxns.refresh.p"]!!.startsWith("new"))
                    respond("""{"success":true,"data":[]}""")
                }
                else -> error("Unexpected URL")
            }
        }.use { http ->
            val client = LxnsClient(secrets, http)
            awaitAll(async { client.records("p") }, async { client.records("p") })
            assertEquals("new2", secrets.values["lxns.refresh.p"])
        }
    }

    @Test fun invalidGrantClearsOnlyLxnsBindingForThatProfile() = runTest {
        val secrets = Secrets().apply {
            values["lxns.refresh.p"] = "expired"
            values["lxns.refresh.other"] = "keep"
            values["diving_fish.refresh.p"] = "keep-fish"
        }
        http { respond("""{"error":"invalid_grant"}""", HttpStatusCode.BadRequest) }.use { http ->
            val client = LxnsClient(secrets, http)
            assertEquals("invalid_grant", assertFailsWith<LxnsException> { client.records("p") }.code)
            assertFalse(client.isConnected("p"))
            assertTrue(client.isConnected("other"))
            assertEquals("keep-fish", secrets.values["diving_fish.refresh.p"])
        }
    }

    @Test fun cancellingConsumedCodeSavesTokenButNeverImportsIntoChangedProfile() = runTest {
        val secrets = Secrets()
        val started = CompletableDeferred<Unit>()
        http { request ->
            assertEquals(LxnsClient.TOKEN_URL, request.url.toString())
            started.complete(Unit)
            delay(100)
            respond(token("saved"))
        }.use { http ->
            val client = LxnsClient(secrets, http)
            var applied = 0
            val controller = LxnsImportController(client, this) { _, _ -> applied++; ScoreImportResult(0, 0, 0) }
            controller.selectProfile("p")
            controller.beginAuthorization()
            advanceUntilIdle()
            controller.setCode("code")
            controller.exchangeAndImport()
            started.await()
            controller.selectProfile("other")
            advanceUntilIdle()
            assertEquals(0, applied)
            assertEquals("other", controller.state.value.profileId)
            assertFalse(controller.state.value.connected)
            assertTrue(client.isConnected("p"))
            assertNull(client.authorizationUrl("p"))
        }
    }

    @Test fun unsuccessfulApiEnvelopeIsAnErrorAndPublicCatalogHasNoBearer() = runTest {
        val secrets = Secrets().apply { values["lxns.refresh.p"] = "old" }
        http { request ->
            when (request.url.toString()) {
                LxnsClient.TOKEN_URL -> respond(token("new"))
                LxnsClient.RECORDS_URL -> respond("""{"success":false,"code":404,"message":"missing"}""")
                else -> { assertNull(request.headers[HttpHeaders.Authorization]); respond("""{"songs":[]}""") }
            }
        }.use { http ->
            val client = LxnsClient(secrets, http)
            assertEquals("player_missing", assertFailsWith<LxnsException> { client.records("p") }.code)
            client.songs()
        }
    }
}
