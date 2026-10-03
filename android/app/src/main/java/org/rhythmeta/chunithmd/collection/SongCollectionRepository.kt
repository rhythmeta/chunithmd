package org.rhythmeta.chunithmd.collection

import org.rhythmeta.chunithmd.shared.localization.tr

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.songCollectionsDataStore by preferencesDataStore(name = "song_collections")
internal val CollectionJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/** Local collections are shared by profiles, just like the song library. Edits are atomic. */
class SongCollectionRepository internal constructor(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.songCollectionsDataStore)

    val collections = store.data.map { values -> decode(values[CollectionsKey]) }

    suspend fun replaceCollections(collections: List<SongCollection>) = update { collections }

    suspend fun create(name: String): String = importCollection(CollectionExport(name, emptyList()))

    suspend fun importCollection(source: CollectionExport): String {
        val normalized = source.validated()
        val id = UUID.randomUUID().toString()
        update { collections ->
            collections + SongCollection(id, uniqueName(normalized.name, collections.map { it.name }.toSet()), normalized.entries)
        }
        return id
    }

    suspend fun rename(id: String, name: String) {
        val trimmed = name.trim().take(40)
        require(trimmed.isNotEmpty()) { tr("请输入收藏夹名称") }
        update { collections ->
            val unique = uniqueName(trimmed, collections.filter { it.id != id }.map { it.name }.toSet())
            collections.map { if (it.id == id) it.copy(name = unique) else it }
        }
    }

    suspend fun delete(id: String) = update { collections -> collections.filterNot { it.id == id } }

    suspend fun setMembership(collectionId: String, entry: CollectionEntry, included: Boolean) {
        val normalized = entry.normalized()
        update { collections ->
            require(collections.any { it.id == collectionId }) { tr("收藏夹已不存在") }
            collections.map { collection ->
                if (collection.id != collectionId) collection
                else collection.copy(entries = if (included) {
                    (collection.entries + normalized).distinctBy { it.key }
                } else collection.entries.filterNot { it.key == normalized.key })
            }
        }
    }

    private suspend fun update(transform: (List<SongCollection>) -> List<SongCollection>) {
        store.edit { values -> values[CollectionsKey] = CollectionJson.encodeToString(transform(decode(values[CollectionsKey]))) }
    }

    private fun decode(value: String?): List<SongCollection> = value?.let { CollectionJson.decodeFromString<List<SongCollection>>(it) }.orEmpty()

    private fun uniqueName(name: String, existing: Set<String>): String {
        val base = name.take(40)
        var candidate = base
        var index = 2
        while (candidate in existing) {
            val suffix = " ($index)"
            candidate = base.take(40 - suffix.length) + suffix
            index++
        }
        return candidate
    }

    private companion object {
        val CollectionsKey = stringPreferencesKey("collections")
    }
}
