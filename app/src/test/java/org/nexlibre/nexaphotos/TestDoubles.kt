package org.nexlibre.nexaphotos

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.nexlibre.nexaphotos.data.media.MediaStoreSource
import org.nexlibre.nexaphotos.data.media.MediaStoreQuery
import org.nexlibre.nexaphotos.data.media.RawAlbumRow
import org.nexlibre.nexaphotos.data.media.RawMediaPage
import org.nexlibre.nexaphotos.data.media.RawMediaRow
import org.nexlibre.nexaphotos.data.media.MediaCursor
import org.nexlibre.nexaphotos.data.media.MediaStoreQueries
import org.nexlibre.nexaphotos.data.permissions.MediaAccessState
import org.nexlibre.nexaphotos.data.permissions.MediaPermissionChecker
import org.nexlibre.nexaphotos.data.permissions.MediaTypeAccess
import org.nexlibre.nexaphotos.domain.model.MediaItem
import org.nexlibre.nexaphotos.domain.model.MediaType
import org.nexlibre.nexaphotos.domain.repository.FavoriteRepository

private const val CONTENT_PREFIX = "content://media/external/images/media/"

fun rawMediaRow(
    id: Long,
    type: MediaType = MediaType.IMAGE,
    dateTakenMillis: Long = 1_700_000_000_000L + id,
    name: String = "foto-$id",
    bucketId: String? = "camera",
    bucketName: String = "Cámara",
    durationMillis: Long = 0L
): RawMediaRow = RawMediaRow(
    contentUri = CONTENT_PREFIX + id,
    mediaType = type,
    displayName = name,
    mimeType = if (type == MediaType.VIDEO) "video/mp4" else "image/jpeg",
    sizeBytes = 1_024L * id,
    dateTakenMillis = dateTakenMillis,
    dateAddedMillis = dateTakenMillis,
    width = 1080,
    height = 1920,
    durationMillis = durationMillis,
    bucketId = bucketId,
    bucketName = bucketName,
    relativePath = "DCIM/Cámara/"
)

fun mediaItem(
    id: Long,
    type: MediaType = MediaType.IMAGE,
    isFavorite: Boolean = false,
    dateTakenMillis: Long = 1_700_000_000_000L + id
): MediaItem = MediaItem(
    uri = CONTENT_PREFIX + id,
    name = "foto-$id",
    mimeType = if (type == MediaType.VIDEO) "video/mp4" else "image/jpeg",
    type = type,
    sizeBytes = 1_024L * id,
    dateTakenMillis = dateTakenMillis,
    dateAddedMillis = dateTakenMillis,
    width = 1080,
    height = 1920,
    durationMillis = if (type == MediaType.VIDEO) 65_000L else 0L,
    bucketId = "camera",
    bucketName = "Cámara",
    relativePath = "DCIM/Cámara/",
    isFavorite = isFavorite
)

class FakeMediaStoreSource(
    private val rows: List<RawMediaRow> = emptyList(),
    private val albumRows: List<RawAlbumRow> = emptyList(),
    private val counts: Map<MediaType, Int> = emptyMap()
) : MediaStoreSource {

    var pageCalls: Int = 0
        private set
    var albumCalls: Int = 0
        private set
    var lastQuery: MediaStoreQuery? = null
        private set

    override suspend fun fetchPage(query: MediaStoreQuery): RawMediaPage {
        pageCalls++
        lastQuery = query
        val filtered = rows.filter { row ->
            row.mediaType in query.types &&
                (query.albumId == null || row.bucketId == query.albumId) &&
                (query.search.isNullOrBlank() || row.displayName.orEmpty().contains(query.search, true)) &&
                (query.uris.isEmpty() || row.contentUri in query.uris)
        }.sortedWith(MediaStoreQueries.chronologicalDescending)

        // Paginación por keyset real (fecha + _id), igual que la consulta de producción.
        val cursor = MediaCursor.decode(query.cursor)
        val afterCursor = if (cursor == null) {
            filtered
        } else {
            filtered.filter { row ->
                MediaStoreQueries.isStrictlyAfterCursor(
                    cursor = cursor,
                    dateMillis = row.dateTakenMillis,
                    id = MediaStoreQueries.uriId(row.contentUri) ?: 0L
                )
            }
        }
        val page = afterCursor.take(query.limit)
        val hasMore = afterCursor.size > page.size
        val nextCursor = if (hasMore) {
            page.last().let { last ->
                MediaCursor(
                    dateTakenMillis = last.dateTakenMillis,
                    id = MediaStoreQueries.uriId(last.contentUri) ?: 0L
                ).encode()
            }
        } else {
            null
        }
        return RawMediaPage(rows = page, nextCursor = nextCursor, hasMore = hasMore)
    }

    override suspend fun fetchAlbums(query: MediaStoreQuery): List<RawAlbumRow> {
        albumCalls++
        return albumRows
    }

    override suspend fun fetchByUris(uris: List<String>): List<RawMediaRow> {
        val requested = uris.toSet()
        return rows.filter { it.contentUri in requested }
    }

    override suspend fun counts(types: Set<MediaType>): Map<MediaType, Int> =
        counts.filterKeys { it in types }

}

class FakeFavoriteRepository(initial: Set<String> = emptySet()) : FavoriteRepository {

    private val state = MutableStateFlow(initial)
    override val favoriteUris: Flow<Set<String>> = state
    val setCalls: MutableList<Pair<String, Boolean>> = mutableListOf()

    override suspend fun setFavorite(uri: String, favorite: Boolean) {
        setCalls += uri to favorite
        state.value = if (favorite) state.value + uri else state.value - uri
    }

    fun emit(uris: Set<String>) {
        state.value = uris
    }
}

class FakePermissionChecker(
    override val sdkInt: Int = 34,
    var currentState: MediaAccessState = MediaAccessState(MediaTypeAccess.FULL, MediaTypeAccess.FULL)
) : MediaPermissionChecker {
    override fun accessState(): MediaAccessState = currentState

    fun setTypes(images: MediaTypeAccess, videos: MediaTypeAccess) {
        currentState = MediaAccessState(images = images, videos = videos)
    }

    companion object {
        fun imagesOnly(sdkInt: Int = 33) =
            FakePermissionChecker(sdkInt, MediaAccessState(MediaTypeAccess.FULL, MediaTypeAccess.NONE))

        fun videoOnly(sdkInt: Int = 33) =
            FakePermissionChecker(sdkInt, MediaAccessState(MediaTypeAccess.NONE, MediaTypeAccess.FULL))

        fun partialBoth(sdkInt: Int = 34) =
            FakePermissionChecker(sdkInt, MediaAccessState(MediaTypeAccess.PARTIAL, MediaTypeAccess.PARTIAL))

        fun none(sdkInt: Int = 34) =
            FakePermissionChecker(sdkInt, MediaAccessState())
    }
}
