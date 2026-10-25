package org.nexlibre.nexaphotos.domain.model

/** Estilo visual compartido: Google Material Expressive o iOS Classic. */
enum class UiStyle {
    GOOGLE,
    IOS
}

/** Modo de apariencia: seguir al sistema, claro u oscuro. */
enum class AppearanceMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * Preferencias de apariencia del usuario. La app mantiene la misma funcionalidad en ambos
 * estilos y ambos modos; solo cambian los tokens visuales.
 */
data class UserSettings(
    val uiStyle: UiStyle = UiStyle.GOOGLE,
    val appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    val useDynamicColors: Boolean = false
)
