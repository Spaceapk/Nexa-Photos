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
2. **Solo fotos** (Android 13+): la sección se llama "Fotos", aparece el aviso "Falta el permiso
   de videos…" y el botón "Permitir videos" pide **solo** ese permiso. La app no debe decir en
   ningún momento que tiene acceso completo, ni debe mostrar videos vacíos sin explicación.
2b. **Solo videos** (Android 13+): mismo comportamiento simétrico (aviso de fotos).
2c. **Mixto** (Android 14+): fotos con "Permitir todo" y videos con "Seleccionar videos" → aviso de
   videos con acceso parcial; las fotos se ven completas y solo los videos elegidos.
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
9b. **Álbumes grandes**: en una biblioteca de más de 100 000 elementos, los conteos deben mostrar
   "N+ elementos" (nunca un total exacto falso).
9c. **Zoom accesible**: con TalkBack, el menú de acciones del visor ofrece "Acercar", "Alejar" y
   "Restablecer zoom"; al ampliar y arrastrar, la imagen nunca deja un hueco vacío en la pantalla.
9d. **Paginación**: recorrer una lista grande de principio a fin sin elementos repetidos ni saltos,
   incluyendo fotos antiguas con fecha de captura desconocida (0 o negativa en el proveedor).
10. **Temas**: todo lo anterior funciona igual en Google Expressive y iOS Classic, claro y oscuro.
11. **Rotación y multitarea**: el estado de selección y scroll se conserva o se reinicia sin pérdida.
