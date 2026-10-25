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
    val selectionBorderColor: Color,
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
        selectionBorderColor = Color(0xFF0B57D0),
        useDividersInsteadOfCards = false
    )
}

/** Resolución determinista del modo oscuro; aislada para poder probarla sin Compose. */
fun resolveIsDark(mode: AppearanceMode, systemDark: Boolean): Boolean = when (mode) {
    AppearanceMode.SYSTEM -> systemDark
    AppearanceMode.LIGHT -> false
    AppearanceMode.DARK -> true
}

/**
 * Los colores dinámicos (Material You) solo se aplican al estilo Google Expressive y desde
 * Android 12 (API 31). El estilo iOS Classic usa siempre su paleta estática.
 */
fun resolveUseDynamicColors(style: UiStyle, requested: Boolean, sdkInt: Int): Boolean =
    requested && style == UiStyle.GOOGLE && sdkInt >= Build.VERSION_CODES.S

/**
 * Paleta estática de la app, expuesta para poder comprobar el contraste en pruebas unitarias.
 * Los valores están elegidos para cumplir WCAG AA: 4.5:1 en texto normal y 3:1 en texto grande
 * y elementos gráficos. Ver `PaletteContrastTest`.
 */
internal object NexaPalettes {

    /** Negro AMOLED puro, usado en modo oscuro por ambos estilos. */
    val AmoledBlack = Color(0xFF000000)

    /** Gris carbón de superficies elevadas en modo oscuro (ambos estilos). */
    val CharcoalLowest = Color(0xFF0E0E10)
    val CharcoalLow = Color(0xFF141518)
    val Charcoal = Color(0xFF1C1C1E)
    val CharcoalHigh = Color(0xFF26262A)

    /** Acento iOS estático: 4.78:1 con texto blanco, 4.40:1 sobre negro y 3.56:1 sobre carbón. */
    val IosBlue = Color(0xFF1170DE)

    val GoogleLight = lightColorScheme(
        primary = Color(0xFF0B57D0),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD3E3FD),
        onPrimaryContainer = Color(0xFF041E49),
        secondary = Color(0xFF386A20),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCDEBC0),
        onSecondaryContainer = Color(0xFF0C2000),
        tertiary = Color(0xFF6C4E00),
        background = Color(0xFFF8FAFD),
        onBackground = Color(0xFF1B1B1F),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1B1B1F),
        surfaceVariant = Color(0xFFE1E3E9),
        onSurfaceVariant = Color(0xFF44474E),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF4F6FB),
        surfaceContainer = Color(0xFFF0F4F9),
        surfaceContainerHigh = Color(0xFFE8EDF3),
        surfaceContainerHighest = Color(0xFFE1E3E9),
        outline = Color(0xFF73777F),
        outlineVariant = Color(0xFFC3C7CF),
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF)
    )

    val GoogleDark = darkColorScheme(
        primary = Color(0xFFA8C7FA),
        onPrimary = Color(0xFF062E6F),
        primaryContainer = Color(0xFF0842A0),
        onPrimaryContainer = Color(0xFFD3E3FD),
        secondary = Color(0xFFB1D18A),
        onSecondary = Color(0xFF1B3B00),
        secondaryContainer = Color(0xFF354E1B),
        onSecondaryContainer = Color(0xFFCDEBC0),
        tertiary = Color(0xFFE9C46A),
        background = AmoledBlack,
        onBackground = Color(0xFFE3E3E3),
        surface = AmoledBlack,
        onSurface = Color(0xFFE3E3E3),
        surfaceVariant = Color(0xFF44474E),
        onSurfaceVariant = Color(0xFFC4C6D0),
        // Negro puro: modo AMOLED sin paneles grises en el fondo de la cuadrícula.
        surfaceContainerLowest = AmoledBlack,
        surfaceContainerLow = CharcoalLowest,
        surfaceContainer = CharcoalLow,
        surfaceContainerHigh = Charcoal,
        surfaceContainerHighest = CharcoalHigh,
        outline = Color(0xFF8E9099),
        outlineVariant = Color(0xFF44474E),
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410)
    )

    val IosLight = lightColorScheme(
        primary = IosBlue,
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE5F0FF),
        onPrimaryContainer = Color(0xFF003D82),
        secondary = Color(0xFF5856D6),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE9E8FF),
        onSecondaryContainer = Color(0xFF1B1A43),
        tertiary = Color(0xFFC2410C),
        background = Color(0xFFFFFFFF),
        onBackground = Color(0xFF111111),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF111111),
        surfaceVariant = Color(0xFFF2F2F7),
        onSurfaceVariant = Color(0xFF5E5E63),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFAFAFC),
        surfaceContainer = Color(0xFFF7F7FA),
        surfaceContainerHigh = Color(0xFFEFEFF4),
        surfaceContainerHighest = Color(0xFFE5E5EA),
        outline = Color(0xFF7C7C80),
        outlineVariant = Color(0xFFD8D8DD),
        error = Color(0xFFD70015),
        onError = Color(0xFFFFFFFF)
    )

    val IosDark = darkColorScheme(
        primary = IosBlue,
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF0A2E52),
        onPrimaryContainer = Color(0xFFCFE0FF),
        secondary = Color(0xFF9C9AFF),
        onSecondary = Color(0xFF1B1A43),
        secondaryContainer = Color(0xFF2C2B45),
        onSecondaryContainer = Color(0xFFE9E8FF),
        tertiary = Color(0xFFF0A375),
        background = AmoledBlack,
        onBackground = Color(0xFFF5F5F7),
        surface = AmoledBlack,
        onSurface = Color(0xFFF5F5F7),
        surfaceVariant = Charcoal,
        onSurfaceVariant = Color(0xFFB8B8BE),
        // Superficies gris carbón discretas sobre fondo negro.
        surfaceContainerLowest = AmoledBlack,
        surfaceContainerLow = CharcoalLowest,
        surfaceContainer = CharcoalLow,
        surfaceContainerHigh = Charcoal,
        surfaceContainerHighest = CharcoalHigh,
        outline = Color(0xFF8C8C92),
        outlineVariant = Color(0xFF3A3A3E),
        error = Color(0xFFFF6B60),
        onError = Color(0xFF2B0000)
    )
}

