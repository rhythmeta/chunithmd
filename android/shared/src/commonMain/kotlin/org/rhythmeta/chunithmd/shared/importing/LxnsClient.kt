package org.rhythmeta.chunithmd.shared.importing

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.*
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import org.rhythmeta.chunithmd.shared.account.RhythmetaSecretStore
import org.rhythmeta.chunithmd.shared.backup.secureRandomBytes
import kotlin.time.Clock

@Serializable
private class LxnsPending(val verifier: String, val state: String, val createdAt: Long)

/** Public native client: PKCE S256, OOB code entry, and per-profile encrypted credentials. */
class LxnsClient(
    private val secrets: RhythmetaSecretStore,
    private val client: HttpClient = HttpClient {
        followRedirects = false
        install(HttpTimeout) { requestTimeoutMillis = 30_000 }
    },
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    fun isConnected(profileId: String) = !secrets.read(tokenKey(profileId)).value.isNullOrBlank()
    fun authorizationUrl(profileId: String): String? = pending(profileId)?.let(::url)

    suspend fun beginAuthorization(profileId: String): String = mutex.withLock {
        val pending = LxnsPending(randomToken(), randomToken(), clock())
        secrets.write(pendingKey(profileId), json.encodeToString(pending))
        url(pending)
    }

    suspend fun disconnect(profileId: String) = mutex.withLock {
        secrets.write(tokenKey(profileId), null)
        secrets.write(pendingKey(profileId), null)
    }

    suspend fun exchange(profileId: String, code: String) = mutex.withLock {
        val pending = pending(profileId) ?: throw LxnsException("missing_pkce")
        val normalizedCode = code.trim()
        if (normalizedCode.isEmpty() || normalizedCode.length > 4096 || normalizedCode.any(Char::isWhitespace))
            throw LxnsException("invalid_code")
        withContext(NonCancellable) {
            // Save the new refresh token before honoring cancellation of a consumed code.
            saveToken(profileId, request(TOKEN_URL, mapOf(
                "client_id" to CLIENT_ID, "grant_type" to "authorization_code", "code" to normalizedCode,
                "redirect_uri" to REDIRECT_URI, "code_verifier" to pending.verifier,
            )))
            secrets.write(pendingKey(profileId), null)
        }
        currentCoroutineContext().ensureActive()
    }

    suspend fun records(profileId: String): JsonObject {
        val access = mutex.withLock {
            withContext(NonCancellable) {
                val refresh = secrets.read(tokenKey(profileId)).value?.takeIf { it.isNotBlank() }
                    ?: throw LxnsException("invalid_grant")
                try {
                    saveToken(profileId, request(TOKEN_URL, mapOf(
                        "client_id" to CLIENT_ID, "grant_type" to "refresh_token", "refresh_token" to refresh,
                    )))
                } catch (error: LxnsException) {
                    if (error.code == "invalid_grant") secrets.write(tokenKey(profileId), null)
                    throw error
                }
            }
        }
        currentCoroutineContext().ensureActive()
        return request(RECORDS_URL, bearer = access)
    }

    suspend fun songs(): JsonObject = request("$BASE/api/v0/chunithm/song/list")

    private fun pending(id: String): LxnsPending? = secrets.read(pendingKey(id)).value?.let {
        runCatching { json.decodeFromString<LxnsPending>(it) }.getOrNull()
    }?.takeIf { clock() - it.createdAt in 0..1_800_000 }

    private fun url(pending: LxnsPending): String = "$BASE/oauth/authorize?" + listOf(
        "response_type" to "code", "client_id" to CLIENT_ID, "redirect_uri" to REDIRECT_URI,
        "scope" to "read_player", "state" to pending.state, "code_challenge_method" to "S256",
        "code_challenge" to pending.verifier.encodeUtf8().sha256().base64Url().trimEnd('='),
    ).formUrlEncode()

    private fun saveToken(profileId: String, response: JsonObject): String {
        val access = response.required("access_token")
        val refresh = response.required("refresh_token")
        secrets.write(tokenKey(profileId), refresh)
        return access
    }

    private suspend fun request(address: String, form: Map<String, String>? = null, bearer: String? = null): JsonObject {
        val response = client.request(address) {
            expectSuccess = false
            accept(ContentType.Application.Json)
            bearer?.let { bearerAuth(it) }
            if (form != null) {
                method = HttpMethod.Post
                setBody(FormDataContent(Parameters.build { form.forEach { (key, value) -> append(key, value) } }))
            }
        }
        val payload = runCatching { json.parseToJsonElement(response.bodyAsText()) as? JsonObject }.getOrNull()
        currentCoroutineContext().ensureActive()
        val success = (payload?.get("success") as? JsonPrimitive)?.booleanOrNull
        if (!response.status.isSuccess() || success == false || payload?.string("error") != null) {
            val code = if (response.status.isSuccess()) (payload?.get("code") as? JsonPrimitive)?.intOrNull else response.status.value
            throw LxnsException(payload?.string("error") ?: when (code) {
                401 -> "unauthorized"
                403 -> "forbidden"
                404 -> "player_missing"
                429 -> "rate_limited"
                else -> "http_error"
            })
        }
        return payload ?: throw LxnsException("invalid_response")
    }

    private fun randomToken() = secureRandomBytes(32).toByteString().base64Url().trimEnd('=')
    private fun tokenKey(id: String) = "lxns.refresh.$id"
    private fun pendingKey(id: String) = "lxns.pending.$id"
    private fun JsonObject.required(key: String) = string(key)?.takeIf { it.isNotBlank() } ?: throw LxnsException("invalid_response")

    companion object {
        const val CLIENT_ID = "b1544b93-0ebf-4f30-9096-d7e6043f0c9d"
        const val REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob"
        const val BASE = "https://maimai.lxns.net"
        const val TOKEN_URL = "$BASE/api/v0/oauth/token"
        const val RECORDS_URL = "$BASE/api/v0/user/chunithm/player/scores"
    }
}
