package org.nexlibre.nexaphotos.data.permissions

import org.nexlibre.nexaphotos.domain.model.MediaType

/**
 * Acceso que la app tiene a **un tipo** de contenido multimedia.
 *
 * - [NONE]: no hay permiso para ese tipo; sus elementos no deben mostrarse ni anunciarse como
 *   disponibles.
 * - [PARTIAL]: solo los elementos que el usuario eligió en el diálogo de Android 14+
 *   (`READ_MEDIA_VISUAL_USER_SELECTED`).
 * - [FULL]: permiso completo para ese tipo (`READ_MEDIA_IMAGES` o `READ_MEDIA_VIDEO`).
 */
enum class MediaTypeAccess {
    NONE,
    PARTIAL,
    FULL;

    val canRead: Boolean get() = this != NONE
}

/**
 * Estado de acceso a la biblioteca, resuelto **por tipo**.
 *
 * Android 13+ permite conceder fotos y videos por separado, y Android 14+ permite además que un
 * tipo quede con acceso completo mientras el otro solo tiene los elementos seleccionados
 * (por ejemplo: fotos "Permitir todo" y videos "Seleccionar videos"). Un único valor agregado no
 * puede representar ese caso sin mentir, así que el estado se guarda por tipo y se resume en
 * [overall] solo para la UI.
 */
data class MediaAccessState(
    val images: MediaTypeAccess = MediaTypeAccess.NONE,
    val videos: MediaTypeAccess = MediaTypeAccess.NONE
) {
    fun forType(type: MediaType): MediaTypeAccess = when (type) {
        MediaType.IMAGE -> images
        MediaType.VIDEO -> videos
    }

    /** Tipos que MediaStore puede devolver con los permisos actuales. */
    val readableTypes: Set<MediaType>
        get() = buildSet {
            if (images.canRead) add(MediaType.IMAGE)
            if (videos.canRead) add(MediaType.VIDEO)
        }

    /** Tipos sin ningún acceso: la UI debe explicar que no se muestran. */
    val missingTypes: Set<MediaType>
        get() = buildSet {
            if (!images.canRead) add(MediaType.IMAGE)
            if (!videos.canRead) add(MediaType.VIDEO)
        }

    /** Tipos con acceso limitado a los elementos elegidos por el usuario. */
    val partialTypes: Set<MediaType>
        get() = buildSet {
            if (images == MediaTypeAccess.PARTIAL) add(MediaType.IMAGE)
            if (videos == MediaTypeAccess.PARTIAL) add(MediaType.VIDEO)
        }

    /**
     * Resumen para la UI: [MediaAccess.FULL] solo cuando **ambos** tipos tienen acceso completo.
     * Cualquier combinación mixta (un tipo completo y otro parcial o ausente) es
     * [MediaAccess.PARTIAL], nunca completo.
     */
    val overall: MediaAccess
        get() = when {
            images == MediaTypeAccess.NONE && videos == MediaTypeAccess.NONE -> MediaAccess.NONE
            images == MediaTypeAccess.FULL && videos == MediaTypeAccess.FULL -> MediaAccess.FULL
            else -> MediaAccess.PARTIAL
        }

    /** `true` si algún tipo tiene acceso solo a elementos seleccionados. */
    val hasPartialAccess: Boolean get() = partialTypes.isNotEmpty()
}

/**
 * Nivel de acceso **agregado** a la biblioteca. Se conserva por compatibilidad con el código y
 * las pruebas existentes; para decisiones por tipo usa [MediaAccessState].
 *
 * [PARTIAL] significa "no es acceso completo": puede ser acceso solo a elementos seleccionados
 * o una mezcla de tipos (por ejemplo, fotos completas y videos sin permiso).
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
 * - API 29-32: `READ_EXTERNAL_STORAGE` cubre fotos y videos.
 * - API 33: `READ_MEDIA_IMAGES` y `READ_MEDIA_VIDEO` se conceden por separado.
 * - API 34+: además `READ_MEDIA_VISUAL_USER_SELECTED` (acceso solo a lo seleccionado).
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

    /**
     * Permisos que faltan para [state]: solo los que corresponden a tipos sin acceso completo.
     * Sirve para pedir de nuevo únicamente lo denegado, incluido el acceso parcial de un tipo.
     */
    fun missingPermissions(sdkInt: Int, state: MediaAccessState): List<String> {
        if (sdkInt < API_33) {
            return if (state.images == MediaTypeAccess.FULL && state.videos == MediaTypeAccess.FULL) {
                emptyList()
            } else {
                listOf(READ_EXTERNAL_STORAGE)
            }
        }
        val missing = buildList {
            if (state.images != MediaTypeAccess.FULL) add(READ_MEDIA_IMAGES)
            if (state.videos != MediaTypeAccess.FULL) add(READ_MEDIA_VIDEO)
        }
        if (missing.isEmpty()) return emptyList()
        return if (sdkInt >= API_34) {
            missing + READ_MEDIA_VISUAL_USER_SELECTED
        } else {
            missing
        }
    }

    /**
     * Acceso efectivo por tipo a partir de los permisos concedidos.
     *
     * Un tipo tiene acceso completo solo si se le concedió su permiso específico. En Android 14+,
     * `READ_MEDIA_VISUAL_USER_SELECTED` da acceso parcial a los tipos que no lo tienen completo.
     */
    fun accessState(sdkInt: Int, granted: Set<String>): MediaAccessState {
        if (sdkInt < API_33) {
            val full = if (granted.contains(READ_EXTERNAL_STORAGE)) MediaTypeAccess.FULL else MediaTypeAccess.NONE
            return MediaAccessState(images = full, videos = full)
        }

        val selectedAccess = if (sdkInt >= API_34 && granted.contains(READ_MEDIA_VISUAL_USER_SELECTED)) {
            MediaTypeAccess.PARTIAL
        } else {
            MediaTypeAccess.NONE
        }

        fun access(fullPermission: String): MediaTypeAccess = when {
            granted.contains(fullPermission) -> MediaTypeAccess.FULL
            else -> selectedAccess
        }

        return MediaAccessState(
            images = access(READ_MEDIA_IMAGES),
            videos = access(READ_MEDIA_VIDEO)
        )
    }

    /** Nivel agregado a partir de los permisos concedidos. */
    fun resolveAccess(sdkInt: Int, granted: Set<String>): MediaAccess =
        accessState(sdkInt, granted).overall

    /** Tipos que MediaStore puede devolver con los permisos actuales. */
    fun readableTypes(sdkInt: Int, granted: Set<String>): Set<MediaType> =
        accessState(sdkInt, granted).readableTypes

    /** `true` si el usuario puede ampliar un acceso parcial o conceder un tipo que falta. */
    fun canRequestMoreAccess(sdkInt: Int, state: MediaAccessState): Boolean =
        sdkInt >= API_34 && state.overall != MediaAccess.FULL

    /** Variante de compatibilidad basada en el nivel agregado. */
    fun canRequestMoreAccess(sdkInt: Int, access: MediaAccess): Boolean =
        sdkInt >= API_34 && access != MediaAccess.FULL
}
