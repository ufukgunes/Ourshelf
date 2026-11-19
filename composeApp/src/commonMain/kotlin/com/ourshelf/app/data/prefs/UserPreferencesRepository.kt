package com.ourshelf.app.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.io.IOException

data class UserPreferences(
    val onboardingCompleted: Boolean = false,
    val hapticsEnabled: Boolean = true,
    /** 3B poster eğimi ve ambiyans animasyonu. Sistem "animasyonları kaldır" ayarı ayrıca dikkate alınır. */
    val motionEffectsEnabled: Boolean = true,
)

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val preferences: Flow<UserPreferences> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            UserPreferences(
                onboardingCompleted = p[ONBOARDING_COMPLETED] ?: false,
                hapticsEnabled = p[HAPTICS_ENABLED] ?: true,
                motionEffectsEnabled = p[MOTION_EFFECTS_ENABLED] ?: true,
            )
        }

    suspend fun setOnboardingCompleted(value: Boolean = true) = set(ONBOARDING_COMPLETED, value)
    suspend fun setHapticsEnabled(value: Boolean) = set(HAPTICS_ENABLED, value)
    suspend fun setMotionEffectsEnabled(value: Boolean) = set(MOTION_EFFECTS_ENABLED, value)

    private suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { it[key] = value }
    }

    private companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val MOTION_EFFECTS_ENABLED = booleanPreferencesKey("motion_effects_enabled")
    }
}
