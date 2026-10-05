package org.rhythmeta.chunithmd.shared.importing

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*
import org.rhythmeta.chunithmd.shared.account.*

@OptIn(ExperimentalCoroutinesApi::class)
class DivingFishClientTest {
    private class Secrets : RhythmetaSecretStore {
        val values = mutableMapOf<String, String>()
        override fun read(key: String) = StoredSecret(values[key])
        override fun write(key: String, value: String?) { if (value == null) values.remove(key) else values[key] = value }
    }
    private fun TestScope.http(handler: MockRequestHandler) = HttpClient(MockEngine(MockEngineConfig().apply {
        dispatcher = StandardTestDispatcher(testScheduler)
        addHandler(handler)
    })) { followRedirects = false }
    private val discovery = """{"issuer":"https://auth.diving-fish.com","device_authorization_endpoint":"https://auth.diving-fish.com/device-test","token_endpoint":"https://auth.diving-fish.com/token-test"}"""
    private val device = """{"device_code":"private-code","user_code":"ABCD-EFGH","verification_uri":"https://auth.diving-fish.com/device","expires_in":60,"interval":5}"""
    private fun token(refresh: String) = """{"access_token":"access","refresh_token":"$refresh","token_type":"Bearer"}"""

    @Test fun discoversEndpointsRequestsReadOnlyScopeAndHonorsSlowDown() = runTest {
        val secrets = Secrets()
        val polls = mutableListOf<Long>()
        http { request ->
            when (request.url.encodedPath) {
                "/.well-known/openid-configuration" -> respond(discovery)
                "/device-test" -> {
                    val form = parseQueryString(request.body.toByteArray().decodeToString())
                    assertEquals(DivingFishClient.CLIENT_ID, form["client_id"])
                    assertEquals("chunithm.records.read", form["scope"])
                    assertEquals("chunithmd", form["binding_label"])
                    assertNull(form["client_secret"])
                    respond(device)
                }
                "/token-test" -> {
                    polls += testScheduler.currentTime
                    when (polls.size) {
                        1 -> respond("""{"error":"authorization_pending"}""", HttpStatusCode.BadRequest)
                        2 -> respond("""{"error":"slow_down"}""", HttpStatusCode.BadRequest)
                        else -> respond(token("refresh"))
                    }
                }
                else -> error("Unexpected URL")
            }
        }.use { http ->
            val client = DivingFishClient(secrets, http) { testScheduler.currentTime }
            val authorization = client.authorize()
            assertEquals("ABCD-EFGH", authorization.userCode)
            client.awaitAuthorization(authorization, "p")
            assertEquals(listOf(5000L, 10000L, 20000L), polls)
            assertTrue(client.isConnected("p"))
            assertFalse(client.isConnected("other"))
        }
    }

    @Test fun concurrentRefreshesUseRotatedTokenAndPersistBeforeRecordsRequest() = runTest {
        val secrets = Secrets().apply { values["diving_fish.refresh.p"] = "old" }
        var refreshes = 0
        http { request ->
            when (request.url.encodedPath) {
                "/.well-known/openid-configuration" -> respond(discovery)
                "/token-test" -> {
                    val form = parseQueryString(request.body.toByteArray().decodeToString())
                    assertEquals(if (refreshes == 0) "old" else "new$refreshes", form["refresh_token"])
                    delay(100)
                    refreshes++
                    respond(token("new$refreshes"))
                }
                "/api/chunithmprober/player/records" -> {
                    assertEquals("Bearer access", request.headers[HttpHeaders.Authorization])
                    assertTrue(request.url.parameters.isEmpty())
                    assertTrue(secrets.values["diving_fish.refresh.p"]!!.startsWith("new"))
                    respond("""{"records":{"best":[]}}""")
                }
                else -> error("Unexpected URL")
            }
        }.use { http ->
            val client = DivingFishClient(secrets, http)
            awaitAll(async { client.records("p") }, async { client.records("p") })
            assertEquals("new2", secrets.values["diving_fish.refresh.p"])
        }
    }

