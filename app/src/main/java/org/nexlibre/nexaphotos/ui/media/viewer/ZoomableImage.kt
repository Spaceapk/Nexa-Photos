package org.nexlibre.nexaphotos.ui.media.viewer

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import coil.ImageLoader
import coil.compose.AsyncImage
import org.nexlibre.nexaphotos.domain.model.MediaItem

/**
 * Límite de desplazamiento para que la imagen ampliada nunca salga de la pantalla.
 *
 * Con `ContentScale.Fit`, a escala [scale] la imagen ya no puede desplazarse más de
 * `(scale - 1) * tamaño / 2` en cada eje sin dejar hueco. A escala 1 el desplazamiento es 0.
 * Función pura para poder probarla en la JVM.
 */
fun clampPanOffset(offset: Offset, scale: Float, containerSize: Size): Offset {
    if (scale <= 1f || containerSize.width <= 0f || containerSize.height <= 0f) return Offset.Zero
    val maxX = (scale - 1f) * containerSize.width / 2f
    val maxY = (scale - 1f) * containerSize.height / 2f
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY)
    )
}

/** Escala resultante tras un gesto de pinza, acotada a [[MIN_SCALE], [MAX_SCALE]]. */
fun clampScale(scale: Float): Float = scale.coerceIn(MIN_SCALE, MAX_SCALE)

const val MIN_SCALE = 1f
const val MAX_SCALE = 6f
const val DOUBLE_TAP_SCALE = 2.5f

/**
 * Imagen con zoom por pinza, desplazamiento acotado y doble toque.
 *
 * Accesibilidad: expone acciones personalizadas ("Acercar", "Alejar", "Restablecer") que TalkBack
 * ofrece en su menú de acciones, además del doble toque. El desplazamiento se recorta para que la
 * imagen nunca quede fuera de la pantalla y vuelve a cero al restablecer.
 */
@Composable
fun ZoomableImage(
    item: MediaItem,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier
) {
    var scale by remember(item.uri) { mutableFloatStateOf(MIN_SCALE) }
    var offset by remember(item.uri) { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    val containerPx = with(density) { Size(containerSize.width.toFloat(), containerSize.height.toFloat()) }

    fun applyScale(newScale: Float) {
        scale = clampScale(newScale)
        offset = clampPanOffset(offset, scale, containerPx)
    }

    fun reset() {
        scale = MIN_SCALE
        offset = Offset.Zero
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(item.uri) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = clampScale(scale * zoom)
                    // Al reducir, se recorta el desplazamiento con la escala nueva.
                    val scaled = if (newScale > MIN_SCALE) offset + pan else Offset.Zero
                    scale = newScale
                    offset = clampPanOffset(scaled, newScale, containerPx)
                }
            }
            .pointerInput(item.uri) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > MIN_SCALE) {
                            reset()
                        } else {
                            scale = DOUBLE_TAP_SCALE
                            offset = clampPanOffset(offset, scale, containerPx)
                        }
                    }
                )
            }
            .semantics {
                contentDescription = item.name
                customActions = listOf(
                    CustomAccessibilityAction(label = "Acercar") {
                        applyScale(scale * 1.5f)
                        true
                    },
                    CustomAccessibilityAction(label = "Alejar") {
                        applyScale(scale / 1.5f)
                        true
                    },
                    CustomAccessibilityAction(label = "Restablecer zoom") {
                        reset()
                        true
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = item.uri,
            imageLoader = imageLoader,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}
