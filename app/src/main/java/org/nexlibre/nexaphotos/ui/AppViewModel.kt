package org.nexlibre.nexaphotos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.nexlibre.nexaphotos.NexaPhotosApplication
import org.nexlibre.nexaphotos.data.preferences.SettingsDataStore
import org.nexlibre.nexaphotos.domain.model.AppearanceMode
import org.nexlibre.nexaphotos.domain.model.UiStyle
import org.nexlibre.nexaphotos.domain.model.UserSettings

/** Estado de apariencia compartido (Google Expressive / iOS Classic y claro/oscuro). */
class AppViewModel(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsDataStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettings()
    )

    fun setUiStyle(style: UiStyle) {
        viewModelScope.launch { settingsDataStore.setUiStyle(style) }
    }

    fun setAppearanceMode(mode: AppearanceMode) {
        viewModelScope.launch { settingsDataStore.setAppearanceMode(mode) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as NexaPhotosApplication
                AppViewModel(application.container.settingsDataStore)
            }
        }
    }
}
