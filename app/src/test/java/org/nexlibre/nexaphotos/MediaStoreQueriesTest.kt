package org.nexlibre.nexaphotos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nexlibre.nexaphotos.data.media.MediaCursor
import org.nexlibre.nexaphotos.data.media.MediaStoreQueries
import org.nexlibre.nexaphotos.data.media.MediaStoreQuery
import org.nexlibre.nexaphotos.domain.model.MediaType

class MediaStoreQueriesTest {

    @Test
    fun `sin tipos consultables no hay seleccion`() {
        assertNull(MediaStoreQueries.selection(MediaStoreQuery(limit = 10, types = emptySet())))
    }

    @Test
    fun `filtra por tipo y descarta pendientes`() {
        val selection = MediaStoreQueries.selection(
            MediaStoreQuery(limit = 10, types = setOf(MediaType.IMAGE, MediaType.VIDEO))
        )
        assertNotNull(selection)
        assertTrue(selection!!.clause.contains("media_type IN (1,2)"))
        assertTrue(selection.clause.contains("is_pending = 0"))
        assertTrue(selection.args.isEmpty())
    }

    @Test
    fun `solo imagenes usa media_type 1`() {
        val selection = MediaStoreQueries.selection(
            MediaStoreQuery(limit = 10, types = setOf(MediaType.IMAGE))
        )
        assertEquals(true, selection!!.clause.contains("media_type IN (1)"))
    }

    @Test
    fun `el album se filtra por bucket_id`() {
        val selection = MediaStoreQueries.selection(
            MediaStoreQuery(limit = 10, types = MediaType.ALL, albumId = "123456")
        )
        assertTrue(selection!!.clause.contains("bucket_id = ?"))
        assertEquals(listOf("123456"), selection.args)
    }

    @Test
    fun `la busqueda escapa comodines de LIKE`() {
        val selection = MediaStoreQueries.selection(
            MediaStoreQuery(limit = 10, types = MediaType.ALL, search = " 50%_off ")
        )
        assertEquals(listOf("%50\\%\\_off%", "%50\\%\\_off%", "%50\\%\\_off%"), selection!!.args)
        assertTrue(selection.clause.contains("_display_name LIKE ?"))
        assertTrue(selection.clause.contains("ESCAPE"))
    }

    @Test
    fun `el cursor aplica keyset sobre fecha e id`() {
        val cursor = MediaCursor(dateTakenMillis = 1_700_000_000_000L, id = 77L)
        val selection = MediaStoreQueries.selection(
            MediaStoreQuery(limit = 10, types = MediaType.ALL, cursor = cursor.encode())
        )
        assertTrue(selection!!.clause.contains("< ?"))
        assertEquals(listOf("1700000000000", "1700000000000", "77"), selection.args)
    }

    @Test
    fun `las uri favoritas se convierten en ids`() {
        val selection = MediaStoreQueries.selection(
            MediaStoreQuery(
                limit = 10,
                types = MediaType.ALL,
                uris = listOf(
                    "content://media/external/images/media/9",
                    "content://media/external/video/media/11"
                )
            )
        )
        assertTrue(selection!!.clause.contains("_id IN (9,11)"))
    }

    @Test
    fun `uri sin id valida no produce resultados`() {
        val selection = MediaStoreQueries.selection(
            MediaStoreQuery(limit = 10, types = MediaType.ALL, uris = listOf("no-es-una-uri"))
        )
        assertEquals("0", selection!!.clause)
    }

    @Test
    fun `extrae el id de una uri`() {
        assertEquals(42L, MediaStoreQueries.uriId("content://media/external/images/media/42"))
        assertEquals(7L, MediaStoreQueries.uriId("content://media/external/images/media/7?x=1"))
        assertNull(MediaStoreQueries.uriId("content://media/external/images/media/abc"))
        assertNull(MediaStoreQueries.uriId(""))
    }

    @Test
    fun `el orden es cronologico descendente con desempate por id`() {
        // La fecha efectiva descarta datetaken nulo, cero o negativo y recorta date_added a cero,
        // igual que la normalización al mapear filas.
        assertEquals(
            "IFNULL(NULLIF(MAX(datetaken, 0), 0), IFNULL(MAX(date_added, 0), 0) * 1000) DESC, _id DESC",
            MediaStoreQueries.sortOrder
        )
        assertEquals(
            "IFNULL(NULLIF(MAX(datetaken, 0), 0), IFNULL(MAX(date_added, 0), 0) * 1000)",
            MediaStoreQueries.effectiveDateExpression
        )
    }

    @Test
    fun `la normalizacion de fechas cubre nulo cero y negativo`() {
        assertEquals(5_000L, MediaStoreQueries.normalizedDateMillis(5_000L, 1_000L))
        assertEquals(1_000L, MediaStoreQueries.normalizedDateMillis(null, 1_000L))
        assertEquals(1_000L, MediaStoreQueries.normalizedDateMillis(0L, 1_000L))
        assertEquals(1_000L, MediaStoreQueries.normalizedDateMillis(-1L, 1_000L))
        assertEquals(0L, MediaStoreQueries.normalizedDateMillis(-1L, -1_000L))
        assertEquals(0L, MediaStoreQueries.normalizedDateMillis(null, 0L))
    }

    @Test
    fun `el predicado del cursor es estricto en fecha e id`() {
        val cursor = MediaCursor(dateTakenMillis = 100L, id = 10L)
        assertTrue(MediaStoreQueries.isStrictlyAfterCursor(cursor, dateMillis = 90L, id = 99L))
        assertTrue(MediaStoreQueries.isStrictlyAfterCursor(cursor, dateMillis = 100L, id = 9L))
        assertFalse(MediaStoreQueries.isStrictlyAfterCursor(cursor, dateMillis = 100L, id = 10L))
        assertFalse(MediaStoreQueries.isStrictlyAfterCursor(cursor, dateMillis = 100L, id = 11L))
        assertFalse(MediaStoreQueries.isStrictlyAfterCursor(cursor, dateMillis = 110L, id = 1L))
    }
}
