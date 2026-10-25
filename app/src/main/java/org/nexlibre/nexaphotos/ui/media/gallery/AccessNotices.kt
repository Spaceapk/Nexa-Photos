package org.nexlibre.nexaphotos.ui.media.gallery

import org.nexlibre.nexaphotos.domain.model.MediaType

/**
 * Aviso de acceso que la galería muestra sobre la cuadrícula.
 *
 * @param message explicación para el usuario (qué no se ve y por qué).
 * @param actionLabel texto del botón que abre el diálogo del sistema.
 */
data class AccessNotice(
    val message: String,
    val actionLabel: String
)

/**
 * Construye el aviso a partir del acceso por tipo.
 *
 * Reglas:
 * - Si no hay ningún tipo legible no hay aviso: la pantalla completa de permisos se encarga.
 * - Si falta un tipo, se dice explícitamente cuál y que solo se muestran los otros; nunca se
 *   ocultan videos (o fotos) en silencio.
 * - Si hay acceso parcial, se explica que solo se ven los elementos elegidos, incluso cuando el
 *   otro tipo sí está completo.
 *
 * Función pura para poder probarla en la JVM.
 */
fun accessNotice(
    readableTypes: Set<MediaType>,
    missingTypes: Set<MediaType>,
    partialTypes: Set<MediaType>
): AccessNotice? {
    if (readableTypes.isEmpty()) return null

    val missingDescription = when (missingTypes) {
        setOf(MediaType.VIDEO) -> "Falta el permiso de videos: solo se muestran fotos."
        setOf(MediaType.IMAGE) -> "Falta el permiso de fotos: solo se muestran videos."
        else -> null
    }

    if (missingDescription != null) {
        val action = if (MediaType.VIDEO in missingTypes) "Permitir videos" else "Permitir fotos"
        val partialSuffix = if (partialTypes.isNotEmpty()) {
            " Del tipo permitido solo ves los elementos que elegiste."
        } else {
            ""
        }
        return AccessNotice(message = missingDescription + partialSuffix, actionLabel = action)
    }

    if (partialTypes.isEmpty()) return null

    val message = when (partialTypes) {
        setOf(MediaType.IMAGE, MediaType.VIDEO) ->
            "Acceso parcial: solo ves las fotos y los videos que elegiste."
        setOf(MediaType.VIDEO) -> "Videos con acceso parcial: solo ves los que elegiste."
        setOf(MediaType.IMAGE) -> "Fotos con acceso parcial: solo ves las que elegiste."
        else -> "Acceso parcial: solo ves los elementos que elegiste."
    }
    return AccessNotice(message = message, actionLabel = "Elegir más")
}

/** Etiqueta de la sección cronológica según los tipos legibles (no promete lo que no hay). */
fun timelineLabel(readableTypes: Set<MediaType>): String = when {
    readableTypes.containsAll(MediaType.ALL) -> "Fotos y videos"
    readableTypes == setOf(MediaType.IMAGE) -> "Fotos"
    readableTypes == setOf(MediaType.VIDEO) -> "Videos"
    else -> "Biblioteca"
}

/** Texto del estado vacío cuando faltan tipos por permiso. */
fun emptyLibraryMessage(readableTypes: Set<MediaType>, missingTypes: Set<MediaType>): String = when {
    readableTypes.isEmpty() -> "Sin acceso a la biblioteca"
    missingTypes.isNotEmpty() && readableTypes == setOf(MediaType.IMAGE) -> "No hay fotos visibles"
    missingTypes.isNotEmpty() && readableTypes == setOf(MediaType.VIDEO) -> "No hay videos visibles"
    else -> "No hay fotos ni videos"
}
