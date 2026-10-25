package org.nexlibre.nexaphotos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nexlibre.nexaphotos.data.permissions.MediaAccess
import org.nexlibre.nexaphotos.data.permissions.MediaAccessState
import org.nexlibre.nexaphotos.data.permissions.MediaPermissionRules
import org.nexlibre.nexaphotos.data.permissions.MediaTypeAccess
import org.nexlibre.nexaphotos.domain.model.MediaType

class MediaPermissionsTest {

    private val images = MediaPermissionRules.READ_MEDIA_IMAGES
    private val video = MediaPermissionRules.READ_MEDIA_VIDEO
    private val selected = MediaPermissionRules.READ_MEDIA_VISUAL_USER_SELECTED
    private val storage = MediaPermissionRules.READ_EXTERNAL_STORAGE

    // --- permisos solicitados por versión ---

    @Test
    fun `android 10 y 12 piden almacenamiento externo`() {
        assertEquals(listOf(storage), MediaPermissionRules.requiredPermissions(29))
        assertEquals(listOf(storage), MediaPermissionRules.requiredPermissions(32))
    }

    @Test
    fun `android 13 pide imagenes y video`() {
        assertEquals(listOf(images, video), MediaPermissionRules.requiredPermissions(33))
    }

    @Test
    fun `android 14 pide tambien el acceso parcial`() {
        assertEquals(listOf(images, video, selected), MediaPermissionRules.requiredPermissions(34))
        assertEquals(MediaPermissionRules.requiredPermissions(34), MediaPermissionRules.requiredPermissions(35))
    }

    // --- acceso por tipo ---

    @Test
    fun `sin permisos no hay acceso ni tipos legibles`() {
        val state = MediaPermissionRules.accessState(33, emptySet())
        assertEquals(MediaTypeAccess.NONE, state.images)
        assertEquals(MediaTypeAccess.NONE, state.videos)
        assertEquals(MediaAccess.NONE, state.overall)
        assertEquals(emptySet<MediaType>(), state.readableTypes)
        assertEquals(setOf(MediaType.IMAGE, MediaType.VIDEO), state.missingTypes)

        assertEquals(MediaAccess.NONE, MediaPermissionRules.resolveAccess(29, emptySet()))
        assertEquals(emptySet<MediaType>(), MediaPermissionRules.readableTypes(29, emptySet()))
    }

    @Test
    fun `android 10 con permiso es acceso completo a ambos tipos`() {
        val granted = setOf(storage)
        val state = MediaPermissionRules.accessState(29, granted)
        assertEquals(MediaTypeAccess.FULL, state.images)
        assertEquals(MediaTypeAccess.FULL, state.videos)
        assertEquals(MediaAccess.FULL, MediaPermissionRules.resolveAccess(29, granted))
        assertEquals(MediaType.ALL, MediaPermissionRules.readableTypes(29, granted))
    }

    @Test
    fun `android 13 solo imagenes no es acceso completo y los videos se informan como ausentes`() {
        val state = MediaPermissionRules.accessState(33, setOf(images))

        assertEquals(MediaTypeAccess.FULL, state.images)
        assertEquals(MediaTypeAccess.NONE, state.videos)
        // Regresión: antes se presentaba como FULL y los videos desaparecían sin explicación.
        assertEquals(MediaAccess.PARTIAL, state.overall)
        assertEquals(setOf(MediaType.IMAGE), state.readableTypes)
        assertEquals(setOf(MediaType.VIDEO), state.missingTypes)
        assertFalse(state.hasPartialAccess)
        assertEquals(listOf(video), MediaPermissionRules.missingPermissions(33, state))
    }

    @Test
    fun `android 13 solo videos deja las fotos como tipo ausente`() {
        val state = MediaPermissionRules.accessState(33, setOf(video))

        assertEquals(MediaTypeAccess.NONE, state.images)
        assertEquals(MediaTypeAccess.FULL, state.videos)
        assertEquals(MediaAccess.PARTIAL, state.overall)
        assertEquals(setOf(MediaType.VIDEO), state.readableTypes)
        assertEquals(setOf(MediaType.IMAGE), state.missingTypes)
        assertEquals(listOf(images), MediaPermissionRules.missingPermissions(33, state))
    }

