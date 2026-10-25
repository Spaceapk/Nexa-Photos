package org.nexlibre.nexaphotos.data.media

import java.net.URLDecoder
import java.net.URLEncoder

/** Utilidades de URI compartidas por el adaptador MediaStore y los favoritos. */
object MediaUris {

    /**
     * Normaliza una URI de contenido para usarla como clave en la tabla de favoritos:
     * recorta espacios y unifica el esquema vacío (`//media/...` -> `content://media/...`).
     * No decodifica ni altera el identificador del elemento.
     */
    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return trimmed
        return when {
            trimmed.startsWith("content://", ignoreCase = true) -> "content://" + trimmed.substring("content://".length)
            trimmed.startsWith("//") -> "content:$trimmed"
            else -> trimmed
        }
    }

    /** Codifica un `bucket_id` para usarlo como cursor de álbum seguro en URL/estado de UI. */
    fun encodeAlbumId(raw: String): String = URLEncoder.encode(raw, "UTF-8")

    /** Decodifica un id de álbum previamente codificado con [encodeAlbumId]. */
    fun decodeAlbumId(raw: String): String = runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw)

    /** Extensión del nombre de archivo en minúsculas, o cadena vacía. */
    fun extensionOf(name: String): String {
        val dot = name.lastIndexOf('.')
        return if (dot >= 0 && dot < name.length - 1) name.substring(dot + 1).lowercase() else ""
    }
}
