package org.rhythmeta.chunithmd.ui.best

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.rhythmeta.chunithmd.shared.BestTablePreferences
import org.rhythmeta.chunithmd.shared.BestTablePreferencesStore

internal val Context.bestTablePreferencesDataStore by preferencesDataStore(name = "best_table_preferences")

class BestTablePreferencesRepository(private val context: Context) : BestTablePreferencesStore {
    override val preferences: Flow<BestTablePreferences> = context.bestTablePreferencesDataStore.data.map { values ->
        BestTablePreferences(
            bestCount = values[BestCountKey] ?: BestTablePreferences.DEFAULT_BEST_COUNT,
            newCount = values[NewCountKey] ?: BestTablePreferences.DEFAULT_NEW_COUNT,
            selectedVersion = values[SelectedVersionKey],
        ).normalized()
    }

    override suspend fun setCapacity(bestCount: Int, newCount: Int) {
        context.bestTablePreferencesDataStore.edit { values ->
            values[BestCountKey] = bestCount.coerceIn(BestTablePreferences.MIN_COUNT, BestTablePreferences.MAX_COUNT)
            values[NewCountKey] = newCount.coerceIn(BestTablePreferences.MIN_COUNT, BestTablePreferences.MAX_COUNT)
        }
    }

    override suspend fun setVersion(version: String?) {
        context.bestTablePreferencesDataStore.edit { values ->
            if (version.isNullOrBlank()) values.remove(SelectedVersionKey)
            else values[SelectedVersionKey] = version
        }
    }

    private companion object {
        val BestCountKey = intPreferencesKey("best_count")
        val NewCountKey = intPreferencesKey("new_count")
        val SelectedVersionKey = stringPreferencesKey("selected_version")
    }
}
