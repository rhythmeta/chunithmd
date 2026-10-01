package org.rhythmeta.chunithmd.account

import android.content.Context
import android.util.AtomicFile
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.rhythmeta.chunithmd.BuildConfig
import org.rhythmeta.chunithmd.profile.ProfileRepository
import org.rhythmeta.chunithmd.score.ScoreRepository
import org.rhythmeta.chunithmd.collection.*
import org.rhythmeta.chunithmd.ui.catalog.*
import org.rhythmeta.chunithmd.ui.theme.themePreferencesDataStore
import org.rhythmeta.chunithmd.ui.best.bestTablePreferencesDataStore
import org.rhythmeta.chunithmd.ui.plate.plateProgressDataStore
import org.rhythmeta.chunithmd.shared.backup.*

class AndroidSnapshotStore(
    context: Context,
    private val profiles: ProfileRepository,
    private val scores: ScoreRepository,
    private val collections: SongCollectionRepository,
    private val favorites: FavoriteSongRepository,
) : LocalSnapshotStore {
    private val directory = File(context.noBackupFilesDir, "rhythmeta-backups").apply { mkdirs() }
    private val avatars = File(context.filesDir, "profile-avatars").apply { mkdirs() }
    private val rollback = AtomicFile(File(directory, "restore-pending.pb.gz"))
    private val retained = AtomicFile(File(directory, "retained.pb.gz"))
    private val stores = mapOf("theme" to context.themePreferencesDataStore, "catalog" to context.catalogPreferencesDataStore,
        "best" to context.bestTablePreferencesDataStore, "plate" to context.plateProgressDataStore)
    private val prefix = "android.chunithmd."

    override suspend fun exportSnapshot(): BackupSnapshot = withContext(Dispatchers.IO) {
        val retainedSnapshot = if (retained.baseFile.exists()) BackupCodec.decode(retained.readFully()) else null
        val localCollections = collections.collections.first()
        val profileList = profiles.exportProfiles().map { profile ->
            val bytes = profile.avatarPath?.let(::File)?.takeIf { it.isFile }?.let { file ->
                require(file.length() <= 16 * 1024 * 1024); file.readBytes()
            } ?: byteArrayOf()
            profile.toBackup(bytes)
        }
        BackupSnapshot(magic="RHYTHMETA_BACKUP", formatVersion=1, game="chunithmd", createdAt=System.currentTimeMillis(), clientVersion=BuildConfig.VERSION_NAME,
            profiles=profileList,
            scores=retainedSnapshot?.scores.orEmpty().filter { score -> profileList.any { it.id == score.profileId } },
            playRecords=scores.exportRecords().map { it.toBackup() },
            collections=localCollections.mapIndexed { index, item -> BackupCollection(id=item.id, name=item.name, sortIndex=index) },
            collectionItems=localCollections.flatMap { collection -> collection.entries.mapIndexed { index, entry ->
                BackupCollectionItem(id=UUID.nameUUIDFromBytes("${collection.id}:${entry.key}".encodeToByteArray()).toString(), collectionId=collection.id, songId=entry.songId, chartType=entry.chartType, difficulty=entry.difficulty, position=index)
            } }, favoriteSongIds=favorites.favoriteSongIds.first().sorted(),
            settings=stores.flatMap { (name, store) -> store.data.first().asMap().map { (key,value) -> setting("$prefix$name.${key.name}", value) } } + retainedSnapshot?.settings.orEmpty().filterNot { it.key.startsWith(prefix) })
            .also(BackupCodec::validate)
    }
    override suspend fun replaceSnapshot(snapshot: BackupSnapshot) = withContext(Dispatchers.IO) {
        BackupCodec.validate(snapshot)
        val newProfiles = snapshot.profiles.map { profile ->
            val path = if (profile.avatar.isEmpty()) null else File(avatars, "${profile.id}-${UUID.randomUUID()}.png").also { file ->
                atomicWrite(AtomicFile(file), profile.avatar)
            }.absolutePath
            profile.toProfile(path)
        }
        // A durable rollback journal spans the separate Room databases and DataStores.
        profiles.replaceProfiles(newProfiles)
        scores.replaceRecords(snapshot.playRecords.map { it.toRecord() })
        collections.replaceCollections(snapshot.collections.sortedBy { it.sortIndex }.map { item -> SongCollection(item.id, item.name,
            snapshot.collectionItems.filter { it.collectionId==item.id }.sortedBy { it.position }.map { CollectionEntry(it.songId,it.chartType,it.difficulty) }) })
        favorites.replaceFavorites(snapshot.favoriteSongIds.toSet())
        stores.forEach { (name, store) ->
            val namespace="$prefix$name."
            store.edit { values ->
                values.clear()
                snapshot.settings.filter { it.key.startsWith(namespace) }.forEach { set(values, it.key.removePrefix(namespace), it) }
            }
        }
        atomicWrite(retained, BackupCodec.encode(snapshot))
    }
    override suspend fun readRollback(): ByteArray? = withContext(Dispatchers.IO) { if(rollback.baseFile.exists()) rollback.readFully() else null }
    override suspend fun writeRollback(bytes: ByteArray) = withContext(Dispatchers.IO) { atomicWrite(rollback, bytes) }
    override suspend fun clearRollback() = withContext(Dispatchers.IO) { rollback.delete() }
    private fun atomicWrite(file: AtomicFile, bytes: ByteArray) {
        val output = file.startWrite()
        try { output.write(bytes); file.finishWrite(output) } catch(error: Throwable) { file.failWrite(output); throw error }
    }
    private fun setting(key: String, value: Any): BackupSetting = when(value) {
        is String -> BackupSetting(key=key,kind="string",stringValue=value)
        is Boolean -> BackupSetting(key=key,kind="bool",boolValue=value)
        is Int -> BackupSetting(key=key,kind="int",integerValue=value.toLong())
        is Long -> BackupSetting(key=key,kind="long",integerValue=value)
        is Float -> BackupSetting(key=key,kind="float",doubleValue=value.toDouble())
        is Double -> BackupSetting(key=key,kind="double",doubleValue=value)
        is Set<*> -> BackupSetting(key=key,kind="strings",stringValues=value.filterIsInstance<String>().sorted())
        else -> error("Unsupported preference: $key")
    }
    private fun set(values: MutablePreferences, key: String, item: BackupSetting) {
        when(item.kind) {
            "string" -> values[stringPreferencesKey(key)]=item.stringValue
            "bool" -> values[booleanPreferencesKey(key)]=item.boolValue
            "int" -> values[intPreferencesKey(key)]=item.integerValue.toInt()
            "long" -> values[longPreferencesKey(key)]=item.integerValue
            "float" -> values[floatPreferencesKey(key)]=item.doubleValue.toFloat()
            "double" -> values[doublePreferencesKey(key)]=item.doubleValue
            "strings" -> values[stringSetPreferencesKey(key)]=item.stringValues.toSet()
            else -> error("Unsupported preference type: ${item.kind}")
        }
    }
}
