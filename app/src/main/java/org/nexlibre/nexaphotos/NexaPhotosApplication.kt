package org.nexlibre.nexaphotos

import android.app.Application
import android.content.Context
import org.nexlibre.nexaphotos.data.local.NexaDatabase
import org.nexlibre.nexaphotos.data.local.RoomFavoriteRepository
import org.nexlibre.nexaphotos.data.media.MediaStoreLiveSource
import org.nexlibre.nexaphotos.data.media.MediaStoreMediaRepository
import org.nexlibre.nexaphotos.data.media.MediaStoreSource
import org.nexlibre.nexaphotos.data.permissions.AndroidMediaPermissionChecker
import org.nexlibre.nexaphotos.data.permissions.MediaPermissionChecker
import org.nexlibre.nexaphotos.data.preferences.SettingsDataStore
import org.nexlibre.nexaphotos.domain.repository.FavoriteRepository
import org.nexlibre.nexaphotos.domain.repository.MediaRepository

/**
 * Contenedor manual de dependencias. La app no usa frameworks de inyección para mantener el APK
 * pequeño y el arranque predecible.
 */
class AppContainer(context: Context) {
    private val database: NexaDatabase = NexaDatabase.build(context)

    val settingsDataStore: SettingsDataStore = SettingsDataStore(context)
    val favoriteRepository: FavoriteRepository = RoomFavoriteRepository(database.favoriteDao())

    val mediaStoreSource: MediaStoreSource = MediaStoreLiveSource(context)
    val mediaRepository: MediaRepository = MediaStoreMediaRepository(mediaStoreSource, favoriteRepository)
    val mediaPermissionChecker: MediaPermissionChecker = AndroidMediaPermissionChecker(context)
}

class NexaPhotosApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
