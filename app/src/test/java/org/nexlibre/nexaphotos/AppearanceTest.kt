package org.nexlibre.nexaphotos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nexlibre.nexaphotos.data.preferences.toAppearanceMode
import org.nexlibre.nexaphotos.data.preferences.toUiStyle
import org.nexlibre.nexaphotos.domain.model.AppearanceMode
import org.nexlibre.nexaphotos.domain.model.UiStyle
import org.nexlibre.nexaphotos.ui.theme.resolveIsDark

class AppearanceTest {

    @Test
    fun `modo sistema sigue al sistema`() {
        assertTrue(resolveIsDark(AppearanceMode.SYSTEM, systemDark = true))
        assertFalse(resolveIsDark(AppearanceMode.SYSTEM, systemDark = false))
    }

    @Test
    fun `modos explicitos ignoran el sistema`() {
        assertTrue(resolveIsDark(AppearanceMode.DARK, systemDark = false))
        assertFalse(resolveIsDark(AppearanceMode.LIGHT, systemDark = true))
    }

    @Test
    fun `preferencias desconocidas caen al valor por defecto`() {
        assertEquals(UiStyle.GOOGLE, "GOOGLE".toUiStyle())
        assertEquals(UiStyle.IOS, "IOS".toUiStyle())
        assertEquals(UiStyle.GOOGLE, "NO_EXISTE".toUiStyle())
        assertEquals(UiStyle.GOOGLE, null.toUiStyle())

        assertEquals(AppearanceMode.DARK, "DARK".toAppearanceMode())
        assertEquals(AppearanceMode.SYSTEM, "NO_EXISTE".toAppearanceMode())
        assertEquals(AppearanceMode.SYSTEM, null.toAppearanceMode())
    }
}
