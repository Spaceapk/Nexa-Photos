package org.nexlibre.nexaphotos.data.media

import org.nexlibre.nexaphotos.domain.model.MediaType

/**
 * Consulta dirigida al adaptador MediaStore.
 *
 * @param cursor cursor opaco devuelto por una consulta anterior ([RawMediaPage.nextCursor]).
 * @param albumId `bucket_id` de MediaStore, o `null` para toda la biblioteca.
 * @param search texto de búsqueda local (se aplica sobre nombre, carpeta y ruta).
 * @param onlyFavorites si es `true`, el adaptador restringe la consulta a las URI favoritas.
 */
data class MediaStoreQuery(
    val limit: Int,
    val cursor: String? = null,
    val types: Set<MediaType> = MediaType.ALL,
    val albumId: String? = null,
    val search: String? = null,
    val onlyFavorites: Boolean = false,
    val uris: List<String> = emptyList()
)

/** Fila cruda de MediaStore, antes de normalizarse al modelo de dominio. */
data class RawMediaRow(
    val contentUri: String,
    val mediaType: MediaType,
    val displayName: String?,
    val mimeType: String?,
    val sizeBytes: Long,
    val dateTakenMillis: Long,
    val dateAddedMillis: Long,
    val width: Int,
    val height: Int,
    val durationMillis: Long,
    val bucketId: String?,
    val bucketName: String?,
    val relativePath: String?
)

/** Fila cruda de álbum/carpeta. */
data class RawAlbumRow(
    val bucketId: String,
    val bucketName: String,
    val itemCount: Int,
    val coverUri: String?,
    val latestDateMillis: Long = 0L
)

/** Resultado crudo de una página: las filas y el cursor para continuar. */
data class RawMediaPage(
    val rows: List<RawMediaRow>,
    val nextCursor: String?,
    val hasMore: Boolean
)

/**
 * Frontera del acceso a MediaStore.
 *
 * Esta interfaz aísla la implementación de `ContentResolver` para poder sustituirla en pruebas
 * y para mantener el resto de la app sin dependencias directas del proveedor. Las implementaciones
 * deben ejecutar toda la E/S en [kotlinx.coroutines.Dispatchers.IO] y liberar los cursores.
 */
interface MediaStoreSource {

    /** Ejecuta una consulta paginada. */
    suspend fun fetchPage(query: MediaStoreQuery): RawMediaPage

    /** Álbumes/carpetas con conteo y portada. */
    suspend fun fetchAlbums(query: MediaStoreQuery): List<RawAlbumRow>

    /** Recupera filas concretas por URI de contenido, omitiendo las inaccesibles. */
    suspend fun fetchByUris(uris: List<String>): List<RawMediaRow>

    /** Conteos globales de la biblioteca. */
    suspend fun counts(types: Set<MediaType>): Map<MediaType, Int>
}
