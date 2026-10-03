package org.rhythmeta.chunithmd.shared.account

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BackendHealthTest {
    private fun TestScope.http(handler: MockRequestHandler) = HttpClient(MockEngine(MockEngineConfig().apply {
        dispatcher = StandardTestDispatcher(testScheduler)
        addHandler(handler)
    }))

    private val secrets = object : RhythmetaSecretStore {
        override fun read(key: String) = StoredSecret(null)
        override fun write(key: String, value: String?) = error("Health checks must not change credentials")
    }

    @Test fun checksPublicEndpointWithoutLogin() = runTest {
        http { request ->
            assertEquals("https://api.rhythmeta.org/health", request.url.toString())
            assertEquals("app", request.headers["X-Rhythmeta-Client"])
            assertNull(request.headers["Authorization"])
            respond("""{"message":"ok"}""")
        }.use { http ->
            assertTrue(RhythmetaClient(secrets, http).isHealthy())
        }
    }

    @Test fun serverFailureIsUnavailable() = runTest {
        http { respond("unavailable", HttpStatusCode.ServiceUnavailable) }.use { http ->
            assertFalse(RhythmetaClient(secrets, http).isHealthy())
        }
    }

    @Test fun networkFailureIsUnavailable() = runTest {
        http { error("offline") }.use { http ->
            assertFalse(RhythmetaClient(secrets, http).isHealthy())
        }
    }

    @Test fun slowRequestsTimeOutAndCallerCancellationPropagates() = runTest {
        http { awaitCancellation() }.use { http ->
            val client = RhythmetaClient(secrets, http)
            assertFalse(client.isHealthy())
            assertEquals(5_000, testScheduler.currentTime)
            var returned = false
            val job = launch { client.isHealthy(); returned = true }
            testScheduler.runCurrent()
            job.cancelAndJoin()
            assertFalse(returned)
        }
    }
}
