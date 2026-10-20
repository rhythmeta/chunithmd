package org.rhythmeta.chunithmd.core.update

interface AppUpdateStore {
    suspend fun read(): PendingAppUpdate?
    suspend fun save(update: PendingAppUpdate?)
}
