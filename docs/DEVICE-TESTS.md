# Pruebas en dispositivo

Nexa Photos declara `minSdk 29` (Android 10). Las pruebas siguientes deben ejecutarse a mano en
cada versión soportada; ninguna de ellas modifica los archivos originales.

## Matriz mínima

| Versión | Permisos | Escenarios |
|---|---|---|
| Android 10 (API 29) | `READ_EXTERNAL_STORAGE` | Biblioteca con fotos y videos, cuadrícula, visor, compartir |
| Android 12 (API 31/32) | `READ_EXTERNAL_STORAGE` | Igual que 29 |
| Android 13 (API 33) | `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` | Conceder solo un tipo de permiso |
| Android 14+ (API 34/35) | `READ_MEDIA_VISUAL_USER_SELECTED` | Acceso parcial (selección), ampliar selección y volver a denegar |

## Checklist

1. **Sin permiso**: la app muestra el estado vacío con un botón para conceder acceso; no hay crash.
2. **Solo fotos** (Android 13+): la pestaña de videos informa que falta acceso a video.
3. **Acceso parcial** (Android 14+): aparece el aviso de acceso parcial y solo se listan los
   elementos elegidos.
4. **Cuadrícula**: scroll fluido con >2 000 elementos, sin bloquear la interfaz; la paginación
   carga más al llegar al final.
5. **Álbumes**: nombres y conteos coinciden con la app de galería del sistema.
6. **Búsqueda**: filtra por nombre y carpeta en menos de un segundo con la biblioteca completa.
7. **Selección múltiple y compartir**: `ACTION_SEND_MULTIPLE` abre el selector del sistema con
   las URI correctas y permiso de lectura temporal.
8. **Visor**: zoom con doble toque y pinch; desplazamiento sin rebotes; video se reproduce, pausa
   y reanuda.
9. **Favoritos**: marcar y desmarcar persiste tras reiniciar la app (Room).
10. **Temas**: todo lo anterior funciona igual en Google Expressive y iOS Classic, claro y oscuro.
11. **Rotación y multitarea**: el estado de selección y scroll se conserva o se reinicia sin pérdida.
