package org.nexlibre.nexaphotos.data.media

/**
 * Cursor opaco de paginación basado en el orden cronológico de MediaStore.
 *
 * Formato serializado: `v1:<dateTakenMillis>:<id>`. El orden de la consulta es
 * `DATE_TAKEN DESC, _ID DESC`, así que el cursor guarda el último elemento entregado y la
 * página siguiente empieza estrictamente después de él. Se incluye `_ID` como desempate para
 * no perder ni duplicar elementos con la misma marca de tiempo.
 */
data class MediaCursor(
    val dateTakenMillis: Long,
    val id: Long
) {
    fun encode(): String = "$PREFIX$SEPARATOR$dateTakenMillis$SEPARATOR$id"

    companion object {
        private const val PREFIX = "v1"
        private const val SEPARATOR = ":"

        /** Decodifica un cursor; devuelve `null` si está vacío o mal formado. */
        fun decode(raw: String?): MediaCursor? {
            val value = raw?.trim().orEmpty()
            if (value.isEmpty()) return null
            val parts = value.split(SEPARATOR)
            if (parts.size != 3 || parts[0] != PREFIX) return null
            val date = parts[1].toLongOrNull() ?: return null
            val id = parts[2].toLongOrNull() ?: return null
            if (date < 0L || id < 0L) return null
            return MediaCursor(dateTakenMillis = date, id = id)
        }
    }
}