    @Test fun cancellationDuringRotationStillSavesNewTokenWithoutFetchingScores() = runTest {
        val secrets = Secrets().apply { values["diving_fish.refresh.p"] = "old" }
        val started = CompletableDeferred<Unit>()
        http { request ->
            when (request.url.encodedPath) {
                "/.well-known/openid-configuration" -> respond(discovery)
                "/token-test" -> { started.complete(Unit); delay(100); respond(token("rotated")) }
                else -> error("Cancelled import must not fetch records")
            }
        }.use { http ->
            val client = DivingFishClient(secrets, http)
            val job = launch { client.records("p") }
            started.await()
            job.cancelAndJoin()
            assertEquals("rotated", secrets.values["diving_fish.refresh.p"])
        }
    }

    @Test fun expiredDeviceDoesNotPollAndInvalidGrantClearsOnlyItsProfile() = runTest {
        val secrets = Secrets().apply { values["diving_fish.refresh.p"] = "expired"; values["diving_fish.refresh.other"] = "keep" }
        var polls = 0
        http { request ->
            when (request.url.encodedPath) {
                "/.well-known/openid-configuration" -> respond(discovery)
                "/device-test" -> respond(device.replace("\"expires_in\":60", "\"expires_in\":2"))
                else -> { polls++; respond("""{"error":"invalid_grant"}""", HttpStatusCode.BadRequest) }
            }
        }.use { http ->
            val client = DivingFishClient(secrets, http) { testScheduler.currentTime }
            val authorization = client.authorize()
            assertEquals("expired_token", assertFailsWith<DivingFishException> { client.awaitAuthorization(authorization, "p") }.code)
            assertEquals(0, polls)
            assertEquals("invalid_grant", assertFailsWith<DivingFishException> { client.records("p") }.code)
            assertFalse(client.isConnected("p"))
            assertTrue(client.isConnected("other"))
        }
    }

    @Test fun rejectsUntrustedDiscoveryEndpointBeforeSendingCredentials() = runTest {
        http { respond(discovery.replace("https://auth.diving-fish.com/token-test", "https://example.com/token")) }.use { http ->
            assertEquals("invalid_response", assertFailsWith<DivingFishException> { DivingFishClient(Secrets(), http).authorize() }.code)
        }
    }

    @Test fun changingProfileDuringAuthorizationCannotImportIntoEitherProfile() = runTest {
        val secrets = Secrets()
        val exchangeStarted = CompletableDeferred<Unit>()
        http { request ->
            when (request.url.encodedPath) {
                "/.well-known/openid-configuration" -> respond(discovery)
                "/device-test" -> respond(device)
                "/token-test" -> { exchangeStarted.complete(Unit); delay(100); respond(token("saved")) }
                else -> error("A cancelled profile must not fetch scores")
            }
        }.use { http ->
            val client = DivingFishClient(secrets, http) { testScheduler.currentTime }
            var applications = 0
            val controller = DivingFishImportController(client, this) { _, _ ->
                applications++
                ScoreImportResult(0, 0, 0)
            }
            controller.selectProfile("original")
            controller.authorizeAndImport()
            exchangeStarted.await()
            controller.selectProfile("new")
            advanceUntilIdle()
            assertEquals(0, applications)
            assertEquals("new", controller.state.value.profileId)
            assertFalse(controller.state.value.connected)
            assertNull(controller.state.value.result)
            assertNull(controller.state.value.userCode)
            assertTrue(client.isConnected("original"))
            controller.selectProfile("original")
            assertTrue(controller.state.value.connected)
        }
    }

    @Test fun deniedAuthorizationReturnsToIdleWithoutSavingCredentials() = runTest {
        val secrets = Secrets()
        http { request ->
            when (request.url.encodedPath) {
                "/.well-known/openid-configuration" -> respond(discovery)
                "/device-test" -> respond(device)
                else -> respond("""{"error":"access_denied"}""", HttpStatusCode.BadRequest)
            }
        }.use { http ->
            val client = DivingFishClient(secrets, http) { testScheduler.currentTime }
            val controller = DivingFishImportController(client, this) { _, _ -> error("No import on denial") }
            controller.selectProfile("p")
            controller.authorizeAndImport()
            advanceUntilIdle()
            assertFalse(controller.state.value.busy)
            assertFalse(controller.state.value.connected)
            assertNotNull(controller.state.value.error)
            assertNull(controller.state.value.userCode)
        }
    }

}
