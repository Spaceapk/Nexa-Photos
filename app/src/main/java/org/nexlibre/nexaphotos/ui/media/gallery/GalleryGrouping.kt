package org.nexlibre.nexaphotos.ui.media.gallery

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import org.nexlibre.nexaphotos.domain.model.MediaItem

/** Grupo de elementos de un mismo día, ya ordenado cronológicamente de forma descendente. */
data class MediaDayGroup(
    val dayStartMillis: Long,
    val items: List<MediaItem>
)

/**
 * Agrupa por día local conservando el orden recibido (la consulta ya viene descendente).
 * Función pura para poder probarla sin Android.
 */
fun groupByDay(items: List<MediaItem>, zoneId: ZoneId = ZoneId.systemDefault()): List<MediaDayGroup> {
    val groups = LinkedHashMap<Long, MutableList<MediaItem>>()
    items.forEach { item ->
        val dayStart = Instant.ofEpochMilli(item.dateTakenMillis)
            .atZone(zoneId)
            .toLocalDate()
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        groups.getOrPut(dayStart) { mutableListOf() }.add(item)
    }
    return groups.map { (dayStart, groupItems) -> MediaDayGroup(dayStart, groupItems.toList()) }
}

/**
 * Etiqueta del día: "Hoy", "Ayer" o la fecha localizada.
 */
fun formatDayLabel(
    dayStartMillis: Long,
    today: LocalDate,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): String {
    val date = Instant.ofEpochMilli(dayStartMillis).atZone(zoneId).toLocalDate()
    return when (date) {
        today -> "Hoy"
        today.minusDays(1) -> "Ayer"
        else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date)
    }
}

private val itemDateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/** Fecha localizada corta de un elemento (para descripciones de accesibilidad y hojas de detalle). */
fun formatItemDate(
    millis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): String = itemDateFormatter
    .withLocale(locale)
    .format(Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate())

/**
 * Descripción para TalkBack de una miniatura: tipo, duración si es video, fecha y estado.
 * No depende del color ni del overlay para transmitir la selección.
 */
fun describeMediaItem(
    item: MediaItem,
    isSelected: Boolean = false,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): String {
    val parts = mutableListOf(if (item.isVideo) "Video" else "Foto")
    if (item.isVideo) formatDuration(item.durationMillis).takeIf { it.isNotEmpty() }?.let(parts::add)
    parts += formatItemDate(item.dateTakenMillis, zoneId, locale)
    if (item.isFavorite) parts += "favorito"
    if (isSelected) parts += "seleccionado"
    return parts.joinToString(", ")
}

/** Duración legible para la insignia de los videos. */
fun formatDuration(durationMillis: Long): String {
    if (durationMillis <= 0L) return ""
    val totalSeconds = durationMillis / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
