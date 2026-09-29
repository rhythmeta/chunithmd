package org.rhythmeta.chunithmd.shared

import kotlinx.coroutines.flow.Flow

data class CatalogPreferences(
    val sort: CatalogSort = CatalogSort.Default,
    val ascending: Boolean = true,
    val filters: CatalogFilters = CatalogFilters(),
)

interface CatalogPreferencesStore {
    val preferences: Flow<CatalogPreferences>
    suspend fun setSort(sort: CatalogSort)
    suspend fun setAscending(ascending: Boolean)
    suspend fun setFilters(filters: CatalogFilters)
}

interface FavoriteSongStore {
    val favoriteSongIds: Flow<Set<String>>
    suspend fun setFavorite(songId: String, favorite: Boolean)
}
