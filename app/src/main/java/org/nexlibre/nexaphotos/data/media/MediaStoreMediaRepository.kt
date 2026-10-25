package org.nexlibre.nexaphotos.data.media

import kotlinx.coroutines.flow.first
import org.nexlibre.nexaphotos.domain.model.LibraryStats
import org.nexlibre.nexaphotos.domain.model.MediaAlbum
import org.nexlibre.nexaphotos.domain.model.MediaFilter
import org.nexlibre.nexaphotos.domain.model.MediaItem
import org.nexlibre.nexaphotos.domain.model.MediaPage
import org.nexlibre.nexaphotos.domain.model.MediaType
import org.nexlibre.nexaphotos.domain.model.PageRequest
import org.nexlibre.nexaphotos.domain.repository.FavoriteRepository
import org.nexlibre.nexaphotos.domain.repository.MediaRepository

/**
 * Implementación de [MediaRepository] que combina la biblioteca de MediaStore con los favoritos
 * persistidos en Room.
 *
 * La favoritabilidad se resuelve leyendo el Flow de favoritos una vez por consulta y marcando los
 * elementos correspondientes: así el dominio nunca necesita más de una consulta por página.
 */
class MediaStoreMediaRepository(
    private val source: MediaStoreSource,
    private val favorites: FavoriteRepository
) : MediaRepository {

    override suspend fun page(filter: MediaFilter, request: PageRequest): MediaPage {
        if (filter.types.isEmpty()) return MediaPage.Empty

        val favoriteUris = favorites.favoriteUris.first()
        if (filter.onlyFavorites && favoriteUris.isEmpty()) return MediaPage.Empty

        val query = MediaStoreQuery(
            limit = request.limit,
            cursor = request.cursor,
            types = filter.types,
            albumId = filter.albumId,
            onlyFavorites = filter.onlyFavorites,
            uris = if (filter.onlyFavorites) favoriteUris.toList() else emptyList()
        )

        val page = source.fetchPage(query)
        return MediaPage(
            items = page.rows.map { it.toDomain(isFavorite = it.contentUri in favoriteUris) },
            nextCursor = page.nextCursor,
            hasMore = page.hasMore
        )
    }

    override suspend fun albums(filter: MediaFilter): List<MediaAlbum> {
        if (filter.types.isEmpty()) return emptyList()
        val rows = source.fetchAlbums(MediaStoreQuery(limit = 1, types = filter.types))
        return rows
            .map { row ->
                MediaAlbum(
                    id = row.bucketId,
                    name = row.bucketName.ifBlank { DEFAULT_ALBUM_NAME },
                    itemCount = row.itemCount,
                    coverUri = row.coverUri
                ) to row.latestDateMillis
            }
            .sortedWith(
                compareByDescending<Pair<MediaAlbum, Long>> { it.second }
                    .thenBy { it.first.name.lowercase() }
            )
            .map { it.first }
    }

    override suspend fun search(query: String, filter: MediaFilter, limit: Int): List<MediaItem> {
        val term = query.trim()
        if (term.isEmpty() || filter.types.isEmpty()) return emptyList()

        val favoriteUris = favorites.favoriteUris.first()
        val page = source.fetchPage(
            MediaStoreQuery(
                limit = limit.coerceIn(1, MAX_SEARCH_RESULTS),
                types = filter.types,
                albumId = filter.albumId,
                search = term,
                onlyFavorites = filter.onlyFavorites,
                uris = if (filter.onlyFavorites) favoriteUris.toList() else emptyList()
            )
        )
        return page.rows.map { it.toDomain(isFavorite = it.contentUri in favoriteUris) }
    }

    override suspend fun item(uri: String): MediaItem? {
        val normalized = MediaUris.normalize(uri)
        if (normalized.isEmpty()) return null
        val row = source.fetchByUris(listOf(normalized)).firstOrNull() ?: return null
        val favoriteUris = favorites.favoriteUris.first()
        return row.toDomain(isFavorite = row.contentUri in favoriteUris)
    }

    override suspend fun items(uris: List<String>): List<MediaItem> {
        val normalized = uris.map { MediaUris.normalize(it) }.filter { it.isNotEmpty() }.distinct()
        if (normalized.isEmpty()) return emptyList()
        val favoriteUris = favorites.favoriteUris.first()
        return source.fetchByUris(normalized).map { it.toDomain(isFavorite = it.contentUri in favoriteUris) }
    }

    override suspend fun stats(): LibraryStats {
        val counts = source.counts(MediaType.ALL)
        val favoriteCount = favorites.favoriteUris.first().size
        val images = counts[MediaType.IMAGE] ?: 0
        val videos = counts[MediaType.VIDEO] ?: 0
        return LibraryStats(
            totalItems = images + videos,
            imageCount = images,
            videoCount = videos,
            favoriteCount = favoriteCount
        )
    }

    private fun RawMediaRow.toDomain(isFavorite: Boolean): MediaItem = MediaItem(
        uri = contentUri,
        name = displayName ?: DEFAULT_ITEM_NAME,
        mimeType = mimeType,
        type = mediaType,
        sizeBytes = sizeBytes,
        dateTakenMillis = dateTakenMillis,
        dateAddedMillis = dateAddedMillis,
        width = width,
        height = height,
        durationMillis = durationMillis,
        bucketId = bucketId,
        bucketName = bucketName,
        relativePath = relativePath,
        isFavorite = isFavorite
    )

    private companion object {
        const val MAX_SEARCH_RESULTS = 500
        const val DEFAULT_ALBUM_NAME = "Sin carpeta"
        const val DEFAULT_ITEM_NAME = "Sin nombre"
    }
}
