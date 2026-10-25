package org.nexlibre.nexaphotos.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.nexlibre.nexaphotos.data.media.MediaUris
import org.nexlibre.nexaphotos.domain.repository.FavoriteRepository

/**
 * Implementación de [FavoriteRepository] sobre Room.
 *
 * @param nowMillis reloj inyectable para pruebas deterministas.
 */
class RoomFavoriteRepository(
    private val dao: FavoriteDao,
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) : FavoriteRepository {

    override val favoriteUris: Flow<Set<String>> =
        dao.observeFavoriteUris().map { uris -> uris.toSet() }

    override suspend fun setFavorite(uri: String, favorite: Boolean) {
        val normalized = MediaUris.normalize(uri)
        if (normalized.isEmpty()) return
        dao.setFavorite(normalized, favorite, nowMillis())
    }
}
