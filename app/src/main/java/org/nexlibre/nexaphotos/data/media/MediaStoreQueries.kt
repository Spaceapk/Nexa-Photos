package org.nexlibre.nexaphotos.data.media

import org.nexlibre.nexaphotos.domain.model.MediaType

/**
 * Construcción pura de selecciones y orden para `ContentResolver.query` sobre MediaStore.
 *
 * Los nombres de columna son los documentados por `MediaStore.Files.FileColumns`; se declaran
 * aquí como texto para que la lógica sea verificable en pruebas de JVM. La fecha efectiva es
 * `IFNULL(datetaken, date_added * 1000)`: `date_added` está siempre presente, mientras que
 * `datetaken` puede ser nulo (por ejemplo en algunos videos), y así el orden cronológico y el
 * cursor de paginación son coherentes para todas las filas.
 */
object MediaStoreQueries {

    const val COLUMN_ID = "_id"
    const val COLUMN_MEDIA_TYPE = "media_type"
    const val COLUMN_DISPLAY_NAME = "_display_name"
    const val COLUMN_MIME_TYPE = "mime_type"
    const val COLUMN_SIZE = "_size"
    const val COLUMN_DATE_TAKEN = "datetaken"
    const val COLUMN_DATE_ADDED = "date_added"
    const val COLUMN_WIDTH = "width"
    const val COLUMN_HEIGHT = "height"
    const val COLUMN_DURATION = "duration"
    const val COLUMN_BUCKET_ID = "bucket_id"
    const val COLUMN_BUCKET_NAME = "bucket_display_name"
    const val COLUMN_RELATIVE_PATH = "relative_path"
    const val COLUMN_IS_PENDING = "is_pending"

    /** Valores de `media_type` en MediaStore. */
    const val MEDIA_TYPE_IMAGE_VALUE = 1
    const val MEDIA_TYPE_VIDEO_VALUE = 2

    const val ESCAPE_CHAR = "\\"

    /** Expresión SQL de fecha efectiva usada tanto para ordenar como para el cursor. */
    val effectiveDateExpression: String = "IFNULL($COLUMN_DATE_TAKEN, $COLUMN_DATE_ADDED * 1000)"

    /** Orden cronológico descendente, con `_id` como desempate estable. */
    val sortOrder: String = "$effectiveDateExpression DESC, $COLUMN_ID DESC"

    val projection: Array<String> = arrayOf(
        COLUMN_ID,
        COLUMN_MEDIA_TYPE,
        COLUMN_DISPLAY_NAME,
        COLUMN_MIME_TYPE,
        COLUMN_SIZE,
        COLUMN_DATE_TAKEN,
        COLUMN_DATE_ADDED,
        COLUMN_WIDTH,
        COLUMN_HEIGHT,
        COLUMN_DURATION,
        COLUMN_BUCKET_ID,
        COLUMN_BUCKET_NAME,
        COLUMN_RELATIVE_PATH
    )

    /** Proyección reducida para agregar álbumes en memoria. */
    val albumProjection: Array<String> = arrayOf(
        COLUMN_ID,
        COLUMN_DATE_TAKEN,
        COLUMN_DATE_ADDED,
        COLUMN_BUCKET_ID,
        COLUMN_BUCKET_NAME,
        COLUMN_MEDIA_TYPE
    )

    /** Cláusula de tipos: `media_type IN (1,2)`. */
    fun mediaTypeClause(types: Set<MediaType>): String {
        val values = types.map { if (it == MediaType.VIDEO) MEDIA_TYPE_VIDEO_VALUE else MEDIA_TYPE_IMAGE_VALUE }
            .distinct()
            .sorted()
        return "$COLUMN_MEDIA_TYPE IN (${values.joinToString(separator = ",")})"
    }

    /** Escapa comodines de `LIKE` para que la búsqueda sea literal. */
    fun escapeLike(term: String): String = term
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")

    /**
     * Selección completa para [query].
     *
     * @return `null` cuando no hay tipos consultables (sin permisos): el llamador debe devolver
     *   una página vacía sin llegar a MediaStore.
     */
    fun selection(query: MediaStoreQuery): MediaStoreSelection? {
        if (query.types.isEmpty()) return null
        val clauses = mutableListOf<String>()
        val args = mutableListOf<String>()

        clauses += mediaTypeClause(query.types)
        clauses += "$COLUMN_IS_PENDING = 0"

        query.albumId?.takeIf { it.isNotBlank() }?.let { albumId ->
            clauses += "$COLUMN_BUCKET_ID = ?"
            args += albumId
        }

        val search = query.search?.trim().orEmpty()
        if (search.isNotEmpty()) {
            val like = "%${escapeLike(search)}%"
            clauses += "($COLUMN_DISPLAY_NAME LIKE ? ESCAPE '\\'" +
                " OR $COLUMN_BUCKET_NAME LIKE ? ESCAPE '\\'" +
                " OR $COLUMN_RELATIVE_PATH LIKE ? ESCAPE '\\')"
            repeat(3) { args += like }
        }

        if (query.uris.isNotEmpty()) {
            val ids = query.uris.mapNotNull { uriId(it) }
            if (ids.isEmpty()) return MediaStoreSelection(clause = "0", args = emptyList())
            clauses += "$COLUMN_ID IN (${ids.joinToString(separator = ",")})"
        }

        MediaCursor.decode(query.cursor)?.let { cursor ->
            clauses += "($effectiveDateExpression < ? OR ($effectiveDateExpression = ? AND $COLUMN_ID < ?))"
            args += cursor.dateTakenMillis.toString()
            args += cursor.dateTakenMillis.toString()
            args += cursor.id.toString()
        }

        return MediaStoreSelection(clause = clauses.joinToString(" AND "), args = args)
    }

    /** Extrae el `_ID` de una URI de contenido de MediaStore, o `null` si no es válida. */
    fun uriId(uri: String): Long? {
        val trimmed = uri.trim()
        if (trimmed.isEmpty()) return null
        val lastSegment = trimmed.substringAfterLast('/').substringBefore('?')
        return lastSegment.toLongOrNull()
    }
}

/** Cláusula `WHERE` y sus argumentos, listos para `ContentResolver.query`. */
data class MediaStoreSelection(
    val clause: String,
    val args: List<String>
)
