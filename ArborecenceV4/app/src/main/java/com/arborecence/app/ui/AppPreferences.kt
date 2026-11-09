package com.arborecence.app.ui

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("app_settings")

data class AccessibilitySettings(
    val lowStimulus: Boolean = true, // default safe for ND users
    val highContrast: Boolean = false,
    val reducedMotion: Boolean = true,
    val fontScale: Float = 1.0f,
    val noUnexpectedSounds: Boolean = true,
)

@Singleton
class AppPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private object K {
        val LOW = booleanPreferencesKey("low_stimulus")
        val CONTRAST = booleanPreferencesKey("high_contrast")
        val MOTION = booleanPreferencesKey("reduced_motion")
        val FONT = floatPreferencesKey("font_scale")
        val SOUNDS = booleanPreferencesKey("no_unexpected_sounds")
    }

    val settings: Flow<AccessibilitySettings> = context.dataStore.data.map { p ->
        AccessibilitySettings(
            lowStimulus = p[K.LOW] ?: true,
            highContrast = p[K.CONTRAST] ?: false,
            reducedMotion = p[K.MOTION] ?: true,
            fontScale = p[K.FONT] ?: 1.0f,
            noUnexpectedSounds = p[K.SOUNDS] ?: true,
        )
    }

    suspend fun setLowStimulus(v: Boolean) = context.dataStore.edit { it[K.LOW] = v }
    suspend fun setHighContrast(v: Boolean) = context.dataStore.edit { it[K.CONTRAST] = v }
    suspend fun setReducedMotion(v: Boolean) = context.dataStore.edit { it[K.MOTION] = v }
    suspend fun setFontScale(v: Float) = context.dataStore.edit { it[K.FONT] = v.coerceIn(0.85f, 1.5f) }
    suspend fun setNoUnexpectedSounds(v: Boolean) = context.dataStore.edit { it[K.SOUNDS] = v }
}
