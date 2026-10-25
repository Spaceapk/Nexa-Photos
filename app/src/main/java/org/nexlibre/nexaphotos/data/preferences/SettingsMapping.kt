package org.nexlibre.nexaphotos.data.preferences

import org.nexlibre.nexaphotos.domain.model.AppearanceMode
import org.nexlibre.nexaphotos.domain.model.UiStyle

/** Mapeo tolerante de preferencias persistidas a enums de dominio (valores desconocidos → defecto). */
internal fun String?.toUiStyle(): UiStyle =
    UiStyle.entries.firstOrNull { it.name == this } ?: UiStyle.GOOGLE

internal fun String?.toAppearanceMode(): AppearanceMode =
    AppearanceMode.entries.firstOrNull { it.name == this } ?: AppearanceMode.SYSTEM
