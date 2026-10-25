package org.nexlibre.nexaphotos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nexlibre.nexaphotos.data.media.MediaCursor
import org.nexlibre.nexaphotos.data.media.MediaStoreQueries
import org.nexlibre.nexaphotos.data.media.RawMediaRow
import org.nexlibre.nexaphotos.data.media.RawMediaPage

/**
 * Integridad de la paginación por keyset frente a la normalización de fechas.
 *
 * El fallo que detectan estas pruebas: el SQL ordenaba por `IFNULL(datetaken, date_added*1000)`
 * mientras el mapeo usaba `date_added` cuando `datetaken` era 0 o negativo. El cursor se construía
 * con la fecha mapeada, así que la página siguiente volvía a incluir filas ya entregadas o se
 * saltaba otras.
 */
class PaginationIntegrityTest {

    /** Emula MediaStore: orden `fecha DESC, _id DESC` + cláusula keyset, con la MISMA regla de fecha. */
    private fun pageOf(rows: List<RawMediaRow>, limit: Int, cursor: MediaCursor?): RawMediaPage {
        val sorted = rows.sortedWith(MediaStoreQueries.chronologicalDescending)
        val filtered = if (cursor == null) {
            sorted
        } else {
            sorted.filter { row ->
                MediaStoreQueries.isStrictlyAfterCursor(
                    cursor = cursor,
                    dateMillis = row.dateTakenMillis,
                    id = MediaStoreQueries.uriId(row.contentUri) ?: 0L
                )
            }
        }
        val page = filtered.take(limit)
        val hasMore = filtered.size > page.size
        val next = if (hasMore) {
            page.last().let { last ->
                MediaCursor(
                    dateTakenMillis = last.dateTakenMillis,
                    id = MediaStoreQueries.uriId(last.contentUri) ?: 0L
                )
            }
        } else {
            null
        }
        return RawMediaPage(rows = page, nextCursor = next?.encode(), hasMore = hasMore)
    }

    /** Recorre todas las páginas y devuelve los IDs entregados, en orden. */
    private fun drain(rows: List<RawMediaRow>, limit: Int): List<Long> {
        val collected = mutableListOf<Long>()
        var cursor: MediaCursor? = null
        var guard = 0
        while (true) {
            guard++
            require(guard <= 1000) { "La paginación no termina" }
            val page = pageOf(rows, limit, cursor)
            collected += page.rows.mapNotNull { MediaStoreQueries.uriId(it.contentUri) }
            val next = MediaCursor.decode(page.nextCursor) ?: break
            cursor = next
        }
        return collected
    }

    /** Fila con fecha cruda de MediaStore: `datetaken` puede ser null, 0 o negativo. */
    private fun row(id: Long, dateTakenMillis: Long?, dateAddedSeconds: Long): RawMediaRow =
        rawMediaRow(
            id = id,
            dateTakenMillis = MediaStoreQueries.normalizedDateFromColumns(dateTakenMillis, dateAddedSeconds)
        )

    private fun ids(rows: List<RawMediaRow>): List<Long> = rows.mapNotNull { MediaStoreQueries.uriId(it.contentUri) }

    @Test
    fun `dateTaken nulo usa dateAdded y la paginacion no repite ni omite`() {
        val rows = listOf(
            row(id = 1, dateTakenMillis = 5_000L, dateAddedSeconds = 5),
            row(id = 2, dateTakenMillis = null, dateAddedSeconds = 4),
            row(id = 3, dateTakenMillis = null, dateAddedSeconds = 3),
            row(id = 4, dateTakenMillis = 2_000L, dateAddedSeconds = 2),
            row(id = 5, dateTakenMillis = null, dateAddedSeconds = 1)
        )

        val collected = drain(rows, limit = 2)

        assertEquals(ids(rows).size, collected.size)
        assertEquals(collected.distinct(), collected)
        val expected = rows.sortedWith(MediaStoreQueries.chronologicalDescending)
            .mapNotNull { MediaStoreQueries.uriId(it.contentUri) }
        assertEquals(expected, collected)
    }

