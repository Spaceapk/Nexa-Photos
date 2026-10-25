# Nexa Photos

Galería local de fotos y videos para Android 10+, escrita en Kotlin con Jetpack Compose,
Material 3, MediaStore y Room.

- **Paquete:** `org.nexlibre.nexaphotos`
- **Versión:** 0.1.0 (`versionName`), `versionCode` 1
- **Mínimo:** Android 10 (API 29) · **Objetivo:** API 35

## Principios

- Los originales son de **solo lectura**: la app no borra ni modifica fotos o videos.
- **Sin red**: no se declara `INTERNET`, no hay cuentas, nube ni servicios de Google.
- **Sin claves de firma** en el repositorio: el build de depuración usa la firma debug estándar.
- Dos estilos visuales (Google Expressive e iOS Classic) y claro/oscuro con la misma funcionalidad.

## Estructura

```
app/src/main/java/org/nexlibre/nexaphotos/
├── domain/model/         MediaItem, MediaType, paginación, álbumes, filtros, UserSettings
├── domain/repository/    MediaRepository y FavoriteRepository (contratos)
├── data/media/           Frontera MediaStore, cursores y utilidades de URI
├── data/local/           Entidades, DAO, base Room y repositorio de favoritos
├── data/preferences/     DataStore de apariencia
├── ui/theme/             Selector Google Expressive / iOS Classic y claro-oscuro
├── ui/media/gallery/     Cuadrícula, álbumes, búsqueda y selección
├── ui/media/viewer/      Visor con zoom y reproducción de video
├── ui/                   Raíz y estado de apariencia
├── MainActivity.kt
└── NexaPhotosApplication.kt
```

## Compilar

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

La integración continua (`.github/workflows/android.yml`) ejecuta pruebas unitarias, lint y
`assembleDebug`, verifica `applicationId`/`versionName` y publica el APK como artefacto.

La documentación de dependencias está en [`docs/DEPENDENCIES.md`](docs/DEPENDENCIES.md) y la
verificación en [`docs/VERIFICATION.md`](docs/VERIFICATION.md).

## Licencia

Ver [`LICENSE`](LICENSE) (GPL-3.0).
