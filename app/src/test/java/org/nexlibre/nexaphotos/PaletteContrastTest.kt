package org.nexlibre.nexaphotos

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nexlibre.nexaphotos.ui.theme.NexaPalettes

/**
 * Auditoría automática de contraste (WCAG 2.1 AA) de la paleta estática de la app.
 *
 * Calcula el contraste real de cada par texto/fondo que la interfaz usa y falla si baja de
 * 4.5:1 en texto normal o de 3:1 en texto grande y elementos gráficos. Las parejas con colores
 * dinámicos (Material You) no se pueden auditar aquí porque dependen del fondo de pantalla del
 * usuario; en ese caso el tema fuerza fondos negros y superficies neutras en modo oscuro.
 */
class PaletteContrastTest {

    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double {
            val c = value.toDouble()
            return if (c <= 0.04045) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    private fun contrast(foreground: Color, background: Color): Double {
        val a = luminance(foreground)
        val b = luminance(background)
        val hi = maxOf(a, b)
        val lo = minOf(a, b)
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun assertText(scheme: String, label: String, fg: Color, bg: Color) {
        val ratio = contrast(fg, bg)
        assertTrue(
            "$scheme · $label: contraste $ratio por debajo de 4.5:1",
            ratio >= 4.5
        )
    }

    private fun assertGraphic(scheme: String, label: String, fg: Color, bg: Color) {
        val ratio = contrast(fg, bg)
        assertTrue(
            "$scheme · $label: contraste $ratio por debajo de 3:1",
            ratio >= 3.0
        )
    }

    private fun auditTextPairs(name: String, scheme: ColorScheme) {
        assertText(name, "onBackground/background", scheme.onBackground, scheme.background)
        assertText(name, "onSurface/surface", scheme.onSurface, scheme.surface)
        assertText(name, "onSurfaceVariant/surface", scheme.onSurfaceVariant, scheme.surface)
        assertText(name, "onSurfaceVariant/surfaceVariant", scheme.onSurfaceVariant, scheme.surfaceVariant)
        assertText(name, "onSurfaceVariant/surfaceContainer", scheme.onSurfaceVariant, scheme.surfaceContainer)
        assertText(name, "onSurfaceVariant/surfaceContainerHigh", scheme.onSurfaceVariant, scheme.surfaceContainerHigh)
        assertText(name, "onPrimary/primary", scheme.onPrimary, scheme.primary)
        assertText(name, "onPrimaryContainer/primaryContainer", scheme.onPrimaryContainer, scheme.primaryContainer)
        assertText(name, "onSecondary/secondary", scheme.onSecondary, scheme.secondary)
        assertText(name, "onSecondaryContainer/secondaryContainer", scheme.onSecondaryContainer, scheme.secondaryContainer)
        assertText(name, "onError/error", scheme.onError, scheme.error)
    }

    private fun auditGraphicPairs(name: String, scheme: ColorScheme) {
        assertGraphic(name, "primary/background", scheme.primary, scheme.background)
        assertGraphic(name, "primary/surface", scheme.primary, scheme.surface)
        assertGraphic(name, "outline/background", scheme.outline, scheme.background)
        assertGraphic(name, "outline/surface", scheme.outline, scheme.surface)
    }

    @Test
    fun `google claro cumple AA`() {
        val scheme = NexaPalettes.GoogleLight
        auditTextPairs("Google claro", scheme)
        auditGraphicPairs("Google claro", scheme)
    }

    @Test
    fun `google oscuro cumple AA y usa negro AMOLED`() {
        val scheme = NexaPalettes.GoogleDark
        auditTextPairs("Google oscuro", scheme)
        auditGraphicPairs("Google oscuro", scheme)
        assertTrue(
            "El fondo oscuro debe ser negro puro (AMOLED)",
            scheme.background == NexaPalettes.AmoledBlack && scheme.surface == NexaPalettes.AmoledBlack
        )
    }

    @Test
    fun `ios claro cumple AA`() {
        val scheme = NexaPalettes.IosLight
        auditTextPairs("iOS claro", scheme)
        auditGraphicPairs("iOS claro", scheme)
    }

    @Test
    fun `ios oscuro cumple AA con superficies carbon`() {
        val scheme = NexaPalettes.IosDark
        auditTextPairs("iOS oscuro", scheme)
        auditGraphicPairs("iOS oscuro", scheme)
        assertTrue(
            "El fondo oscuro de iOS debe ser negro puro",
            scheme.background == NexaPalettes.AmoledBlack && scheme.surface == NexaPalettes.AmoledBlack
        )
        assertTrue(
            "Las superficies deben ser gris carbón, no el morado por defecto de Material 3",
            scheme.surfaceContainer == NexaPalettes.CharcoalLow &&
                scheme.surfaceContainerHigh == NexaPalettes.Charcoal
        )
    }

    @Test
    fun `el acento ios mantiene contraste sobre caron y negro`() {
        val accent = NexaPalettes.IosBlue
        assertGraphic("iOS acento", "sobre negro", accent, NexaPalettes.AmoledBlack)
        assertGraphic("iOS acento", "sobre carbón", accent, NexaPalettes.Charcoal)
        assertText("iOS acento", "texto blanco encima", Color.White, accent)
    }
}