    @Test
    fun `android 13 con ambos permisos es acceso completo`() {
        val state = MediaPermissionRules.accessState(33, setOf(images, video))
        assertEquals(MediaAccess.FULL, state.overall)
        assertEquals(MediaType.ALL, state.readableTypes)
        assertTrue(state.missingTypes.isEmpty())
        assertTrue(MediaPermissionRules.missingPermissions(33, state).isEmpty())
    }

    // --- Android 14+: acceso parcial y combinaciones mixtas ---

    @Test
    fun `android 14 acceso parcial en ambos tipos`() {
        val state = MediaPermissionRules.accessState(34, setOf(selected))
        assertEquals(MediaTypeAccess.PARTIAL, state.images)
        assertEquals(MediaTypeAccess.PARTIAL, state.videos)
        assertEquals(MediaAccess.PARTIAL, state.overall)
        assertTrue(state.hasPartialAccess)
        assertEquals(MediaType.ALL, state.readableTypes)
        assertTrue(state.missingTypes.isEmpty())
        assertTrue(MediaPermissionRules.canRequestMoreAccess(34, state))
    }

    @Test
    fun `android 14 un tipo completo y el otro seleccionado`() {
        // Fotos "Permitir todo" + videos "Seleccionar videos".
        val state = MediaPermissionRules.accessState(34, setOf(images, selected))

        assertEquals(MediaTypeAccess.FULL, state.images)
        assertEquals(MediaTypeAccess.PARTIAL, state.videos)
        assertEquals(MediaAccess.PARTIAL, state.overall)
        assertEquals(setOf(MediaType.VIDEO), state.partialTypes)
        assertTrue(state.missingTypes.isEmpty())
        assertEquals(MediaType.ALL, state.readableTypes)
        // Falta el video completo: se puede pedir de nuevo.
        assertEquals(
            listOf(video, selected),
            MediaPermissionRules.missingPermissions(34, state)
        )
        assertTrue(MediaPermissionRules.canRequestMoreAccess(34, state))
    }

    @Test
    fun `android 14 videos completos y fotos seleccionadas`() {
        val state = MediaPermissionRules.accessState(34, setOf(video, selected))

        assertEquals(MediaTypeAccess.PARTIAL, state.images)
        assertEquals(MediaTypeAccess.FULL, state.videos)
        assertEquals(MediaAccess.PARTIAL, state.overall)
        assertEquals(setOf(MediaType.IMAGE), state.partialTypes)
        assertEquals(listOf(images, selected), MediaPermissionRules.missingPermissions(34, state))
    }

    @Test
    fun `android 14 tipo completo y el otro sin permiso`() {
        val state = MediaPermissionRules.accessState(34, setOf(images))

        assertEquals(MediaTypeAccess.FULL, state.images)
        assertEquals(MediaTypeAccess.NONE, state.videos)
        assertEquals(MediaAccess.PARTIAL, state.overall)
        assertEquals(setOf(MediaType.VIDEO), state.missingTypes)
        assertEquals(listOf(video, selected), MediaPermissionRules.missingPermissions(34, state))
    }

    @Test
    fun `android 14 con ambos completos no ofrece ampliar`() {
        val state = MediaPermissionRules.accessState(34, setOf(images, video, selected))

        assertEquals(MediaAccess.FULL, state.overall)
        assertTrue(state.missingTypes.isEmpty())
        assertTrue(state.partialTypes.isEmpty())
        assertTrue(MediaPermissionRules.missingPermissions(34, state).isEmpty())
        assertFalse(MediaPermissionRules.canRequestMoreAccess(34, state))
    }

    @Test
    fun `el acceso parcial no existe en android 13`() {
        val state = MediaPermissionRules.accessState(33, setOf(selected))
        assertEquals(MediaAccess.NONE, state.overall)
        assertEquals(emptySet<MediaType>(), state.readableTypes)
        assertFalse(MediaPermissionRules.canRequestMoreAccess(33, state))

        assertEquals(MediaAccess.NONE, MediaPermissionRules.resolveAccess(33, setOf(selected)))
    }

    @Test
    fun `el estado vacio por defecto no concede nada`() {
        val state = MediaAccessState()
        assertEquals(MediaAccess.NONE, state.overall)
        assertEquals(emptySet<MediaType>(), state.readableTypes)
        assertEquals(setOf(MediaType.IMAGE, MediaType.VIDEO), state.missingTypes)
        assertFalse(state.hasPartialAccess)
        assertFalse(state.forType(MediaType.IMAGE).canRead)
    }
}
