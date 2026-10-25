package org.nexlibre.nexaphotos.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.nexlibre.nexaphotos.domain.model.AppearanceMode
import org.nexlibre.nexaphotos.domain.model.UiStyle
import org.nexlibre.nexaphotos.domain.model.UserSettings

/**
 * Tokens propios que complementan [MaterialTheme] para diferenciar el estilo Google Expressive
 * del estilo iOS Classic sin duplicar pantallas.
 */
data class NexaPhotosTokens(
    val uiStyle: UiStyle,
    val isDark: Boolean,
    val cardCornerRadius: Dp,
    val thumbnailCornerRadius: Dp,
    val gridSpacing: Dp,
    val selectionOverlayColor: Color,
    val useDividersInsteadOfCards: Boolean
)

val LocalNexaPhotosTokens = staticCompositionLocalOf {
    NexaPhotosTokens(
        uiStyle = UiStyle.GOOGLE,
        isDark = false,
        cardCornerRadius = 24.dp,
        thumbnailCornerRadius = 16.dp,
        gridSpacing = 4.dp,
        selectionOverlayColor = Color(0x33264F9E),
        useDividersInsteadOfCards = false
    )
}

/** Resolución determinista del modo oscuro; aislada para poder probarla sin Compose. */
fun resolveIsDark(mode: AppearanceMode, systemDark: Boolean): Boolean = when (mode) {
    AppearanceMode.SYSTEM -> systemDark
    AppearanceMode.LIGHT -> false
    AppearanceMode.DARK -> true
}

private val GoogleLightScheme = lightColorScheme(
    primary = Color(0xFF0B57D0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF386A20),
    secondaryContainer = Color(0xFFCDEBC0),
    background = Color(0xFFF8FAFD),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE1E3E9),
    onSurfaceVariant = Color(0xFF44474E),
    surfaceContainer = Color(0xFFF0F4F9),
    surfaceContainerHigh = Color(0xFFE8EDF3),
    error = Color(0xFFB3261E)
)

private val GoogleDarkScheme = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF062E6F),
    primaryContainer = Color(0xFF0842A0),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFFB1D18A),
    secondaryContainer = Color(0xFF354E1B),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
    surfaceContainer = Color(0xFF141518),
    surfaceContainerHigh = Color(0xFF1E1F23),
    error = Color(0xFFF2B8B5)
)

private val IosLightScheme = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F0FF),
    onPrimaryContainer = Color(0xFF003D82),
    secondary = Color(0xFF5856D6),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFF6C6C70),
    surfaceContainer = Color(0xFFF7F7FA),
    surfaceContainerHigh = Color(0xFFEFEFF4),
    error = Color(0xFFFF3B30)
)

private val IosDarkScheme = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0A2540),
    onPrimaryContainer = Color(0xFFBFD7F2),
    secondary = Color(0xFF5E5CE6),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF5F5F7),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFFAEAEB2),
    surfaceContainer = Color(0xFF121214),
    surfaceContainerHigh = Color(0xFF1C1C1E),
    error = Color(0xFFFF453A)
)

@Composable
fun NexaPhotosTheme(
    settings: UserSettings = UserSettings(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDark = resolveIsDark(settings.appearanceMode, isSystemInDarkTheme())
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val useDynamic = settings.useDynamicColors && settings.uiStyle == UiStyle.GOOGLE && supportsDynamic

    val colorScheme = when {
        useDynamic && isDark -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        settings.uiStyle == UiStyle.IOS -> if (isDark) IosDarkScheme else IosLightScheme
        else -> if (isDark) GoogleDarkScheme else GoogleLightScheme
    }

    val tokens = if (settings.uiStyle == UiStyle.IOS) {
        NexaPhotosTokens(
            uiStyle = UiStyle.IOS,
            isDark = isDark,
            cardCornerRadius = 12.dp,
            thumbnailCornerRadius = 6.dp,
            gridSpacing = 2.dp,
            selectionOverlayColor = if (isDark) Color(0x660A84FF) else Color(0x33007AFF),
            useDividersInsteadOfCards = true
        )
    } else {
        NexaPhotosTokens(
            uiStyle = UiStyle.GOOGLE,
            isDark = isDark,
            cardCornerRadius = 24.dp,
            thumbnailCornerRadius = 16.dp,
            gridSpacing = 4.dp,
            selectionOverlayColor = if (isDark) Color(0x66A8C7FA) else Color(0x330B57D0),
            useDividersInsteadOfCards = false
        )
    }

    val shapes = if (settings.uiStyle == UiStyle.IOS) {
        Shapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(18.dp)
        )
    } else {
        Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp)
        )
    }

    CompositionLocalProvider(LocalNexaPhotosTokens provides tokens) {
        MaterialTheme(colorScheme = colorScheme, shapes = shapes, content = content)
    }
}
