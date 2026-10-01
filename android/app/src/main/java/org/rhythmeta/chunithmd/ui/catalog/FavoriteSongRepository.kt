package org.rhythmeta.chunithmd.ui.catalog

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.rhythmeta.chunithmd.shared.FavoriteSongStore

private val Context.favoriteSongsDataStore by preferencesDataStore(name = "favorite_songs")

class FavoriteSongRepository(private val context: Context) : FavoriteSongStore {
    override val favoriteSongIds: Flow<Set<String>> = context.favoriteSongsDataStore.data.map { values ->
        values[FavoriteSongIdsKey].orEmpty()
    }

    suspend fun replaceFavorites(ids: Set<String>) {
        context.favoriteSongsDataStore.edit { it[FavoriteSongIdsKey] = ids }
    }

    override suspend fun setFavorite(songId: String, favorite: Boolean) {
        context.favoriteSongsDataStore.edit { values ->
            val current = values[FavoriteSongIdsKey].orEmpty().toMutableSet()
            if (favorite) current += songId else current -= songId
            values[FavoriteSongIdsKey] = current
        }
    }

    private companion object {
        val FavoriteSongIdsKey = stringSetPreferencesKey("song_ids")
    }
}