@Composable
fun NexaPhotosTheme(
    settings: UserSettings = UserSettings(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDark = resolveIsDark(settings.appearanceMode, isSystemInDarkTheme())
    val useDynamic = resolveUseDynamicColors(settings.uiStyle, settings.useDynamicColors, Build.VERSION.SDK_INT)

    val baseScheme = when {
        useDynamic && isDark -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        settings.uiStyle == UiStyle.IOS -> if (isDark) NexaPalettes.IosDark else NexaPalettes.IosLight
        else -> if (isDark) NexaPalettes.GoogleDark else NexaPalettes.GoogleLight
    }

    // Aunque los colores sean dinámicos, en modo oscuro los fondos se fuerzan a negro AMOLED y el
    // resto de superficies a gris neutro, como el resto del sistema visual de la app.
    val colorScheme = if (useDynamic && isDark) {
        baseScheme.copy(
            background = NexaPalettes.AmoledBlack,
            surface = NexaPalettes.AmoledBlack,
            surfaceContainerLowest = NexaPalettes.AmoledBlack,
            surfaceContainerLow = NexaPalettes.CharcoalLowest,
            surfaceContainer = NexaPalettes.CharcoalLow,
            surfaceContainerHigh = NexaPalettes.Charcoal,
            surfaceContainerHighest = NexaPalettes.CharcoalHigh
        )
    } else {
        baseScheme
    }

    val tokens = if (settings.uiStyle == UiStyle.IOS) {
        NexaPhotosTokens(
            uiStyle = UiStyle.IOS,
            isDark = isDark,
            cardCornerRadius = 12.dp,
            thumbnailCornerRadius = 6.dp,
            gridSpacing = 2.dp,
            selectionOverlayColor = if (isDark) Color(0x590A84FF) else Color(0x33007AFF),
            selectionBorderColor = if (isDark) Color(0xFF6FB1FF) else NexaPalettes.IosBlue,
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
            selectionBorderColor = colorScheme.primary,
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
