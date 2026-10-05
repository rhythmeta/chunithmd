package org.rhythmeta.chunithmd.shared.importing

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.*

class OtogameClient(private val client: HttpClient = HttpClient {
    followRedirects = false
    install(HttpTimeout) { requestTimeoutMillis = 30_000 }
}) {
    suspend fun records(authorization: String, onProgress: (Int, Int) -> Unit = { _, _ -> }): OtogamePayload {
        if (!isAuthorization(authorization)) throw OtogameException("unauthorized")
        var fetched = 0
        val plays = mutableListOf<OtogamePlay>()
        for (page in 1..OtogameImportPolicy.PAGE_LIMIT) {
            currentCoroutineContext().ensureActive()
            val response = client.get(RECORDS_URL) {
                expectSuccess = false
                parameter("page", page)
                header(HttpHeaders.Authorization, authorization)
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.Referrer, MUSIC_URL)
            }
            when (response.status.value) {
                200 -> Unit
                401, 403 -> throw OtogameException("unauthorized")
                else -> throw OtogameException("http_error")
            }
            val body = response.bodyAsText()
            val json = runCatching { Json.parseToJsonElement(body).jsonObject }
                .getOrElse { throw OtogameException("invalid_response") }
            val result = OtogameImportPolicy.decode(json)
            if (result.page != page) throw OtogameException("invalid_response")
            fetched += result.payload.fetched
            plays += result.payload.plays
            onProgress(page, result.totalPages)
            if (result.payload.fetched == 0 || page >= result.totalPages) break
        }
        return OtogamePayload(fetched, plays)
    }

    companion object {
        const val MUSIC_URL = "https://u.otogame.net/chunithm/music"
        const val RECORDS_URL = "https://u.otogame.net/api/game/chunithm/playlog"
        fun isAuthorization(value: String) = value.length <= 16_384 && Regex("(?i)^Bearer [^\\s]+$").matches(value)
    }
}
