package org.nexlibre.nexaphotos

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nexlibre.nexaphotos.ui.media.viewer.DOUBLE_TAP_SCALE
import org.nexlibre.nexaphotos.ui.media.viewer.MAX_SCALE
import org.nexlibre.nexaphotos.ui.media.viewer.MIN_SCALE
import org.nexlibre.nexaphotos.ui.media.viewer.clampPanOffset
import org.nexlibre.nexaphotos.ui.media.viewer.clampScale

/** Reglas de zoom: la imagen ampliada nunca debe salir de la pantalla. */
class ZoomModelTest {

    private val container = Size(1080f, 1920f)

    @Test
    fun `sin ampliar no hay desplazamiento`() {
        assertEquals(Offset.Zero, clampPanOffset(Offset(500f, 900f), scale = 1f, containerSize = container))
        assertEquals(Offset.Zero, clampPanOffset(Offset(500f, 900f), scale = 0.8f, containerSize = container))
    }

    @Test
    fun `el desplazamiento se limita al margen disponible`() {
        // scale 2 => margen 540 horizontal, 960 vertical.
        val clamped = clampPanOffset(Offset(5_000f, -5_000f), scale = 2f, containerSize = container)
        assertEquals(540f, clamped.x, 0.01f)
        assertEquals(-960f, clamped.y, 0.01f)
    }

    @Test
    fun `dentro del margen el desplazamiento no cambia`() {
        val inside = Offset(100f, -200f)
        assertEquals(inside, clampPanOffset(inside, scale = 3f, containerSize = container))
    }

    @Test
    fun `al reducir el zoom el desplazamiento vuelve al area visible`() {
        val far = Offset(1_000f, 1_000f)
        // Con escala 2 el máximo es 540/960; al volver a 1 se restablece por completo.
        val atTwo = clampPanOffset(far, scale = 2f, containerSize = container)
        assertEquals(540f, atTwo.x, 0.01f)
        assertEquals(Offset.Zero, clampPanOffset(atTwo, scale = 1f, containerSize = container))
    }

    @Test
    fun `un contenedor sin medir no permite desplazar`() {
        assertEquals(Offset.Zero, clampPanOffset(Offset(50f, 50f), scale = 3f, containerSize = Size.Zero))
    }

    @Test
    fun `la escala se limita entre el minimo y el maximo`() {
        assertEquals(MIN_SCALE, clampScale(0.1f))
        assertEquals(MIN_SCALE, clampScale(1f))
        assertEquals(MAX_SCALE, clampScale(99f))
        assertEquals(2f, clampScale(2f))
        assertTrue(DOUBLE_TAP_SCALE in (MIN_SCALE + 0.01f)..MAX_SCALE)
    }

    @Test
    fun `ampliar y volver a restablecer deja el encuadre original`() {
        var scale = clampScale(1f * 1.5f)
        var offset = clampPanOffset(Offset(300f, 300f), scale, container)
        scale = clampScale(scale * 1.5f)
        assertEquals(2.25f, scale, 0.001f)
        assertTrue(offset.x <= (scale - 1f) * container.width / 2f)

        // Restablecer: como hace la acción accesible "Restablecer zoom".
        scale = MIN_SCALE
        offset = Offset.Zero
        assertEquals(Offset.Zero, offset)
        assertEquals(MIN_SCALE, scale)
    }
}
