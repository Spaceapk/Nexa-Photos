package org.nexlibre.nexaphotos.ui.media.gallery

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.nexlibre.nexaphotos.domain.model.MediaItem
import org.nexlibre.nexaphotos.domain.model.MediaType

/** Contenido a compartir, ya resuelto por tipo MIME común. */
data class SharePayload(
    val uris: List<String>,
    val mimeType: String,
    val multiple: Boolean
)

/**
 * Construcción de intents de compartir del sistema. Solo se concede permiso de lectura temporal
 * sobre los URI elegidos; nunca se exportan ni se copian los archivos.
 */
object MediaShare {

    /** Payload para [items]; `null` si no hay nada que compartir. */
    fun payload(items: List<MediaItem>): SharePayload? {
        if (items.isEmpty()) return null
        val imagesOnly = items.all { it.type == MediaType.IMAGE }
        val videosOnly = items.all { it.type == MediaType.VIDEO }
        val mimeType = when {
            imagesOnly -> "image/*"
            videosOnly -> "video/*"
            else -> "*/*"
        }
        return SharePayload(
            uris = items.map { it.uri },
            mimeType = mimeType,
            multiple = items.size > 1
        )
    }

    /** Intent `ACTION_SEND` o `ACTION_SEND_MULTIPLE` según el número de elementos. */
    fun intent(payload: SharePayload): Intent =
        if (payload.multiple) {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = payload.mimeType
                putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM,
                    ArrayList(payload.uris.map(Uri::parse))
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = payload.mimeType
                putExtra(Intent.EXTRA_STREAM, Uri.parse(payload.uris.first()))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

    /** Abre el selector del sistema; no hace nada si no hay payload. */
    fun share(context: Context, items: List<MediaItem>, chooserTitle: String = "Compartir") {
        val payload = payload(items) ?: return
        val chooser = Intent.createChooser(intent(payload), chooserTitle)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(chooser)
    }
}
