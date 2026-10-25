package org.nexlibre.nexaphotos.domain.repository

import kotlinx.coroutines.flow.Flow
import org.nexlibre.nexaphotos.domain.model.LibraryStats
import org.nexlibre.nexaphotos.domain.model.MediaAlbum
import org.nexlibre.nexaphotos.domain.model.MediaFilter
import org.nexlibre.nexaphotos.domain.model.MediaItem
import org.nexlibre.nexaphotos.domain.model.MediaPage
import org.nexlibre.nexaphotos.domain.model.PageRequest

/**
 * Contrato de la biblioteca multimedia local.
 *
 * Todas las funciones son `suspend` y deben ejecutarse fuera del hilo principal: la
 * implementación real consulta el proveedor MediaStore, que es E/S bloqueante.
 * La biblioteca es de solo lectura: este contrato no expone borrado ni modificación.
 */
interface MediaRepository {

    /** Página cronológica (descendente) de la biblioteca según [filter]. */
    suspend fun page(filter: MediaFilter, request: PageRequest): MediaPage

    /** Álbumes/carpetas visibles para [filter], ordenados por fecha del contenido más reciente. */
    suspend fun albums(filter: MediaFilter = MediaFilter()): List<MediaAlbum>

    /** Búsqueda local por nombre, carpeta o ruta. Devuelve como máximo [limit] elementos. */
    suspend fun search(query: String, filter: MediaFilter = MediaFilter(), limit: Int = 200): List<MediaItem>

    /** Elemento individual por URI de contenido, o `null` si ya no existe o no hay permiso. */
    suspend fun item(uri: String): MediaItem?

    /** Varios elementos por URI, conservando solo los que siguen accesibles. */
    suspend fun items(uris: List<String>): List<MediaItem>

    /** Conteos de la biblioteca para encabezados y estados vacíos. */
    suspend fun stats(): LibraryStats
}

/**
 * Contrato de favoritos. Las URI no se alteran ni se recortan en la vista: el setter es
 * idempotente respecto al estado pedido (marcar dos veces un favorito deja un único registro).
 */
interface FavoriteRepository {

    /** Flow de URI favoritas; se emite de nuevo ante cada cambio. */
    val favoriteUris: Flow<Set<String>>

    /** Marca o desmarca un favorito. Idempotente. */
    suspend fun setFavorite(uri: String, favorite: Boolean)
}
