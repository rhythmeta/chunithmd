package org.rhythmeta.chunithmd.ui.catalog

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.rhythmeta.chunithmd.shared.CatalogFilters
import org.rhythmeta.chunithmd.shared.CatalogSort
import org.rhythmeta.chunithmd.shared.isWorldsEndCategory

private val Context.catalogPreferencesDataStore by preferencesDataStore(name = "catalog_preferences")

data class CatalogPreferences(
    val sort: CatalogSort = CatalogSort.Default,
    val ascending: Boolean = true,
    val filters: CatalogFilters = CatalogFilters(),
)

class CatalogPreferencesRepository(private val context: Context) {
    val preferences: Flow<CatalogPreferences> = context.catalogPreferencesDataStore.data.map { values ->
        CatalogPreferences(
            sort = values[SortKey]
                ?.let { stored -> CatalogSort.entries.firstOrNull { it.name == stored } }
                ?: CatalogSort.Default,
            ascending = values[AscendingKey] ?: true,
            filters = CatalogFilters(
                categories = values[CategoriesKey].orEmpty().filterNot(::isWorldsEndCategory).toSet(),
                versions = values[VersionsKey].orEmpty(),
                difficulties = values[DifficultiesKey].orEmpty(),
                types = values[TypesKey].orEmpty(),
                playableOnly = values[PlayableOnlyKey] ?: false,
            ),
        )
    }

    suspend fun setSort(sort: CatalogSort) {
        context.catalogPreferencesDataStore.edit { values -> values[SortKey] = sort.name }
    }

    suspend fun setAscending(ascending: Boolean) {
        context.catalogPreferencesDataStore.edit { values -> values[AscendingKey] = ascending }
    }

    suspend fun setFilters(filters: CatalogFilters) {
        context.catalogPreferencesDataStore.edit { values ->
            values[CategoriesKey] = filters.categories
            values[VersionsKey] = filters.versions
            values[DifficultiesKey] = filters.difficulties
            values[TypesKey] = filters.types
            values[PlayableOnlyKey] = filters.playableOnly
        }
    }

    private companion object {
        val SortKey = stringPreferencesKey("sort")
        val AscendingKey = booleanPreferencesKey("ascending")
        val CategoriesKey = stringSetPreferencesKey("categories")
        val VersionsKey = stringSetPreferencesKey("versions")
        val DifficultiesKey = stringSetPreferencesKey("difficulties")
        val TypesKey = stringSetPreferencesKey("types")
        val PlayableOnlyKey = booleanPreferencesKey("playable_only")
    }
}
