package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.RetroSoundEngine
import com.example.data.GameSettings
import com.example.data.GameSettingsRepository
import com.example.game.Difficulty
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GameSettingsRepository(application)
    val soundEngine = RetroSoundEngine(application)

    val settings: StateFlow<GameSettings> = repository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = GameSettings()
        )

    var selectedDifficultyId: Int = Difficulty.NORMAL.id
        private set

    fun selectDifficulty(id: Int) {
        selectedDifficultyId = id
    }

    fun updateSensitivity(value: Float) {
        viewModelScope.launch { repository.updateSensitivity(value) }
    }

    fun updateSfxVolume(value: Float) {
        soundEngine.setVolume(value)
        viewModelScope.launch { repository.updateSfxVolume(value) }
    }

    fun setShowMinimap(enabled: Boolean) {
        viewModelScope.launch { repository.setShowMinimap(enabled) }
    }

    fun setShowCrosshair(enabled: Boolean) {
        viewModelScope.launch { repository.setShowCrosshair(enabled) }
    }

    fun setShowFps(enabled: Boolean) {
        viewModelScope.launch { repository.setShowFps(enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setVibrationEnabled(enabled) }
    }

    fun recordLevelClear(levelId: Int, elapsedSec: Int, killsPct: Int) {
        viewModelScope.launch {
            repository.recordLevelClear(levelId, elapsedSec, killsPct)
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundEngine.release()
    }
}
