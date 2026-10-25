package org.nexlibre.nexaphotos

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nexlibre.nexaphotos.domain.model.MediaItem
import org.nexlibre.nexaphotos.ui.media.gallery.SelectionState
import org.nexlibre.nexaphotos.ui.media.gallery.formatDayLabel
import org.nexlibre.nexaphotos.ui.media.gallery.formatDuration
import org.nexlibre.nexaphotos.ui.media.gallery.groupByDay

class GalleryUiLogicTest {

    private val utc: ZoneId = ZoneOffset.UTC

    private fun itemAt(id: Long, dateTime: LocalDateTime): MediaItem = MediaItem(
        uri = "content://media/external/images/media/$id",
        name = "foto-$id",
        mimeType = "image/jpeg",
        type = org.nexlibre.nexaphotos.domain.model.MediaType.IMAGE,
        sizeBytes = 10L,
        dateTakenMillis = dateTime.toInstant(ZoneOffset.UTC).toEpochMilli(),
        dateAddedMillis = dateTime.toInstant(ZoneOffset.UTC).toEpochMilli(),
        width = 1,
        height = 1
    )

    @Test
    fun `agrupa por dia conservando el orden`() {
        val items = listOf(
            itemAt(1, LocalDateTime.of(2026, 10, 25, 18, 0)),
            itemAt(2, LocalDateTime.of(2026, 10, 25, 9, 0)),
            itemAt(3, LocalDateTime.of(2026, 10, 24, 23, 30)),
            itemAt(4, LocalDateTime.of(2026, 10, 20, 12, 0))
        )

        val groups = groupByDay(items, utc)

        assertEquals(3, groups.size)
        // Dentro del día se conserva el orden descendente recibido: 18:00 antes que 09:00.
        assertEquals(listOf(1L, 2L), groups[0].items.map { it.uri.substringAfterLast('/').toLong() })
        assertEquals(1, groups[1].items.size)
        assertEquals(1, groups[2].items.size)
    }

    @Test
    fun `lista vacia no produce grupos`() {
        assertTrue(groupByDay(emptyList(), utc).isEmpty())
    }

    @Test
    fun `etiqueta hoy ayer y fecha`() {
        val today = LocalDate.of(2026, 10, 25)
        val todayMillis = today.atStartOfDay(utc).toInstant().toEpochMilli()
        val yesterdayMillis = today.minusDays(1).atStartOfDay(utc).toInstant().toEpochMilli()
        val oldMillis = today.minusDays(10).atStartOfDay(utc).toInstant().toEpochMilli()

        assertEquals("Hoy", formatDayLabel(todayMillis, today, utc))
        assertEquals("Ayer", formatDayLabel(yesterdayMillis, today, utc))
        assertFalse(formatDayLabel(oldMillis, today, utc).isEmpty())
    }

    @Test
    fun `duracion legible`() {
        assertEquals("", formatDuration(0))
        assertEquals("0:05", formatDuration(5_000))
        assertEquals("1:05", formatDuration(65_000))
        assertEquals("1:02:05", formatDuration(3_725_000))
    }

    @Test
    fun `seleccion alterna y conserva solo visibles`() {
        var selection = SelectionState()
        assertFalse(selection.isActive)

        selection = selection.toggle("a")
        assertTrue(selection.contains("a"))
        assertEquals(1, selection.count)

        selection = selection.toggle("b")
        assertEquals(2, selection.count)

        selection = selection.toggle("a")
        assertFalse(selection.contains("a"))

        selection = selection.add(listOf("c", "d"))
        assertEquals(3, selection.count)

        selection = selection.retainOnly(listOf("b"))
        assertEquals(setOf("b"), selection.selected)

        selection = selection.clear()
        assertFalse(selection.isActive)
    }
}
