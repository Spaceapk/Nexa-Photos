package org.nexlibre.nexaphotos.data.permissions

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import org.nexlibre.nexaphotos.domain.model.MediaType

/** Lectura del estado de permisos; abstraído para poder simularlo en pruebas de ViewModel. */
interface MediaPermissionChecker {
    val sdkInt: Int

    /** Acceso por tipo (fotos y videos se resuelven por separado). */
    fun accessState(): MediaAccessState

    /** Nivel agregado; se deriva de [accessState] para no tener dos fuentes de verdad. */
    fun access(): MediaAccess = accessState().overall

    fun readableTypes(): Set<MediaType> = accessState().readableTypes

    fun requiredPermissions(): Array<String> =
        MediaPermissionRules.requiredPermissions(sdkInt).toTypedArray()

    /** Permisos que faltan para completar el acceso actual. */
    fun missingPermissions(): Array<String> =
        MediaPermissionRules.missingPermissions(sdkInt, accessState()).toTypedArray()
}

/** Implementación real con [ContextCompat.checkSelfPermission]. */
class AndroidMediaPermissionChecker(context: Context) : MediaPermissionChecker {

    private val appContext = context.applicationContext

    override val sdkInt: Int get() = Build.VERSION.SDK_INT

    override fun accessState(): MediaAccessState =
        MediaPermissionRules.accessState(sdkInt, grantedPermissions())

    private fun grantedPermissions(): Set<String> =
        MediaPermissionRules.requiredPermissions(sdkInt)
            .filter { permission ->
                ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
            }
            .toSet()
}
