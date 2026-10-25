package org.nexlibre.nexaphotos.data.permissions

import org.nexlibre.nexaphotos.domain.model.MediaType

/**
 * Nivel de acceso concedido a la biblioteca multimedia.
 *
 * [PARTIAL] solo existe en Android 14+ y significa que el usuario eligió elementos concretos
 * (`READ_MEDIA_VISUAL_USER_SELECTED`): MediaStore devuelve únicamente esos elementos.
 */
enum class MediaAccess {
    NONE,
    PARTIAL,
    FULL
}

/**
 * Reglas de permisos por versión de Android, sin dependencias de `android.*` para poder
 * probarlas en la JVM.
 *
 * - API 29-32: `READ_EXTERNAL_STORAGE`.
 * - API 33+: `READ_MEDIA_IMAGES` y `READ_MEDIA_VIDEO` (se pueden conceder por separado).
 * - API 34+: además `READ_MEDIA_VISUAL_USER_SELECTED` para el acceso parcial.
 */
object MediaPermissionRules {

    const val READ_MEDIA_IMAGES = "android.permission.READ_MEDIA_IMAGES"
    const val READ_MEDIA_VIDEO = "android.permission.READ_MEDIA_VIDEO"
    const val READ_MEDIA_VISUAL_USER_SELECTED = "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
    const val READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE"

    const val API_33 = 33
    const val API_34 = 34

    /** Permisos que se solicitan en [sdkInt], en el orden en que deben pedirse. */
    fun requiredPermissions(sdkInt: Int): List<String> = when {
        sdkInt >= API_34 -> listOf(
            READ_MEDIA_IMAGES,
            READ_MEDIA_VIDEO,
            READ_MEDIA_VISUAL_USER_SELECTED
        )
        sdkInt >= API_33 -> listOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO)
        else -> listOf(READ_EXTERNAL_STORAGE)
    }

    /** Nivel de acceso efectivo a partir de los permisos concedidos. */
    fun resolveAccess(sdkInt: Int, granted: Set<String>): MediaAccess = when {
        sdkInt >= API_33 && (granted.contains(READ_MEDIA_IMAGES) || granted.contains(READ_MEDIA_VIDEO)) ->
            MediaAccess.FULL
        sdkInt >= API_34 && granted.contains(READ_MEDIA_VISUAL_USER_SELECTED) ->
            MediaAccess.PARTIAL
        sdkInt in API_33 until API_34 -> MediaAccess.NONE
        sdkInt < API_33 && granted.contains(READ_EXTERNAL_STORAGE) -> MediaAccess.FULL
        else -> MediaAccess.NONE
    }

    /**
     * Tipos que MediaStore puede devolver con los permisos actuales.
     * Con acceso parcial se permite consultar ambos tipos: el proveedor ya limita las filas a
     * los elementos elegidos por el usuario.
     */
    fun readableTypes(sdkInt: Int, granted: Set<String>): Set<MediaType> {
        if (sdkInt < API_33) {
            return if (granted.contains(READ_EXTERNAL_STORAGE)) MediaType.ALL else emptySet()
        }
        val types = buildSet {
            if (granted.contains(READ_MEDIA_IMAGES)) add(MediaType.IMAGE)
            if (granted.contains(READ_MEDIA_VIDEO)) add(MediaType.VIDEO)
        }
        if (types.isNotEmpty()) return types
        val partial = sdkInt >= API_34 && granted.contains(READ_MEDIA_VISUAL_USER_SELECTED)
        return if (partial) MediaType.ALL else emptySet()
    }

    /** `true` si el usuario puede ampliar el acceso parcial (Android 14+). */
    fun canRequestMoreAccess(sdkInt: Int, access: MediaAccess): Boolean =
        sdkInt >= API_34 && access == MediaAccess.PARTIAL
}
