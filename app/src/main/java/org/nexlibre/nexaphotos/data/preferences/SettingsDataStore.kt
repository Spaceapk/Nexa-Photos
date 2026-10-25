package org.nexlibre.nexaphotos.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.nexlibre.nexaphotos.domain.model.AppearanceMode
import org.nexlibre.nexaphotos.domain.model.UiStyle
import org.nexlibre.nexaphotos.domain.model.UserSettings

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "nexa_photos_settings")

/** Persistencia de apariencia: solo preferencias locales, sin red ni cuentas. */
class SettingsDataStore(private val context: Context) {

    private object Keys {
        val UI_STYLE = stringPreferencesKey("ui_style")
        val APPEARANCE = stringPreferencesKey("appearance_mode")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
    }

    val settings: Flow<UserSettings> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences.toUserSettings() }

    suspend fun setUiStyle(style: UiStyle) {
        context.settingsDataStore.edit { it[Keys.UI_STYLE] = style.name }
    }

    suspend fun setAppearanceMode(mode: AppearanceMode) {
        context.settingsDataStore.edit { it[Keys.APPEARANCE] = mode.name }
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DYNAMIC_COLORS] = enabled }
    }

    private fun Preferences.toUserSettings(): UserSettings = UserSettings(
        uiStyle = this[Keys.UI_STYLE].toUiStyle(),
        appearanceMode = this[Keys.APPEARANCE].toAppearanceMode(),
        useDynamicColors = this[Keys.DYNAMIC_COLORS] ?: false
    )
}
