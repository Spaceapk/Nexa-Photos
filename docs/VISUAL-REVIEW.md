# Revisión visual y de accesibilidad (0.1.0)

Revisión de la interfaz existente siguiendo las recomendaciones de Gemini. **No se duplicaron
pantallas ni se replanteó la arquitectura**: se ajustaron colores, formas, espaciado, estados
visuales y semántica sobre las mismas pantallas, y ambos estilos siguen compartiendo código.

> Alcance: lo que se describe aquí se **midió o se ejecutó**. No se ha hecho una auditoría de
> accesibilidad con lector de pantalla ni con herramientas externas (Accessibility Scanner,
> TalkBack en dispositivo); eso queda en `docs/DEVICE-TESTS.md`.

## Aplicado

| Recomendación | Qué se hizo |
|---|---|
| Un solo conjunto de pantallas para ambos estilos | Sin cambios estructurales: `GalleryScreen`, `MediaViewerScreen`, `MediaGrid` y `ZoomableImage` son únicos. Solo cambian tokens (`NexaPhotosTokens`: radios 24/16 dp vs 12/6 dp, espaciado 4 vs 2 dp, esquinas y colores de selección). |
| Colores dinámicos solo en Google Expressive y desde API 31 | `resolveUseDynamicColors(style, requested, sdkInt)` (función pura, probada en `VisualRulesTest`). Se añadió el interruptor en el diálogo de apariencia, visible únicamente con `UiStyle.GOOGLE` y `SDK_INT >= 31`. En iOS Classic no existe. |
| Modo oscuro con fondos AMOLED negros | `#000000` en `background` y `surface` para ambos estilos. **Con color dinámico en oscuro** se copia el esquema del sistema y se fuerzan fondos negros y superficies neutras (`CharcoalLowest…High`), que antes se quedaban con el gris del sistema. |
| iOS Classic: paleta estática, superficies gris carbón sobre negro | Paleta fija: `#000000` de fondo, carbón `#141518` / `#1C1C1E` / `#26262A` en contenedores, contornos neutros. Se eliminaron los morados por defecto de Material 3 (`surfaceContainer*`, `secondaryContainer`, `outline`). |
| Estados seleccionados sin depender solo del color | Tres señales simultáneas: borde de 3 dp con color de selección, icono de verificación (relleno vs contorno) y `stateDescription`/`selected` para TalkBack. Las descripciones añaden "favorito" y "seleccionado" como texto. |
| Objetivos táctiles ≥ 48 dp | Todos los controles son `IconButton`, `FilterChip`, `Button` o `FilledTonalButton` de Material 3, que aplican `minimumInteractiveComponentSize` (48 dp de área táctil aunque el elemento visual mida menos). La barra de selección tiene `heightIn(min = 56.dp)`. **No se midió con Accessibility Scanner**: es una garantía del componente de Material 3, no una medición propia. |
| Descripciones útiles para TalkBack | `describeMediaItem()` (probado): "Foto, 24 oct 2026" / "Video, 1:05, 24 oct 2026", más "favorito" y "seleccionado". Los álbumes anuncian nombre y número de elementos. Las miniaturas internas usan `contentDescription = null` para no duplicar la lectura (`mergeDescendants`). El indicador de carga se anuncia como "Cargando biblioteca". |
| Texto ampliado hasta 200 % | Chips de sección en fila desplazable, diálogo de apariencia en columna desplazable con controles apilados, contador de selección con `weight` y elipsis, estado vacío desplazable verticalmente, títulos con `maxLines`. |
| Rendimiento en móviles modestos | Sin blur ni sombras (el visor usa un degradado lineal, no desenfoque). Se **desactivó el crossfade de Coil**: en listas largas animaba en cada celda y en cada reutilización. Indicadores de carga simples (`CircularProgressIndicator` + "Cargando…"). |
| Respetar la escala de animación del sistema | No se añadió ninguna lectura manual de ajustes globales (`Settings.Global.ANIMATOR_DURATION_SCALE`). Las únicas animaciones propias eran crossfades; el resto son del sistema/Compose y respetan la escala. |
| No añadir "Eliminar" al visor | El visor sigue teniendo solo cerrar, favorito y compartir. No existe ninguna operación destructiva en el código. |

## Descartado (y por qué)

| Recomendación | Motivo |
|---|---|
| Nueva paleta completa "de marca" para ambos estilos | Gemini Business preparará especificaciones visuales por separado. Aquí solo se corrigieron los valores que **no** cumplían contraste y los morados heredados de Material 3, sin rediseñar la identidad. |
| Tipografía propia | Requiere añadir fuentes al APK; no había indicación concreta de familia ni pesos. Se mantiene la tipografía Material 3 (que escala con el tamaño de fuente del sistema). |
| `FlowRow` para los grupos de chips | La versión de Compose del proyecto no la expone de forma estable; se resolvió con filas/columnas desplazables, sin APIs experimentales nuevas. |
| Animaciones de transición entre cuadrícula y visor | Se prefirió no añadir animaciones costosas mientras se arrastra en móviles modestos; el visor se abre sin transición animada. |
| Leer la escala de animación global para acortar animaciones | Prohibido por la propia recomendación: se respeta la escala del sistema sin consultar ajustes globales. |

## Contraste: medición real

`PaletteContrastTest` (pruebas JVM, ejecutadas en CI) calcula WCAG 2.1 con la fórmula de luminancia
relativa sobre las cuatro paletas y falla el build si algo baja de 4.5:1 (texto normal) o 3:1 (texto
grande y elementos gráficos). Parejas comprobadas por esquema: `onBackground/background`,
`onSurface/surface`, `onSurfaceVariant` sobre `surface`, `surfaceVariant`, `surfaceContainer` y
`surfaceContainerHigh`, `onPrimary/primary`, `onPrimaryContainer/primaryContainer`,
`onSecondary/secondary`, `onSecondaryContainer/secondaryContainer`, `onError/error`, y como
gráficos `primary` y `outline` sobre fondo y superficie. Además verifica que el fondo oscuro sea
negro puro y que las superficies de iOS sean carbón.

Cambio de valor medido:

| Estilo | Antes | Contraste con texto blanco | Después | Contraste |
|---|---|---|---|---|
| iOS Classic | `#007AFF` (claro) | 4.02:1 ❌ (mínimo 4.5) | `#1170DE` | **4.78:1** ✅ |
| iOS Classic | `#0A84FF` (oscuro) | 3.65:1 ❌ | `#1170DE` | **4.78:1** ✅ |

`#1170DE` sobre negro da 4.40:1 y sobre carbón `#1C1C1E` 3.56:1, así que también sirve para iconos y
acentos sobre fondo oscuro (mínimo 3:1). El resto de valores de la paleta ya cumplían y no se tocaron
salvo para sustituir los morados por defecto de Material 3.

**No se auditaron** los colores dinámicos (dependen del fondo de pantalla del usuario) ni el
contraste real de las miniaturas (contenido del usuario, sin texto encima).

## Qué falta verificar en dispositivo

1. TalkBack real: orden de foco en la cuadrícula, anuncio de selección y del visor.
2. Texto al 200 % en pantallas pequeñas (revisar recortes en la barra superior y el diálogo).
3. Accessibility Scanner: tamaños táctiles efectivos y etiquetas faltantes.
4. Legibilidad del degradado del visor sobre fotos claras y oscuras.