    @Test
    fun `dateTaken cero no rompe la paginacion`() {
        // Antes: SQL usaba 0 (IFNULL no cubre 0) y el mapeo date_added -> cursor incoherente.
        val rows = listOf(
            row(id = 1, dateTakenMillis = 0L, dateAddedSeconds = 9),
            row(id = 2, dateTakenMillis = 0L, dateAddedSeconds = 8),
            row(id = 3, dateTakenMillis = 0L, dateAddedSeconds = 7),
            row(id = 4, dateTakenMillis = 0L, dateAddedSeconds = 6),
            row(id = 5, dateTakenMillis = 0L, dateAddedSeconds = 5)
        )

        val collected = drain(rows, limit = 2)

        assertEquals(5, collected.size)
        assertEquals(collected.distinct(), collected)
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), collected)
        // Todas las fechas efectivas son positivas (date_added * 1000), nunca 0.
        assertTrue(rows.all { it.dateTakenMillis > 0L })
    }

    @Test
    fun `dateTaken negativo cae a dateAdded sin duplicados ni omisiones`() {
        val rows = listOf(
            row(id = 1, dateTakenMillis = -5_000L, dateAddedSeconds = 8),
            row(id = 2, dateTakenMillis = 1_000L, dateAddedSeconds = 1),
            row(id = 3, dateTakenMillis = -1L, dateAddedSeconds = 7),
            row(id = 4, dateTakenMillis = null, dateAddedSeconds = 6),
            row(id = 5, dateTakenMillis = -9_999_999L, dateAddedSeconds = 5)
        )

        val collected = drain(rows, limit = 2)

        assertEquals(5, collected.size)
        assertEquals(collected.distinct(), collected)
        assertTrue(collected.containsAll(listOf(1L, 2L, 3L, 4L, 5L)))
    }

    @Test
    fun `dateAdded negativo o cero se recorta a cero`() {
        assertEquals(0L, MediaStoreQueries.normalizedDateFromColumns(null, -10L))
        assertEquals(0L, MediaStoreQueries.normalizedDateFromColumns(0L, 0L))
        assertEquals(4_000L, MediaStoreQueries.normalizedDateFromColumns(-1L, 4L))
        assertEquals(9_000L, MediaStoreQueries.normalizedDateFromColumns(9_000L, 1L))
    }

    @Test
    fun `la regla kotlin coincide con la expresion SQL documentada`() {
        // La expresión SQL cubre: NULL, 0 y negativos en datetaken; date_added nulo o negativo.
        assertTrue(MediaStoreQueries.effectiveDateExpression.contains("NULLIF(MAX(datetaken, 0), 0)"))
        assertTrue(MediaStoreQueries.effectiveDateExpression.contains("IFNULL(MAX(date_added, 0), 0) * 1000"))
        assertEquals(
            MediaStoreQueries.effectiveDateExpression,
            MediaStoreQueries.sortOrder.substringBefore(" DESC")
        )
    }

    @Test
    fun `fechas iguales se desempatan por id sin perder filas`() {
        val rows = (1L..7L).map { row(id = it, dateTakenMillis = 1_000L, dateAddedSeconds = 1) }

        val collected = drain(rows, limit = 3)

        assertEquals(7, collected.size)
        assertEquals(collected.distinct(), collected)
        assertEquals(listOf(7L, 6L, 5L, 4L, 3L, 2L, 1L), collected)
    }

    @Test
    fun `mezcla de fechas y empates cubre toda la lista exactamente una vez`() {
        val rows = listOf(
            row(id = 1, dateTakenMillis = 3_000L, dateAddedSeconds = 3),
            row(id = 2, dateTakenMillis = 3_000L, dateAddedSeconds = 3),
            row(id = 3, dateTakenMillis = null, dateAddedSeconds = 3),
            row(id = 4, dateTakenMillis = 0L, dateAddedSeconds = 2),
            row(id = 5, dateTakenMillis = -4L, dateAddedSeconds = 2),
            row(id = 6, dateTakenMillis = 1_000L, dateAddedSeconds = 1),
            row(id = 7, dateTakenMillis = null, dateAddedSeconds = 0)
        )

        val collected = drain(rows, limit = 2)

        assertEquals(7, collected.size)
        assertEquals(collected.distinct(), collected)
        assertEquals((1L..7L).toSet(), collected.toSet())
    }

    @Test
    fun `el cursor codifica la fecha normalizada del ultimo elemento`() {
        val rows = listOf(
            row(id = 1, dateTakenMillis = 9_000L, dateAddedSeconds = 9),
            row(id = 2, dateTakenMillis = 0L, dateAddedSeconds = 8),
            row(id = 3, dateTakenMillis = null, dateAddedSeconds = 7)
        )

        val page = pageOf(rows, limit = 1, cursor = null)
        val cursor = MediaCursor.decode(page.nextCursor)

        assertEquals(9_000L, cursor?.dateTakenMillis)
        assertEquals(1L, cursor?.id)
        assertTrue(page.hasMore)

        val second = pageOf(rows, limit = 1, cursor = cursor)
        assertEquals(1, second.rows.size)
        assertEquals(8_000L, second.rows.first().dateTakenMillis)
    }
}
