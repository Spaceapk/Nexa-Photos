package org.nexlibre.nexaphotos

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nexlibre.nexaphotos.data.media.MediaStoreMediaRepository
import org.nexlibre.nexaphotos.data.media.RawAlbumRow
import org.nexlibre.nexaphotos.domain.model.MediaAlbum
import org.nexlibre.nexaphotos.ui.media.gallery.albumCountDescription
import org.nexlibre.nexaphotos.ui.media.gallery.albumCountLabel

class AlbumCountTest {

    private fun album(count: Int, exact: Boolean = true) = MediaAlbum(
        id = "camera",
        name = "Cámara",
        itemCount = count,
        coverUri = null,
        countIsExact = exact
    )

    @Test
    fun `un conteo truncado se marca como inexacto en el dominio`() = runTest {
        val source = FakeMediaStoreSource(
            albumRows = listOf(
                RawAlbumRow("camera", "Cámara", 100_000, null, countIsExact = false),
                RawAlbumRow("shots", "Capturas", 12, null, countIsExact = true)
            )
        )
        val repository = MediaStoreMediaRepository(source, FakeFavoriteRepository())

        val albums = repository.albums()

        val camera = albums.first { it.name == "Cámara" }
        val shots = albums.first { it.name == "Capturas" }
        assertFalse("El conteo truncado no debe presentarse como exacto", camera.countIsExact)
        assertTrue(shots.countIsExact)
    }

    @Test
    fun `un album truncado muestra el signo mas y no un total exacto`() {
        assertEquals("100000+ elementos", albumCountLabel(album(100_000, exact = false)))
        assertEquals("12 elementos", albumCountLabel(album(12)))
        assertEquals("1 elemento", albumCountLabel(album(1)))
    }

    @Test
    fun `la descripcion accesible distingue conteos truncados`() {
        assertTrue(albumCountDescription(album(100_000, exact = false)).contains("al menos"))
        assertFalse(albumCountDescription(album(12)).contains("al menos"))
        assertTrue(albumCountDescription(album(100_000, exact = false)).contains("100000+"))
    }

    @Test
    fun `por defecto los conteos se consideran exactos`() {
        val default = MediaAlbum(id = "x", name = "X", itemCount = 3, coverUri = null)
        assertTrue(default.countIsExact)
        val raw = RawAlbumRow("x", "X", 3, null)
        assertTrue(raw.countIsExact)
    }
}
