package org.nexlibre.nexaphotos.data.permissions

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import org.nexlibre.nexaphotos.domain.model.MediaType

/** Lectura del estado de permisos; abstraído para poder simularlo en pruebas de ViewModel. */
interface MediaPermissionChecker {
    val sdkInt: Int
    fun access(): MediaAccess
    fun readableTypes(): Set<MediaType>
    fun requiredPermissions(): Array<String> = MediaPermissionRules.requiredPermissions(sdkInt).toTypedArray()
}

/** Implementación real con [ContextCompat.checkSelfPermission]. */
class AndroidMediaPermissionChecker(context: Context) : MediaPermissionChecker {

    private val appContext = context.applicationContext

    override val sdkInt: Int get() = Build.VERSION.SDK_INT

    override fun access(): MediaAccess =
        MediaPermissionRules.resolveAccess(sdkInt, grantedPermissions())

    override fun readableTypes(): Set<MediaType> =
        MediaPermissionRules.readableTypes(sdkInt, grantedPermissions())

    private fun grantedPermissions(): Set<String> =
        MediaPermissionRules.requiredPermissions(sdkInt)
            .filter { permission ->
                ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
            }
            .toSet()
}
