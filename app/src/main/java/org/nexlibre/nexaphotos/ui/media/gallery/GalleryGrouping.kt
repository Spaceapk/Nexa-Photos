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
