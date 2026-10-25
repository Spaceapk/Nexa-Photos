package org.nexlibre.nexaphotos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nexlibre.nexaphotos.domain.model.MediaType
import org.nexlibre.nexaphotos.ui.media.gallery.accessNotice
import org.nexlibre.nexaphotos.ui.media.gallery.emptyLibraryMessage
import org.nexlibre.nexaphotos.ui.media.gallery.timelineLabel

/** La UI debe explicar los permisos por tipo en lugar de ocultar contenido en silencio. */
class AccessNoticeTest {

    @Test
    fun `sin ningun tipo legible la pantalla de permisos se encarga`() {
        assertNull(
            accessNotice(
                readableTypes = emptySet(),
                missingTypes = setOf(MediaType.IMAGE, MediaType.VIDEO),
                partialTypes = emptySet()
            )
        )
    }

    @Test
    fun `faltar videos se explica y se ofrece el permiso`() {
        val notice = accessNotice(
            readableTypes = setOf(MediaType.IMAGE),
            missingTypes = setOf(MediaType.VIDEO),
            partialTypes = emptySet()
        )!!

        assertTrue(notice.message.contains("videos"))
        assertTrue(notice.message.contains("solo se muestran fotos"))
        assertEquals("Permitir videos", notice.actionLabel)
    }

    @Test
    fun `faltar fotos se explica y se ofrece el permiso`() {
        val notice = accessNotice(
            readableTypes = setOf(MediaType.VIDEO),
            missingTypes = setOf(MediaType.IMAGE),
            partialTypes = emptySet()
        )!!

        assertTrue(notice.message.contains("fotos"))
        assertTrue(notice.message.contains("solo se muestran videos"))
        assertEquals("Permitir fotos", notice.actionLabel)
    }

    @Test
    fun `tipo ausente y otro parcial se explican juntos`() {
        val notice = accessNotice(
            readableTypes = setOf(MediaType.IMAGE),
            missingTypes = setOf(MediaType.VIDEO),
            partialTypes = setOf(MediaType.IMAGE)
        )!!

        assertTrue(notice.message.contains("Falta el permiso de videos"))
        assertTrue(notice.message.contains("elegiste"))
        assertEquals("Permitir videos", notice.actionLabel)
    }

    @Test
    fun `acceso parcial en ambos tipos avisa de los elementos elegidos`() {
        val notice = accessNotice(
            readableTypes = MediaType.ALL,
            missingTypes = emptySet(),
            partialTypes = setOf(MediaType.IMAGE, MediaType.VIDEO)
        )!!

        assertTrue(notice.message.contains("Acceso parcial"))
        assertEquals("Elegir más", notice.actionLabel)
    }

    @Test
    fun `acceso parcial de un solo tipo se nombra`() {
        val videos = accessNotice(
            readableTypes = MediaType.ALL,
            missingTypes = emptySet(),
            partialTypes = setOf(MediaType.VIDEO)
        )!!
        assertTrue(videos.message.contains("Videos con acceso parcial"))

        val photos = accessNotice(
            readableTypes = MediaType.ALL,
            missingTypes = emptySet(),
            partialTypes = setOf(MediaType.IMAGE)
        )!!
        assertTrue(photos.message.contains("Fotos con acceso parcial"))
    }

    @Test
    fun `acceso completo no muestra aviso`() {
        assertNull(
            accessNotice(
                readableTypes = MediaType.ALL,
                missingTypes = emptySet(),
                partialTypes = emptySet()
            )
        )
    }

    @Test
    fun `la etiqueta de la seccion no promete tipos sin permiso`() {
        assertEquals("Fotos y videos", timelineLabel(MediaType.ALL))
        assertEquals("Fotos", timelineLabel(setOf(MediaType.IMAGE)))
        assertEquals("Videos", timelineLabel(setOf(MediaType.VIDEO)))
        assertEquals("Biblioteca", timelineLabel(emptySet()))
    }

    @Test
    fun `el estado vacio distingue la falta de permiso`() {
        assertEquals(
            "No hay fotos visibles",
            emptyLibraryMessage(setOf(MediaType.IMAGE), setOf(MediaType.VIDEO))
        )
        assertEquals(
            "No hay videos visibles",
            emptyLibraryMessage(setOf(MediaType.VIDEO), setOf(MediaType.IMAGE))
        )
        assertEquals(
            "No hay fotos ni videos",
            emptyLibraryMessage(MediaType.ALL, emptySet())
        )
    }
}
