package org.nexlibre.nexaphotos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nexlibre.nexaphotos.domain.model.MediaType
import org.nexlibre.nexaphotos.ui.media.gallery.MediaShare

class MediaShareTest {

    @Test
    fun `sin elementos no hay payload`() {
        assertNull(MediaShare.payload(emptyList()))
    }

    @Test
    fun `solo imagenes usa image wildcard`() {
        val payload = MediaShare.payload(listOf(mediaItem(1), mediaItem(2)))!!

        assertEquals("image/*", payload.mimeType)
        assertEquals(2, payload.uris.size)
        assertTrue(payload.multiple)
    }

    @Test
    fun `solo videos usa video wildcard`() {
        val payload = MediaShare.payload(
            listOf(mediaItem(1, MediaType.VIDEO), mediaItem(2, MediaType.VIDEO))
        )!!

        assertEquals("video/*", payload.mimeType)
    }

    @Test
    fun `mezcla usa cualquier tipo`() {
        val payload = MediaShare.payload(
            listOf(mediaItem(1), mediaItem(2, MediaType.VIDEO))
        )!!

        assertEquals("*/*", payload.mimeType)
    }

    @Test
    fun `un solo elemento no es multiple`() {
        val payload = MediaShare.payload(listOf(mediaItem(5)))!!

        assertFalse(payload.multiple)
        assertEquals(listOf(mediaItem(5).uri), payload.uris)
    }
}
