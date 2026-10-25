package org.nexlibre.nexaphotos.ui.media.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.nexlibre.nexaphotos.NexaPhotosApplication
import org.nexlibre.nexaphotos.data.permissions.MediaAccess
import org.nexlibre.nexaphotos.data.permissions.MediaAccessState
import org.nexlibre.nexaphotos.data.permissions.MediaPermissionChecker
import org.nexlibre.nexaphotos.data.permissions.MediaTypeAccess
import org.nexlibre.nexaphotos.domain.model.MediaAlbum
import org.nexlibre.nexaphotos.domain.model.MediaFilter
import org.nexlibre.nexaphotos.domain.model.MediaItem
import org.nexlibre.nexaphotos.domain.model.MediaType
import org.nexlibre.nexaphotos.domain.model.PageRequest
import org.nexlibre.nexaphotos.domain.repository.FavoriteRepository
import org.nexlibre.nexaphotos.domain.repository.MediaRepository

/** Sección principal de la galería. */
enum class GallerySection {
    TIMELINE,
    ALBUMS,
    FAVORITES
}

/** Elementos y posición con los que se abre el visor. */
data class ViewerTarget(
    val items: List<MediaItem>,
    val index: Int
)

/** Estado completo de la pantalla de galería. */
data class GalleryUiState(
    val access: MediaAccess = MediaAccess.NONE,
    /** Acceso por tipo: un tipo puede estar completo y el otro solo con elementos elegidos. */
    val typeAccess: MediaAccessState = MediaAccessState(),
    val readableTypes: Set<MediaType> = emptySet(),
    val section: GallerySection = GallerySection.TIMELINE,
    val items: List<MediaItem> = emptyList(),
    val albums: List<MediaAlbum> = emptyList(),
    val album: MediaAlbum? = null,
    val favoriteUris: Set<String> = emptySet(),
    val query: String = "",
    val searchResults: List<MediaItem> = emptyList(),
    val selection: SelectionState = SelectionState(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null,
    val viewer: ViewerTarget? = null
) {
    val isSearchActive: Boolean get() = query.isNotBlank()

    /** Algún tipo solo tiene acceso a los elementos elegidos por el usuario. */
    val hasPartialAccess: Boolean get() = typeAccess.hasPartialAccess

    /** Tipos sin permiso: se explican en la UI en lugar de ocultarse en silencio. */
    val missingTypes: Set<MediaType> get() = typeAccess.missingTypes

    /** `true` si falta algún tipo (aunque la biblioteca visible no esté vacía). */
    val hasMissingTypes: Boolean get() = missingTypes.isNotEmpty()

    val needsPermission: Boolean get() = readableTypes.isEmpty()
    val canLoadMore: Boolean get() = hasMore && !isLoading && !isLoadingMore && !isSearchActive
    val displayItems: List<MediaItem> get() = if (isSearchActive) searchResults else items
    val showAlbums: Boolean get() = section == GallerySection.ALBUMS && album == null
}

/**
 * Estado de la galería: paginación incremental, álbumes, búsqueda local, selección múltiple,
 * favoritos y apertura del visor. Toda la E/S ocurre en el repositorio (fuera del hilo principal).
 */
class GalleryViewModel(
    private val repository: MediaRepository,
    private val favoriteRepository: FavoriteRepository,
    private val permissionChecker: MediaPermissionChecker
) : ViewModel() {

    private val _state = MutableStateFlow(GalleryUiState())
    val state: StateFlow<GalleryUiState> = _state.asStateFlow()

    private var nextCursor: String? = null
    private val loadMutex = Mutex()
    private var loadJob: Job? = null
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            favoriteRepository.favoriteUris.collect { uris -> applyFavorites(uris) }
        }
        refreshPermissions()
    }

    /** Relee permisos del sistema (llamar al conceder, denegar o volver al primer plano). */
    fun refreshPermissions() {
        val typeAccess = permissionChecker.accessState()
        _state.update {
            it.copy(
                access = typeAccess.overall,
                typeAccess = typeAccess,
                readableTypes = typeAccess.readableTypes
            )
        }
        val types = typeAccess.readableTypes
        if (types.isEmpty()) {
            nextCursor = null
            _state.update {
                it.copy(
                    items = emptyList(),
                    albums = emptyList(),
                    searchResults = emptyList(),
                    hasMore = false,
                    isLoading = false,
                    isLoadingMore = false,
                    selection = SelectionState()
                )
            }
            return
        }
        reload()
    }

    fun setSection(section: GallerySection) {
        if (_state.value.section == section && _state.value.album == null) return
        _state.update {
            it.copy(
                section = section,
                album = null,
                selection = SelectionState(),
                query = "",
                searchResults = emptyList()
            )
        }
        reload()
    }

    fun openAlbum(album: MediaAlbum) {
        _state.update { it.copy(album = album, selection = SelectionState(), query = "", searchResults = emptyList()) }
        reload()
    }

    fun closeAlbum() {
        if (_state.value.album == null) return
        _state.update { it.copy(album = null, selection = SelectionState()) }
        reload()
    }

    fun setQuery(text: String) {
        _state.update { it.copy(query = text) }
        searchJob?.cancel()
        val term = text.trim()
        if (term.isEmpty()) {
            _state.update { it.copy(searchResults = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            try {
                val results = repository.search(term, currentFilter(), SEARCH_LIMIT)
                _state.update { current ->
                    if (current.query.trim() == term) {
                        current.copy(searchResults = results.map { it.withFavorite(current.favoriteUris) })
                    } else {
                        current
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(error = error.message ?: SEARCH_ERROR) }
            }
        }
    }

    fun reload() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            loadMutex.withLock {
                if (_state.value.readableTypes.isEmpty()) return@withLock
                nextCursor = null
                _state.update { it.copy(isLoading = true, isLoadingMore = false, error = null) }
                try {
                    val favorites = _state.value.favoriteUris
                    val page = repository.page(currentFilter(), PageRequest(limit = PAGE_SIZE))
                    nextCursor = page.nextCursor
                    _state.update { current ->
                        current.copy(
                            items = page.items.map { it.withFavorite(favorites) },
                            hasMore = page.hasMore,
                            isLoading = false,
                            isLoadingMore = false,
                            selection = current.selection.retainOnly(page.items.map { item -> item.uri })
                        )
                    }
                    if (_state.value.showAlbums) loadAlbums()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    _state.update {
                        it.copy(isLoading = false, isLoadingMore = false, error = error.message ?: LOAD_ERROR)
                    }
                }
            }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (!current.canLoadMore) return
        val cursor = nextCursor ?: return
        viewModelScope.launch {
            loadMutex.withLock {
                if (_state.value.isLoadingMore) return@withLock
                _state.update { it.copy(isLoadingMore = true) }
                try {
                    val favorites = _state.value.favoriteUris
                    val page = repository.page(
                        currentFilter(),
                        PageRequest(limit = PAGE_SIZE, cursor = cursor)
                    )
                    nextCursor = page.nextCursor
                    _state.update { state ->
                        val known = state.items.mapTo(HashSet()) { item -> item.uri }
                        val fresh = page.items.filterNot { it.uri in known }.map { it.withFavorite(favorites) }
                        state.copy(
                            items = state.items + fresh,
                            hasMore = page.hasMore,
                            isLoadingMore = false
                        )
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    _state.update {
                        it.copy(isLoadingMore = false, error = error.message ?: LOAD_ERROR)
                    }
                }
            }
        }
    }

    fun toggleSelection(uri: String) {
        _state.update { it.copy(selection = it.selection.toggle(uri)) }
    }

    fun selectAllVisible() {
        _state.update { current ->
            current.copy(selection = current.selection.add(current.displayItems.map { it.uri }))
        }
    }

    fun clearSelection() {
        _state.update { it.copy(selection = SelectionState()) }
    }

    fun toggleFavorite(item: MediaItem) {
        val isFavorite = item.uri in _state.value.favoriteUris
        viewModelScope.launch {
            try {
                favoriteRepository.setFavorite(item.uri, !isFavorite)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(error = error.message ?: FAVORITE_ERROR) }
            }
        }
    }

    fun openViewer(index: Int) {
        val visible = _state.value.displayItems
        if (index !in visible.indices) return
        _state.update { it.copy(viewer = ViewerTarget(items = visible, index = index)) }
    }

    fun closeViewer() {
        _state.update { it.copy(viewer = null) }
    }

    /** Permisos que la UI debe solicitar en la versión de Android actual. */
    fun requiredPermissions(): Array<String> = permissionChecker.requiredPermissions()

    /**
     * Permisos que faltan ahora mismo: solo los tipos denegados o parciales, incluido
     * `READ_MEDIA_VISUAL_USER_SELECTED` en Android 14+ para poder elegir más elementos.
     */
    fun missingPermissions(): Array<String> = permissionChecker.missingPermissions()

    /** `true` si todavía se puede pedir más acceso (tipo ausente o acceso parcial). */
    fun canRequestMoreAccess(): Boolean = _state.value.let { state ->
        state.hasMissingTypes || state.hasPartialAccess
    }

    /** Acceso por tipo, para textos que expliquen qué falta. */
    fun accessFor(type: MediaType): MediaTypeAccess = _state.value.typeAccess.forType(type)

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    /** Elementos a compartir: la selección si existe, o el elemento abierto en el visor. */
    fun shareItems(): List<MediaItem> {
        val current = _state.value
        if (current.selection.isActive) {
            return current.displayItems.filter { it.uri in current.selection.selected }
        }
        current.viewer?.let { viewer ->
            return viewer.items.getOrNull(viewer.index)?.let { listOf(it) } ?: emptyList()
        }
        return emptyList()
    }

    private suspend fun loadAlbums() {
        try {
            val albums = repository.albums(currentFilter(ignoreAlbum = true))
            _state.update { it.copy(albums = albums) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            _state.update { it.copy(error = error.message ?: ALBUMS_ERROR) }
        }
    }

    private fun currentFilter(ignoreAlbum: Boolean = false): MediaFilter {
        val current = _state.value
        return MediaFilter(
            types = current.readableTypes,
            albumId = if (ignoreAlbum) null else current.album?.id,
            onlyFavorites = current.section == GallerySection.FAVORITES
        )
    }

    private fun applyFavorites(uris: Set<String>) {
        _state.update { current ->
            current.copy(
                favoriteUris = uris,
                items = current.items.map { it.withFavorite(uris) },
                searchResults = current.searchResults.map { it.withFavorite(uris) },
                viewer = current.viewer?.let { viewer ->
                    viewer.copy(items = viewer.items.map { it.withFavorite(uris) })
                }
            )
        }
    }

    private fun MediaItem.withFavorite(uris: Set<String>): MediaItem =
        if (isFavorite == (uri in uris)) this else copy(isFavorite = uri in uris)

    companion object {
        const val PAGE_SIZE = 60
        private const val SEARCH_LIMIT = 200
        private const val SEARCH_DEBOUNCE_MS = 250L
        private const val LOAD_ERROR = "No se pudo leer la biblioteca"
        private const val SEARCH_ERROR = "No se pudo completar la búsqueda"
        private const val ALBUMS_ERROR = "No se pudieron cargar los álbumes"
        private const val FAVORITE_ERROR = "No se pudo actualizar el favorito"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as NexaPhotosApplication
                GalleryViewModel(
                    repository = application.container.mediaRepository,
                    favoriteRepository = application.container.favoriteRepository,
                    permissionChecker = application.container.mediaPermissionChecker
                )
            }
        }
    }
}
