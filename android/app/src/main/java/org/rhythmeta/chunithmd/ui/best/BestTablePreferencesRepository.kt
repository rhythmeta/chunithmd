package org.rhythmeta.chunithmd.ui.best

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.bestTablePreferencesDataStore by preferencesDataStore(name = "best_table_preferences")

data class BestTablePreferences(
    val bestCount: Int = 30,
    val newCount: Int = 20,
    val selectedVersion: String? = null,
)

class BestTablePreferencesRepository(private val context: Context) {
    val preferences: Flow<BestTablePreferences> = context.bestTablePreferencesDataStore.data.map { values ->
        BestTablePreferences(
            bestCount = values[BestCountKey]?.coerceIn(1, 99) ?: 30,
            newCount = values[NewCountKey]?.coerceIn(1, 99) ?: 20,
            selectedVersion = values[SelectedVersionKey],
        )
    }

    suspend fun setCapacity(bestCount: Int, newCount: Int) {
        context.bestTablePreferencesDataStore.edit { values ->
            values[BestCountKey] = bestCount.coerceIn(1, 99)
            values[NewCountKey] = newCount.coerceIn(1, 99)
        }
    }

    suspend fun setVersion(version: String?) {
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
