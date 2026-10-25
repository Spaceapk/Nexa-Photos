package org.nexlibre.nexaphotos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nexlibre.nexaphotos.data.permissions.MediaAccess
import org.nexlibre.nexaphotos.data.permissions.MediaPermissionRules
import org.nexlibre.nexaphotos.domain.model.MediaType

class MediaPermissionsTest {

    @Test
    fun `android 10 y 12 piden almacenamiento externo`() {
        assertEquals(
            listOf(MediaPermissionRules.READ_EXTERNAL_STORAGE),
            MediaPermissionRules.requiredPermissions(29)
        )
        assertEquals(
            listOf(MediaPermissionRules.READ_EXTERNAL_STORAGE),
            MediaPermissionRules.requiredPermissions(32)
        )
    }

    @Test
    fun `android 13 pide imagenes y video`() {
        assertEquals(
            listOf(
                MediaPermissionRules.READ_MEDIA_IMAGES,
                MediaPermissionRules.READ_MEDIA_VIDEO
            ),
            MediaPermissionRules.requiredPermissions(33)
        )
    }

    @Test
    fun `android 14 pide tambien el acceso parcial`() {
        assertEquals(
            listOf(
                MediaPermissionRules.READ_MEDIA_IMAGES,
                MediaPermissionRules.READ_MEDIA_VIDEO,
                MediaPermissionRules.READ_MEDIA_VISUAL_USER_SELECTED
            ),
            MediaPermissionRules.requiredPermissions(34)
        )
        assertEquals(
            MediaPermissionRules.requiredPermissions(34),
            MediaPermissionRules.requiredPermissions(35)
        )
    }

    @Test
    fun `sin permisos no hay acceso ni tipos legibles`() {
        assertEquals(MediaAccess.NONE, MediaPermissionRules.resolveAccess(33, emptySet()))
        assertEquals(emptySet<MediaType>(), MediaPermissionRules.readableTypes(33, emptySet()))

        assertEquals(MediaAccess.NONE, MediaPermissionRules.resolveAccess(29, emptySet()))
        assertEquals(emptySet<MediaType>(), MediaPermissionRules.readableTypes(29, emptySet()))
    }

    @Test
    fun `android 10 con permiso es acceso completo`() {
        val granted = setOf(MediaPermissionRules.READ_EXTERNAL_STORAGE)
        assertEquals(MediaAccess.FULL, MediaPermissionRules.resolveAccess(29, granted))
        assertEquals(MediaType.ALL, MediaPermissionRules.readableTypes(29, granted))
    }

    @Test
    fun `android 13 permite conceder solo un tipo`() {
        val granted = setOf(MediaPermissionRules.READ_MEDIA_IMAGES)
        assertEquals(MediaAccess.FULL, MediaPermissionRules.resolveAccess(33, granted))
        assertEquals(setOf(MediaType.IMAGE), MediaPermissionRules.readableTypes(33, granted))
    }

    @Test
    fun `android 14 acceso parcial solo con visual user selected`() {
        val granted = setOf(MediaPermissionRules.READ_MEDIA_VISUAL_USER_SELECTED)
        assertEquals(MediaAccess.PARTIAL, MediaPermissionRules.resolveAccess(34, granted))
        assertEquals(MediaType.ALL, MediaPermissionRules.readableTypes(34, granted))
        assertTrue(MediaPermissionRules.canRequestMoreAccess(34, MediaAccess.PARTIAL))
    }

    @Test
    fun `el acceso parcial no existe en android 13`() {
        val granted = setOf(MediaPermissionRules.READ_MEDIA_VISUAL_USER_SELECTED)
        assertEquals(MediaAccess.NONE, MediaPermissionRules.resolveAccess(33, granted))
        assertEquals(emptySet<MediaType>(), MediaPermissionRules.readableTypes(33, granted))
        assertFalse(MediaPermissionRules.canRequestMoreAccess(33, MediaAccess.PARTIAL))
    }

    @Test
    fun `el acceso completo no ofrece ampliar`() {
        assertFalse(MediaPermissionRules.canRequestMoreAccess(34, MediaAccess.FULL))
        assertFalse(MediaPermissionRules.canRequestMoreAccess(34, MediaAccess.NONE))
    }
}
