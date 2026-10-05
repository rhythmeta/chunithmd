package org.rhythmeta.chunithmd.shared.importing

import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import org.rhythmeta.chunithmd.shared.account.RhythmetaSecretStore
import kotlin.time.TimeSource

class DivingFishException(val code: String) : Exception(code)

// The device code is deliberately not part of the UI state or a data class's toString().
class DivingFishAuthorization internal constructor(
    internal val deviceCode: String,
    val userCode: String,
    val url: String,
    internal val tokenEndpoint: String,
    internal val expiresAt: Long,
    internal val intervalSeconds: Long,
)

class DivingFishClient(
    private val secrets: RhythmetaSecretStore,
    private val client: HttpClient = HttpClient {
        followRedirects = false
        install(HttpTimeout) { requestTimeoutMillis = 30_000 }
    },
    private val elapsedMillis: () -> Long = monotonicClock(),
) {
    private val tokenMutex = Mutex()
    private var discovery: JsonObject? = null

    fun isConnected(profileId: String): Boolean = !secrets.read(key(profileId)).value.isNullOrBlank()

    suspend fun disconnect(profileId: String) = tokenMutex.withLock {
        secrets.write(key(profileId), null)
    }

    suspend fun authorize(): DivingFishAuthorization {
        val config = configuration()
        val issuedAt = elapsedMillis()
        val response = request(endpoint(config, "device_authorization_endpoint"), mapOf(
            "client_id" to CLIENT_ID,
            "scope" to "chunithm.records.read",
            "binding_label" to "chunithmd",
        ))
        val lifetime = response.long("expires_in")?.takeIf { it in 1..3600 }
            ?: throw DivingFishException("invalid_response")
        return DivingFishAuthorization(
            response.required("device_code"), response.required("user_code"),
            trustedUrl(response.string("verification_uri_complete") ?: response.required("verification_uri")),
            endpoint(config, "token_endpoint"), issuedAt + lifetime * 1000,
            (response.long("interval") ?: 5).coerceIn(5, 3600),
        )
    }

    suspend fun awaitAuthorization(device: DivingFishAuthorization, profileId: String) {
        var interval = device.intervalSeconds
        while (elapsedMillis() < device.expiresAt) {
            delay(interval * 1000)
            if (elapsedMillis() >= device.expiresAt) break
            try {
                tokenMutex.withLock {
                    // The one-time device code is consumed remotely. Always persist the rotated
                    // credential, even when the UI is cancelled while the response is in flight.
                    withContext(NonCancellable) {
                        saveToken(profileId, request(device.tokenEndpoint, mapOf(
                            "client_id" to CLIENT_ID,
                            "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
                            "device_code" to device.deviceCode,
                        )))
                    }
                }
                currentCoroutineContext().ensureActive()
                return
            } catch (error: DivingFishException) {
                when (error.code) {
                    "authorization_pending" -> Unit
                    "slow_down" -> interval += 5
                    else -> throw error
                }
            }
        }
        throw DivingFishException("expired_token")
    }

    suspend fun records(profileId: String): JsonObject {
        val endpoint = endpoint(configuration(), "token_endpoint")
        val access = tokenMutex.withLock {
            withContext(NonCancellable) {
                val refresh = secrets.read(key(profileId)).value?.takeIf { it.isNotBlank() }
                    ?: throw DivingFishException("invalid_grant")
                try {
                    saveToken(profileId, request(endpoint, mapOf(
                        "client_id" to CLIENT_ID, "grant_type" to "refresh_token", "refresh_token" to refresh,
                    )))
                } catch (error: DivingFishException) {
                    if (error.code == "invalid_grant") secrets.write(key(profileId), null)
                    throw error
                }
            }
        }
        currentCoroutineContext().ensureActive()
        return request(RECORDS_URL, bearer = access)
    }

    private fun saveToken(profileId: String, response: JsonObject): String {
        val access = response.required("access_token")
        val refresh = response.required("refresh_token")
        secrets.write(key(profileId), refresh)
        return access
    }

    private suspend fun configuration(): JsonObject = discovery ?: request(
        "$AUTH_BASE/.well-known/openid-configuration",
    ).also { config ->
        if (config.string("issuer") != AUTH_BASE) throw DivingFishException("invalid_response")
        endpoint(config, "device_authorization_endpoint")
        endpoint(config, "token_endpoint")
        discovery = config
    }

    private fun endpoint(config: JsonObject, key: String) = trustedUrl(config.required(key))
    private fun trustedUrl(value: String): String {
        val url = Url(value)
        if (url.protocol != URLProtocol.HTTPS || url.host != "auth.diving-fish.com" ||
            url.port != 443 || !url.user.isNullOrEmpty() || !url.password.isNullOrEmpty()) {
            throw DivingFishException("invalid_response")
        }
        return value
    }

    private suspend fun request(url: String, form: Map<String, String>? = null, bearer: String? = null): JsonObject {
        val response = client.request(url) {
            expectSuccess = false
            accept(ContentType.Application.Json)
            bearer?.let { bearerAuth(it) }
            if (form != null) {
                method = HttpMethod.Post
                setBody(FormDataContent(Parameters.build { form.forEach { (key, value) -> append(key, value) } }))
            }
        }
        val payload = runCatching { Json.parseToJsonElement(response.bodyAsText()) as? JsonObject }.getOrNull()
        currentCoroutineContext().ensureActive()
        if (!response.status.isSuccess()) throw DivingFishException(
            payload?.string("error") ?: when (response.status.value) {
                401 -> "unauthorized"
                403 -> "forbidden"
                429 -> "rate_limited"
                else -> "http_error"
            },
        )
        return payload ?: throw DivingFishException("invalid_response")
    }

    private fun key(profileId: String) = "diving_fish.refresh.$profileId"

    companion object {
        const val CLIENT_ID = "51e02d2edc681939641788153eb6361e"
        const val AUTH_BASE = "https://auth.diving-fish.com"
        const val RECORDS_URL = "https://www.diving-fish.com/api/chunithmprober/player/records"
    }
}

private fun monotonicClock(): () -> Long {
    val start = TimeSource.Monotonic.markNow()
    return { start.elapsedNow().inWholeMilliseconds }
}
internal fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
private fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull
private fun JsonObject.required(key: String): String = string(key)?.takeIf { it.isNotBlank() }
    ?: throw DivingFishException("invalid_response")
