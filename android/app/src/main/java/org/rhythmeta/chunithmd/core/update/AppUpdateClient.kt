package org.rhythmeta.chunithmd.core.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException
import java.net.HttpURLConnection
import java.net.URL

class AppUpdateClient(private val updateUrl: String, private val json: Json) : AppUpdateRemoteSource {
    override suspend fun fetchManifest(): AppUpdateManifest {
        val body = get(updateUrl)
        return try { json.decodeFromString(body) }
        catch (_: SerializationException) { throw AppUpdateException(AppUpdateFailure.InvalidManifest) }
    }
    override suspend fun fetchHistory(url: String): AppUpdateHistory = json.decodeFromString(get(url))

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.setRequestProperty("User-Agent", "chunithmd-android")
            if (connection.responseCode !in 200..299) throw AppUpdateException(AppUpdateFailure.Network)
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
