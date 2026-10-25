package org.nexlibre.nexaphotos.domain.model

/**
 * Tipo de elemento multimedia soportado por Nexa Photos.
 * La biblioteca es de solo lectura: nunca se modifica ni se elimina el original.
 */
enum class MediaType {
    IMAGE,
    VIDEO;

    companion object {
        /** Todos los tipos que la biblioteca muestra por defecto. */
        val ALL: Set<MediaType> = setOf(IMAGE, VIDEO)

        fun fromMediaStoreValue(value: Int): MediaType =
            if (value == 2) VIDEO else IMAGE // MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO == 2
    }
}

/**
 * Elemento multimedia normalizado de la biblioteca local.
 *
 * @param uri URI de contenido (`content://media/...`) serializado como texto; es la clave estable
 *   del elemento y el identificador usado por favoritos.
 * @param dateTakenMillis fecha de captura en milisegundos; si MediaStore no la informa se usa
 *   `dateAddedMillis`. Es el criterio cronológico principal de la cuadrícula.
 * @param durationMillis duración en milisegundos para videos; `0` para imágenes.
 */
data class MediaItem(
    val uri: String,
    val name: String,
    val mimeType: String?,
    val type: MediaType,
    val sizeBytes: Long,
    val dateTakenMillis: Long,
    val dateAddedMillis: Long,
    val width: Int,
    val height: Int,
    val durationMillis: Long = 0L,
    val bucketId: String? = null,
    val bucketName: String? = null,
    val relativePath: String? = null,
    val isFavorite: Boolean = false
) {
    val isVideo: Boolean get() = type == MediaType.VIDEO
}

/** Álbum o carpeta derivada del `bucket` de MediaStore. */
data class MediaAlbum(
    val id: String,
    val name: String,
    val itemCount: Int,
    val coverUri: String?
)

/** Conteo global de la biblioteca, útil para estados vacíos y encabezados. */
data class LibraryStats(
    val totalItems: Int = 0,
    val imageCount: Int = 0,
    val videoCount: Int = 0,
    val favoriteCount: Int = 0
)

/** Filtro de biblioteca aplicado a las consultas paginadas. */
data class MediaFilter(
    val types: Set<MediaType> = MediaType.ALL,
    val albumId: String? = null,
    val onlyFavorites: Boolean = false
)

/** Solicitud de página de la biblioteca. `cursor` es opaco y proviene de [MediaPage.nextCursor]. */
data class PageRequest(
    val limit: Int = DEFAULT_PAGE_SIZE,
    val cursor: String? = null
) {
    init {
        require(limit > 0) { "limit debe ser mayor que cero" }
    }

    companion object {
        const val DEFAULT_PAGE_SIZE: Int = 60
        val First: PageRequest = PageRequest()
    }
}

/**
 * Página de resultados de la biblioteca.
 *
 * @param nextCursor cursor para pedir la página siguiente o `null` si no hay más datos.
 */
data class MediaPage(
    val items: List<MediaItem>,
    val nextCursor: String?,
    val hasMore: Boolean
) {
    companion object {
        val Empty: MediaPage = MediaPage(items = emptyList(), nextCursor = null, hasMore = false)
    }
}
