package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "doom93_prefs")

data class GameSettings(
    val lookSensitivity: Float = 1.0f,
    val sfxVolume: Float = 0.85f,
    val showMinimap: Boolean = true,
    val showCrosshair: Boolean = true,
    val showFps: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val bestTimesSec: Map<Int, Int> = emptyMap(),
    val bestKillsPct: Map<Int, Int> = emptyMap()
)

class GameSettingsRepository(private val context: Context) {

    private object Keys {
        val LOOK_SENSITIVITY = floatPreferencesKey("look_sensitivity")
        val SFX_VOLUME = floatPreferencesKey("sfx_volume")
        val SHOW_MINIMAP = booleanPreferencesKey("show_minimap")
        val SHOW_CROSSHAIR = booleanPreferencesKey("show_crosshair")
        val SHOW_FPS = booleanPreferencesKey("show_fps")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        fun bestTime(levelId: Int) = intPreferencesKey("best_time_$levelId")
        fun bestKills(levelId: Int) = intPreferencesKey("best_kills_$levelId")
    }

    val settingsFlow: Flow<GameSettings> = context.dataStore.data.map { prefs ->
        val bestTimes = mutableMapOf<Int, Int>()
        val bestKills = mutableMapOf<Int, Int>()
        for (levelId in 1..3) {
            prefs[Keys.bestTime(levelId)]?.let { bestTimes[levelId] = it }
            prefs[Keys.bestKills(levelId)]?.let { bestKills[levelId] = it }
        }
        GameSettings(
            lookSensitivity = prefs[Keys.LOOK_SENSITIVITY] ?: 1.0f,
            sfxVolume = prefs[Keys.SFX_VOLUME] ?: 0.85f,
            showMinimap = prefs[Keys.SHOW_MINIMAP] ?: true,
            showCrosshair = prefs[Keys.SHOW_CROSSHAIR] ?: true,
            showFps = prefs[Keys.SHOW_FPS] ?: false,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            bestTimesSec = bestTimes,
            bestKillsPct = bestKills
        )
    }

    suspend fun updateSensitivity(value: Float) {
        context.dataStore.edit { it[Keys.LOOK_SENSITIVITY] = value.coerceIn(0.4f, 2.2f) }
    }

    suspend fun updateSfxVolume(value: Float) {
        context.dataStore.edit { it[Keys.SFX_VOLUME] = value.coerceIn(0f, 1f) }
    }

    suspend fun setShowMinimap(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_MINIMAP] = enabled }
    }

    suspend fun setShowCrosshair(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_CROSSHAIR] = enabled }
    }

    suspend fun setShowFps(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_FPS] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun recordLevelClear(levelId: Int, elapsedSec: Int, killsPct: Int) {
        context.dataStore.edit { prefs ->
            val prevTime = prefs[Keys.bestTime(levelId)]
            if (prevTime == null || elapsedSec < prevTime) {
                prefs[Keys.bestTime(levelId)] = elapsedSec
            }
            val prevKills = prefs[Keys.bestKills(levelId)] ?: 0
            if (killsPct > prevKills) {
                prefs[Keys.bestKills(levelId)] = killsPct
            }
        }
    }
}
