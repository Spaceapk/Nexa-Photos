package org.nexlibre.nexaphotos

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.nexlibre.nexaphotos.data.media.MediaStoreMediaRepository
import org.nexlibre.nexaphotos.data.permissions.MediaAccess
import org.nexlibre.nexaphotos.domain.model.MediaAlbum
import org.nexlibre.nexaphotos.domain.model.MediaType
import org.nexlibre.nexaphotos.ui.media.gallery.GallerySection
import org.nexlibre.nexaphotos.ui.media.gallery.GalleryViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        source: FakeMediaStoreSource,
        favorites: FakeFavoriteRepository = FakeFavoriteRepository(),
        checker: FakePermissionChecker = FakePermissionChecker()
    ): GalleryViewModel = GalleryViewModel(
        repository = MediaStoreMediaRepository(source, favorites),
        favoriteRepository = favorites,
        permissionChecker = checker
    )

    @Test
    fun `sin permisos no consulta la biblioteca`() = runTest {
        val source = FakeMediaStoreSource(rows = listOf(rawMediaRow(id = 1)))
        val viewModel = createViewModel(
            source,
            checker = FakePermissionChecker(currentAccess = MediaAccess.NONE, currentTypes = emptySet())
        )

        advanceUntilIdle()

        assertEquals(0, source.pageCalls)
        assertTrue(viewModel.state.value.needsPermission)
        assertTrue(viewModel.state.value.displayItems.isEmpty())
    }

    @Test
    fun `carga la primera pagina con favoritos aplicados`() = runTest {
        val source = FakeMediaStoreSource(
            rows = listOf(rawMediaRow(id = 1), rawMediaRow(id = 2))
        )
        val favorites = FakeFavoriteRepository(setOf(mediaItem(2).uri))
        val viewModel = createViewModel(source, favorites)

        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.items.size)
        assertFalse(state.isLoading)
        assertFalse(state.hasMore)
        assertTrue(state.items.first { it.uri == mediaItem(2).uri }.isFavorite)
        assertFalse(state.needsPermission)
    }

    @Test
    fun `paginacion incremental agrega elementos sin duplicados`() = runTest {
        val rows = (1L..130L).map { rawMediaRow(id = it) }
        val source = FakeMediaStoreSource(rows = rows)
        val viewModel = createViewModel(source)

        advanceUntilIdle()
        assertEquals(GalleryViewModel.PAGE_SIZE, viewModel.state.value.items.size)
        assertTrue(viewModel.state.value.hasMore)

        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals(GalleryViewModel.PAGE_SIZE * 2, viewModel.state.value.items.size)
        assertTrue(viewModel.state.value.hasMore)

        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals(130, viewModel.state.value.items.size)
        assertFalse(viewModel.state.value.hasMore)
        assertEquals(130, viewModel.state.value.items.map { it.uri }.distinct().size)
    }

    @Test
    fun `seleccion multiple y seleccionar todo`() = runTest {
        val source = FakeMediaStoreSource(rows = listOf(rawMediaRow(id = 1), rawMediaRow(id = 2)))
        val viewModel = createViewModel(source)
        advanceUntilIdle()

        viewModel.toggleSelection(mediaItem(1).uri)
        assertEquals(1, viewModel.state.value.selection.count)

        viewModel.toggleSelection(mediaItem(1).uri)
        assertFalse(viewModel.state.value.selection.isActive)

        viewModel.selectAllVisible()
        assertEquals(2, viewModel.state.value.selection.count)

        viewModel.clearSelection()
        assertFalse(viewModel.state.value.selection.isActive)
    }

    @Test
    fun `alternar favorito delega en el repositorio y refleja el flujo`() = runTest {
        val source = FakeMediaStoreSource(rows = listOf(rawMediaRow(id = 1)))
        val favorites = FakeFavoriteRepository()
        val viewModel = createViewModel(source, favorites)
        advanceUntilIdle()

        viewModel.toggleFavorite(mediaItem(1))
        advanceUntilIdle()

        assertEquals(listOf(mediaItem(1).uri to true), favorites.setCalls)
        assertTrue(viewModel.state.value.favoriteUris.contains(mediaItem(1).uri))
        assertTrue(viewModel.state.value.items.first().isFavorite)

        viewModel.toggleFavorite(mediaItem(1))
        advanceUntilIdle()

        assertEquals(mediaItem(1).uri to false, favorites.setCalls.last())
        assertFalse(viewModel.state.value.favoriteUris.contains(mediaItem(1).uri))
    }

    @Test
    fun `seccion favoritos filtra por favoritos`() = runTest {
        val source = FakeMediaStoreSource(rows = listOf(rawMediaRow(id = 1), rawMediaRow(id = 2)))
        val favorites = FakeFavoriteRepository(setOf(mediaItem(1).uri))
        val viewModel = createViewModel(source, favorites)
        advanceUntilIdle()

        viewModel.setSection(GallerySection.FAVORITES)
        advanceUntilIdle()

        assertTrue(source.lastQuery?.onlyFavorites == true)
        assertEquals(listOf(mediaItem(1).uri), viewModel.state.value.items.map { it.uri })
    }

    @Test
    fun `busqueda con debounce actualiza resultados`() = runTest {
        val source = FakeMediaStoreSource(
            rows = listOf(rawMediaRow(id = 1, name = "playa"), rawMediaRow(id = 2, name = "gato"))
        )
        val viewModel = createViewModel(source)
        advanceUntilIdle()

        viewModel.setQuery("playa")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isSearchActive)
        assertEquals(1, viewModel.state.value.searchResults.size)
        assertEquals("playa", source.lastQuery?.search)

        viewModel.setQuery("")
        advanceUntilIdle()
        assertTrue(viewModel.state.value.searchResults.isEmpty())
        assertFalse(viewModel.state.value.isSearchActive)
    }

    @Test
    fun `abrir album filtra y cerrar vuelve a la biblioteca`() = runTest {
        val source = FakeMediaStoreSource(
            rows = listOf(
                rawMediaRow(id = 1, bucketId = "camera"),
                rawMediaRow(id = 2, bucketId = "shots")
            )
        )
        val viewModel = createViewModel(source)
        advanceUntilIdle()

        viewModel.openAlbum(MediaAlbum(id = "shots", name = "Capturas", itemCount = 1, coverUri = null))
        advanceUntilIdle()

        assertEquals("shots", source.lastQuery?.albumId)
        assertEquals(listOf(mediaItem(2).uri), viewModel.state.value.items.map { it.uri })

        viewModel.closeAlbum()
        advanceUntilIdle()

        assertEquals(null, source.lastQuery?.albumId)
        assertEquals(2, viewModel.state.value.items.size)
    }

    @Test
    fun `visor conserva indice y devuelve el elemento para compartir`() = runTest {
        val source = FakeMediaStoreSource(rows = listOf(rawMediaRow(id = 1), rawMediaRow(id = 2)))
        val viewModel = createViewModel(source)
        advanceUntilIdle()

        viewModel.openViewer(1)
        val viewer = viewModel.state.value.viewer
        assertNotNull(viewer)
        assertEquals(1, viewer?.index)
        assertEquals(listOf(mediaItem(2).uri), viewModel.shareItems().map { it.uri })

        viewModel.toggleSelection(mediaItem(1).uri)
        assertEquals(listOf(mediaItem(1).uri), viewModel.shareItems().map { it.uri })

        viewModel.closeViewer()
        assertEquals(null, viewModel.state.value.viewer)
    }

    @Test
    fun `el acceso parcial se refleja en el estado`() = runTest {
        val source = FakeMediaStoreSource(rows = listOf(rawMediaRow(id = 1, type = MediaType.VIDEO)))
        val viewModel = createViewModel(
            source,
            checker = FakePermissionChecker(
                sdkInt = 34,
                currentAccess = MediaAccess.PARTIAL,
                currentTypes = MediaType.ALL
            )
        )

        advanceUntilIdle()

        assertTrue(viewModel.state.value.hasPartialAccess)
        assertEquals(1, viewModel.state.value.items.size)
    }
}
