package org.nexlibre.nexaphotos

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.nexlibre.nexaphotos.data.media.MediaStoreSource
import org.nexlibre.nexaphotos.data.media.MediaStoreQuery
import org.nexlibre.nexaphotos.data.media.RawAlbumRow
import org.nexlibre.nexaphotos.data.media.RawMediaPage
import org.nexlibre.nexaphotos.data.media.RawMediaRow
import org.nexlibre.nexaphotos.data.permissions.MediaAccess
import org.nexlibre.nexaphotos.data.permissions.MediaPermissionChecker
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
        }.sortedByDescending { it.dateTakenMillis }

        val offset = query.cursor?.removePrefix(CURSOR_PREFIX)?.toIntOrNull() ?: 0
        val page = filtered.drop(offset).take(query.limit)
        val consumed = offset + page.size
        val hasMore = consumed < filtered.size
        return RawMediaPage(
            rows = page,
            nextCursor = if (hasMore) "$CURSOR_PREFIX$consumed" else null,
            hasMore = hasMore
        )
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

    companion object {
        const val CURSOR_PREFIX = "offset:"
    }
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
    var currentAccess: MediaAccess = MediaAccess.FULL,
    var currentTypes: Set<MediaType> = MediaType.ALL
) : MediaPermissionChecker {
    override fun access(): MediaAccess = currentAccess
    override fun readableTypes(): Set<MediaType> = currentTypes
}
