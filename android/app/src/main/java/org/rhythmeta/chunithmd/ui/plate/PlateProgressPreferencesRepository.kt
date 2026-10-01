package org.rhythmeta.chunithmd.ui.plate

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.rhythmeta.chunithmd.shared.PlateProgressCalculator
import org.rhythmeta.chunithmd.shared.PlateType

private val Context.plateProgressDataStore by preferencesDataStore(name = "plate_progress_preferences")

data class PlateProgressPreferences(
    val selectedVersion: String? = null,
    val plateType: PlateType = PlateType.Spirit,
    val difficulty: String? = null,
    val remainingOnly: Boolean = false,
)

class PlateProgressPreferencesRepository internal constructor(private val dataStore: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.plateProgressDataStore)

    val preferences: Flow<PlateProgressPreferences> = dataStore.data.map { values ->
        PlateProgressPreferences(
            selectedVersion = values[VersionKey],
            plateType = PlateType.entries.firstOrNull { it.name == values[PlateTypeKey] } ?: PlateType.Spirit,
            difficulty = values[DifficultyKey]?.takeIf { it in PlateProgressCalculator.difficulties },
            remainingOnly = values[RemainingOnlyKey] ?: false,
        )
    }

    suspend fun setVersion(version: String) {
        dataStore.edit { it[VersionKey] = version }
    }

    suspend fun setPlateType(plateType: PlateType) {
        dataStore.edit { it[PlateTypeKey] = plateType.name }
    }

    suspend fun setDifficulty(difficulty: String?) {
        dataStore.edit { values ->
            if (difficulty == null) values.remove(DifficultyKey)
            else values[DifficultyKey] = difficulty
        }
    }

    suspend fun setRemainingOnly(remainingOnly: Boolean) {
        dataStore.edit { it[RemainingOnlyKey] = remainingOnly }
    }

    private companion object {
        val VersionKey = stringPreferencesKey("selected_version")
        val PlateTypeKey = stringPreferencesKey("plate_type")
        val DifficultyKey = stringPreferencesKey("difficulty")
        val RemainingOnlyKey = booleanPreferencesKey("remaining_only")
    }
}
