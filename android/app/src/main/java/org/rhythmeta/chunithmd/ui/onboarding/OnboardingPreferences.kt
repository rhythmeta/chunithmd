package org.rhythmeta.chunithmd.ui.onboarding

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding")

class OnboardingPreferences(private val context: Context) {
    private val completedKey = booleanPreferencesKey("completed")
    val completed = context.onboardingDataStore.data.map { it[completedKey] ?: false }

    suspend fun complete() {
        context.onboardingDataStore.edit { it[completedKey] = true }
    }
}
