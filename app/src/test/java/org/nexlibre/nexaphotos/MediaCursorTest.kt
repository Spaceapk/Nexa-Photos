package org.nexlibre.nexaphotos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.nexlibre.nexaphotos.data.media.MediaCursor

class MediaCursorTest {

    @Test
    fun `codifica y decodifica sin perdida`() {
        val cursor = MediaCursor(dateTakenMillis = 1_700_000_000_123L, id = 42L)

        val decoded = MediaCursor.decode(cursor.encode())

        assertEquals(cursor, decoded)
    }

    @Test
    fun `decodifica cero como un cursor valido`() {
        assertEquals(MediaCursor(0L, 0L), MediaCursor.decode(MediaCursor(0L, 0L).encode()))
    }

    @Test
    fun `cursors vacios o mal formados devuelven null`() {
        assertNull(MediaCursor.decode(null))
        assertNull(MediaCursor.decode(""))
        assertNull(MediaCursor.decode("   "))
        assertNull(MediaCursor.decode("v1:1"))
        assertNull(MediaCursor.decode("v2:1:2"))
        assertNull(MediaCursor.decode("v1:abc:2"))
        assertNull(MediaCursor.decode("v1:1:abc"))
        assertNull(MediaCursor.decode("v1:-1:2"))
        assertNull(MediaCursor.decode("v1:1:-2"))
    }

    @Test
    fun `codifica con separador estable`() {
        assertEquals("v1:10:7", MediaCursor(10L, 7L).encode())
    }
}
