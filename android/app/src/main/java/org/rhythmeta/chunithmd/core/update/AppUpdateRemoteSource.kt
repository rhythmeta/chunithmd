package org.rhythmeta.chunithmd.core.update

interface AppUpdateRemoteSource {
    suspend fun fetchManifest(): AppUpdateManifest
    suspend fun fetchHistory(url: String): AppUpdateHistory
}
