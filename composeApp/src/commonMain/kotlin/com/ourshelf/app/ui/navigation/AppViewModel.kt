package com.ourshelf.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ourshelf.app.data.prefs.UserPreferences
import com.ourshelf.app.data.prefs.UserPreferencesRepository
import com.ourshelf.app.data.repository.MediaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ekranlar arası işlemler ve uygulama geneli tercihler. Activity ömrü boyunca yaşar. */
class AppViewModel(
    private val mediaRepository: MediaRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    /** null = henüz okunmadı (onboarding'in yanlışlıkla bir an görünmesini önler). */
    val preferences: StateFlow<UserPreferences?> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun restoreItem(id: String) {
        viewModelScope.launch { mediaRepository.restore(id) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { preferencesRepository.setOnboardingCompleted() }
    }

    fun setHapticsEnabled(value: Boolean) {
        viewModelScope.launch { preferencesRepository.setHapticsEnabled(value) }
    }

    fun setMotionEffectsEnabled(value: Boolean) {
        viewModelScope.launch { preferencesRepository.setMotionEffectsEnabled(value) }
    }
}
