package org.nexlibre.nexaphotos.ui.media.gallery

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.decode.VideoFrameDecoder

/**
 * Cargador de miniaturas de Coil con decodificación de fotogramas de video.
 *
 * Las miniaturas se leen directamente de los URI de MediaStore (nunca se copian archivos) y se
 * cachean en memoria y disco por Coil.
 *
 * Sin `crossfade`: en listas largas la animación se repite en cada celda y en cada reutilización,
 * lo que castiga a los móviles modestos durante el desplazamiento. La carga se indica con el
 * marcador estático de cada celda.
 */
@Composable
fun rememberGalleryImageLoader(): ImageLoader {
    val context: Context = LocalContext.current.applicationContext
    return remember(context) {
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(false)
            .build()
    }
}
