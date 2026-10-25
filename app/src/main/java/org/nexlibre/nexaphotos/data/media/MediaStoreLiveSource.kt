package org.nexlibre.nexaphotos.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nexlibre.nexaphotos.domain.model.MediaType

/**
 * Adaptador real sobre `ContentResolver` + MediaStore.
 *
 * - Toda la E/S se ejecuta en [ioDispatcher] (por defecto [Dispatchers.IO]): nunca en el hilo
 *   principal.
 * - Los cursores se cierran siempre (`use`).
 * - Paginación por cursor (`date, _id`), pidiendo `limit + 1` filas para saber si hay más.
 * - Solo lectura: no hay operaciones de escritura ni borrado.
 */
class MediaStoreLiveSource(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MediaStoreSource {

    private val resolver: ContentResolver = context.applicationContext.contentResolver
    private val collection: Uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

    override suspend fun fetchPage(query: MediaStoreQuery): RawMediaPage = withContext(ioDispatcher) {
        val selection = MediaStoreQueries.selection(query)
            ?: return@withContext RawMediaPage(emptyList(), null, false)
        val limit = query.limit.coerceIn(1, MAX_PAGE_SIZE)

        resolver.query(
            collection,
            MediaStoreQueries.projection,
            selection.clause,
            selection.args.toTypedArray(),
            MediaStoreQueries.sortOrder
        )?.use { cursor ->
            val rows = ArrayList<RawMediaRow>(limit + 1)
            while (rows.size <= limit && cursor.moveToNext()) {
                rows += cursor.toRawRow()
            }
            val hasMore = rows.size > limit
            val page = if (hasMore) rows.subList(0, limit).toList() else rows.toList()
            val nextCursor = if (hasMore) {
                page.lastOrNull()?.let { last ->
                    val id = MediaStoreQueries.uriId(last.contentUri) ?: 0L
                    MediaCursor(dateTakenMillis = last.dateTakenMillis, id = id).encode()
                }
            } else {
                null
            }
            RawMediaPage(rows = page, nextCursor = nextCursor, hasMore = hasMore)
        } ?: RawMediaPage(emptyList(), null, false)
    }

    override suspend fun fetchAlbums(query: MediaStoreQuery): List<RawAlbumRow> = withContext(ioDispatcher) {
        val selection = MediaStoreQueries.selection(query.copy(cursor = null))
            ?: return@withContext emptyList()

        resolver.query(
            collection,
            MediaStoreQueries.albumProjection,
            selection.clause,
            selection.args.toTypedArray(),
            "${MediaStoreQueries.effectiveDateExpression} DESC"
        )?.use { cursor ->
            val buckets = LinkedHashMap<String, Bucket>()
            val totalRows = cursor.count
            var scanned = 0
            while (cursor.moveToNext() && scanned < MAX_ALBUM_SCAN) {
                scanned++
                val bucketId = cursor.stringOrNull(MediaStoreQueries.COLUMN_BUCKET_ID) ?: continue
                val bucketName = cursor.stringOrNull(MediaStoreQueries.COLUMN_BUCKET_NAME)
                    ?.takeIf { it.isNotBlank() }
                    ?: DEFAULT_ALBUM_NAME
                val existing = buckets[bucketId]
                val coverUri = contentUriOf(cursor.longOrZero(MediaStoreQueries.COLUMN_ID))
                val latest = MediaStoreQueries.normalizedDateFromColumns(
                    dateTakenMillis = cursor.longOrNull(MediaStoreQueries.COLUMN_DATE_TAKEN),
                    dateAddedSeconds = cursor.longOrNull(MediaStoreQueries.COLUMN_DATE_ADDED)
                )
                if (existing == null) {
                    buckets[bucketId] = Bucket(bucketId, bucketName, coverUri, latest)
                } else {
                    existing.count++
                }
            }
            // Si el cursor tenía más filas que el tope de escaneo, los conteos son parciales:
            // se marca explícitamente en lugar de presentarlos como totales.
            val truncated = totalRows > MAX_ALBUM_SCAN || scanned >= MAX_ALBUM_SCAN
            buckets.values.map { bucket ->
                RawAlbumRow(
                    bucketId = bucket.id,
                    bucketName = bucket.name,
                    itemCount = bucket.count,
                    coverUri = bucket.coverUri,
                    latestDateMillis = bucket.latestDateMillis,
                    countIsExact = !truncated
                )
            }
        } ?: emptyList()
    }

    override suspend fun fetchByUris(uris: List<String>): List<RawMediaRow> = withContext(ioDispatcher) {
        val orderedIds = uris.mapNotNull { MediaStoreQueries.uriId(it) }.distinct()
        if (orderedIds.isEmpty()) return@withContext emptyList()

        val clause = "${MediaStoreQueries.COLUMN_IS_PENDING} = 0 AND " +
            "${MediaStoreQueries.COLUMN_ID} IN (${orderedIds.joinToString(separator = ",")})"

        val rows = resolver.query(
            collection,
            MediaStoreQueries.projection,
            clause,
            null,
            null
        )?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toRawRow())
            }
        }.orEmpty()

        val byId = rows.associateBy { MediaStoreQueries.uriId(it.contentUri) }
        orderedIds.mapNotNull { byId[it] }
    }

    override suspend fun counts(types: Set<MediaType>): Map<MediaType, Int> = withContext(ioDispatcher) {
        types.associateWith { type ->
            val mediaTypeValue = if (type == MediaType.VIDEO) {
                MediaStoreQueries.MEDIA_TYPE_VIDEO_VALUE
            } else {
                MediaStoreQueries.MEDIA_TYPE_IMAGE_VALUE
            }
            val clause = "${MediaStoreQueries.COLUMN_MEDIA_TYPE} = $mediaTypeValue AND " +
                "${MediaStoreQueries.COLUMN_IS_PENDING} = 0"
            resolver.query(collection, arrayOf(MediaStoreQueries.COLUMN_ID), clause, null, null)
                ?.use { it.count } ?: 0
        }
    }

    private fun contentUriOf(id: Long): String = ContentUris.withAppendedId(collection, id).toString()

    private fun Cursor.toRawRow(): RawMediaRow {
        val id = longOrZero(MediaStoreQueries.COLUMN_ID)
        val mediaTypeValue = intOrZero(MediaStoreQueries.COLUMN_MEDIA_TYPE)
        // Misma normalización que la expresión SQL de orden y cursor: datetaken nulo, cero o
        // negativo cae a date_added, y date_added nunca es negativo.
        val dateAddedMillis =
            (longOrNull(MediaStoreQueries.COLUMN_DATE_ADDED) ?: 0L).coerceAtLeast(0L) * 1000L
        val dateTaken = MediaStoreQueries.normalizedDateMillis(
            dateTakenMillis = longOrNull(MediaStoreQueries.COLUMN_DATE_TAKEN),
            dateAddedMillis = dateAddedMillis
        )

        return RawMediaRow(
            contentUri = contentUriOf(id),
            mediaType = MediaType.fromMediaStoreValue(mediaTypeValue),
            displayName = stringOrNull(MediaStoreQueries.COLUMN_DISPLAY_NAME),
            mimeType = stringOrNull(MediaStoreQueries.COLUMN_MIME_TYPE),
            sizeBytes = longOrZero(MediaStoreQueries.COLUMN_SIZE),
            dateTakenMillis = dateTaken,
            dateAddedMillis = dateAddedMillis,
            width = intOrZero(MediaStoreQueries.COLUMN_WIDTH),
            height = intOrZero(MediaStoreQueries.COLUMN_HEIGHT),
            durationMillis = longOrZero(MediaStoreQueries.COLUMN_DURATION),
            bucketId = stringOrNull(MediaStoreQueries.COLUMN_BUCKET_ID),
            bucketName = stringOrNull(MediaStoreQueries.COLUMN_BUCKET_NAME),
            relativePath = stringOrNull(MediaStoreQueries.COLUMN_RELATIVE_PATH)
        )
    }

    private fun Cursor.stringOrNull(column: String): String? {
        val index = getColumnIndex(column)
        return if (index < 0 || isNull(index)) null else getString(index)
    }

    private fun Cursor.longOrNull(column: String): Long? {
        val index = getColumnIndex(column)
        return if (index < 0 || isNull(index)) null else getLong(index)
    }

    private fun Cursor.longOrZero(column: String): Long = longOrNull(column) ?: 0L

    private fun Cursor.intOrZero(column: String): Int {
        val index = getColumnIndex(column)
        return if (index < 0 || isNull(index)) 0 else getInt(index)
    }

    private class Bucket(
        val id: String,
        val name: String,
        val coverUri: String?,
        val latestDateMillis: Long
    ) {
        var count: Int = 1
    }

    private companion object {
        const val MAX_PAGE_SIZE = 500
        const val MAX_ALBUM_SCAN = 100_000
        const val DEFAULT_ALBUM_NAME = "Sin nombre"
    }
}
