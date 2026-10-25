package org.nexlibre.nexaphotos

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nexlibre.nexaphotos.domain.model.MediaType
import org.nexlibre.nexaphotos.domain.model.UiStyle
import org.nexlibre.nexaphotos.ui.media.gallery.describeMediaItem
import org.nexlibre.nexaphotos.ui.media.gallery.formatItemDate
import org.nexlibre.nexaphotos.ui.theme.resolveUseDynamicColors

class VisualRulesTest {

    private val utc: ZoneId = ZoneOffset.UTC

    @Test
    fun `los colores dinamicos solo aplican a Google desde Android 12`() {
        // Google + solicitado + API 31+ => sí
        assertTrue(resolveUseDynamicColors(UiStyle.GOOGLE, requested = true, sdkInt = 31))
        assertTrue(resolveUseDynamicColors(UiStyle.GOOGLE, requested = true, sdkInt = 35))
        // Google, pero sin solicitarlo o sin soporte
        assertFalse(resolveUseDynamicColors(UiStyle.GOOGLE, requested = false, sdkInt = 35))
        assertFalse(resolveUseDynamicColors(UiStyle.GOOGLE, requested = true, sdkInt = 30))
        assertFalse(resolveUseDynamicColors(UiStyle.GOOGLE, requested = true, sdkInt = 29))
        // iOS Classic usa siempre su paleta estática
        assertFalse(resolveUseDynamicColors(UiStyle.IOS, requested = true, sdkInt = 35))
    }

    @Test
    fun `describe una foto con fecha`() {
        val item = mediaItem(id = 1, dateTakenMillis = millis(2026, 10, 24))
        val description = describeMediaItem(item, isSelected = false, zoneId = utc, locale = Locale.forLanguageTag("es-ES"))

        assertTrue(description.startsWith("Foto"))
        assertTrue(description.contains("2026"))
        assertFalse(description.contains("seleccionado"))
        assertFalse(description.contains("favorito"))
    }

    @Test
    fun `describe un video con duracion`() {
        val item = mediaItem(id = 2, type = MediaType.VIDEO, dateTakenMillis = millis(2026, 10, 24))
        val description = describeMediaItem(item, isSelected = false, zoneId = utc, locale = Locale.forLanguageTag("es-ES"))

        assertTrue(description.startsWith("Video"))
        assertTrue(description.contains("1:05"))
    }

    @Test
    fun `la descripcion anuncia seleccion y favorito`() {
        val item = mediaItem(id = 3, isFavorite = true, dateTakenMillis = millis(2026, 1, 2))
        val description = describeMediaItem(item, isSelected = true, zoneId = utc, locale = Locale.forLanguageTag("es-ES"))

        assertTrue(description.contains("favorito"))
        assertTrue(description.contains("seleccionado"))
    }

    @Test
    fun `la fecha del elemento usa el idioma pedido`() {
        val millis = millis(2026, 10, 24)

        val spanish = formatItemDate(millis, utc, Locale.forLanguageTag("es-ES"))
        val english = formatItemDate(millis, utc, Locale.forLanguageTag("en-US"))

        assertTrue(spanish.contains("2026"))
        assertTrue(english.contains("2026"))
        assertFalse(spanish == english)
    }

    private fun millis(year: Int, month: Int, day: Int): Long =
        LocalDateTime.of(year, month, day, 12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
}
