package org.nexlibre.nexaphotos

import org.junit.Assert.assertEquals
import org.junit.Test
import org.nexlibre.nexaphotos.data.media.MediaUris

class MediaUrisTest {

    @Test
    fun `normaliza espacios y esquema ausente`() {
        assertEquals(
            "content://media/external/images/media/1",
            MediaUris.normalize("  content://media/external/images/media/1  ")
        )
        assertEquals(
            "content://media/external/images/media/1",
            MediaUris.normalize("//media/external/images/media/1")
        )
    }

    @Test
    fun `conserva esquemas desconocidos sin romper`() {
        assertEquals("file:///storage/photo.jpg", MediaUris.normalize("file:///storage/photo.jpg"))
        assertEquals("", MediaUris.normalize("   "))
    }

    @Test
    fun `codifica y decodifica el id de album`() {
        val id = "-1735689600000/DCIM Cámara"
        assertEquals(id, MediaUris.decodeAlbumId(MediaUris.encodeAlbumId(id)))
    }

    @Test
    fun `calcula la extension del nombre`() {
        assertEquals("jpg", MediaUris.extensionOf("IMG_0001.JPG"))
        assertEquals("", MediaUris.extensionOf("IMG_0001"))
        assertEquals("mp4", MediaUris.extensionOf("clip.final.mp4"))
    }
}
