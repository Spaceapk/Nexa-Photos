package org.nexlibre.nexaphotos

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nexlibre.nexaphotos.data.media.MediaStoreMediaRepository
import org.nexlibre.nexaphotos.data.media.RawAlbumRow
import org.nexlibre.nexaphotos.domain.model.MediaFilter
import org.nexlibre.nexaphotos.domain.model.MediaType
import org.nexlibre.nexaphotos.domain.model.PageRequest

class MediaStoreMediaRepositoryTest {

    private val source = FakeMediaStoreSource(
        rows = listOf(
            rawMediaRow(id = 1, dateTakenMillis = 3_000L),
            rawMediaRow(id = 2, type = MediaType.VIDEO, dateTakenMillis = 2_000L),
            rawMediaRow(id = 3, dateTakenMillis = 1_000L, bucketId = "shots", bucketName = "Capturas")
        ),
        albumRows = listOf(
            RawAlbumRow("camera", "Cámara", 2, "content://media/external/images/media/1", latestDateMillis = 3_000L),
            RawAlbumRow("shots", "Capturas", 9, "content://media/external/images/media/3", latestDateMillis = 5_000L)
        ),
        counts = mapOf(MediaType.IMAGE to 2, MediaType.VIDEO to 1)
    )

    private val favorites = FakeFavoriteRepository(setOf(mediaItem(1).uri))
    private val repository = MediaStoreMediaRepository(source, favorites)

    @Test
    fun `pagina vacia sin consultar cuando no hay tipos legibles`() = runTest {
        val page = repository.page(MediaFilter(types = emptySet()), PageRequest(limit = 10))

        assertEquals(0, source.pageCalls)
        assertTrue(page.items.isEmpty())
        assertFalse(page.hasMore)
    }

    @Test
    fun `marca los favoritos al paginar`() = runTest {
        val page = repository.page(MediaFilter(), PageRequest(limit = 10))

        assertEquals(3, page.items.size)
        assertTrue(page.items.first { it.uri == mediaItem(1).uri }.isFavorite)
        assertFalse(page.items.first { it.uri == mediaItem(2).uri }.isFavorite)
        assertFalse(page.hasMore)
    }

    @Test
    fun `solo favoritos sin favoritos devuelve vacio sin consultar`() = runTest {
        favorites.emit(emptySet())
        val page = repository.page(
            MediaFilter(onlyFavorites = true),
            PageRequest(limit = 10)
        )

        assertEquals(0, source.pageCalls)
        assertTrue(page.items.isEmpty())
    }

    @Test
    fun `solo favoritos consulta por uri`() = runTest {
        repository.page(MediaFilter(onlyFavorites = true), PageRequest(limit = 10))

        val query = source.lastQuery
        assertEquals(listOf(mediaItem(1).uri), query?.uris)
        assertTrue(query?.onlyFavorites == true)
    }

    @Test
    fun `solo fotos filtra el tipo`() = runTest {
        repository.page(MediaFilter(types = setOf(MediaType.IMAGE)), PageRequest(limit = 10))

        assertEquals(setOf(MediaType.IMAGE), source.lastQuery?.types)
    }

    @Test
    fun `albumes ordenados por fecha del contenido`() = runTest {
        val albums = repository.albums()

        assertEquals(listOf("Capturas", "Cámara"), albums.map { it.name })
        assertEquals(9, albums.first().itemCount)
    }

    @Test
    fun `busqueda recorta el termino y marca favoritos`() = runTest {
        val results = repository.search("  foto-1 ", limit = 10)

        assertEquals("foto-1", source.lastQuery?.search)
        assertEquals(mediaItem(1).uri, results.first().uri)
        assertTrue(results.first().isFavorite)
    }

    @Test
    fun `busqueda vacia no consulta`() = runTest {
        assertTrue(repository.search("   ").isEmpty())
        assertEquals(0, source.pageCalls)
    }

    @Test
    fun `item inexistente devuelve null`() = runTest {
        assertNull(repository.item("content://media/external/images/media/999"))
        assertNull(repository.item(""))
    }

    @Test
    fun `item devuelve favorito`() = runTest {
        val item = repository.item(mediaItem(1).uri)

        assertEquals(mediaItem(1).uri, item?.uri)
        assertTrue(item?.isFavorite == true)
    }

    @Test
    fun `items conserva el orden solicitado`() = runTest {
        val items = repository.items(listOf(mediaItem(3).uri, mediaItem(1).uri))

        assertEquals(listOf(mediaItem(3).uri, mediaItem(1).uri), items.map { it.uri })
    }

    @Test
    fun `stats suma imagenes videos y favoritos`() = runTest {
        val stats = repository.stats()

        assertEquals(3, stats.totalItems)
        assertEquals(2, stats.imageCount)
        assertEquals(1, stats.videoCount)
        assertEquals(1, stats.favoriteCount)
        assertEquals(1, favorites.favoriteUris.first().size)
    }
}
