# Dependencias

Todas las dependencias son de código abierto, se resuelven desde `google()` y `mavenCentral()`
y no requieren servicios de Google, cuentas ni claves. Ninguna añade permiso de red.

## Base (0.1.0)

| Dependencia | Versión | Licencia | Uso |
|---|---|---|---|
| `androidx.core:core-ktx` | 1.15.0 | Apache-2.0 | Utilidades del SDK |
| `androidx.activity:activity-compose` | 1.9.3 | Apache-2.0 | `ComponentActivity` + Compose |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.8.7 | Apache-2.0 | Coroutines y ciclo de vida |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.8.7 | Apache-2.0 | `collectAsStateWithLifecycle` |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.8.7 | Apache-2.0 | ViewModels en Compose |
| `androidx.compose:compose-bom` | 2024.10.01 | Apache-2.0 | Alineación de versiones Compose |
| `androidx.compose.ui:ui`, `ui-graphics`, `ui-tooling-preview` | BOM | Apache-2.0 | UI y previsualización |
| `androidx.compose.material3:material3` | BOM | Apache-2.0 | Material 3 |
| `androidx.compose.material:material-icons-extended` | BOM | Apache-2.0 | Iconografía |
| `androidx.room:room-runtime`, `room-ktx`, `room-compiler` | 2.6.1 | Apache-2.0 | Favoritos locales |
| `androidx.datastore:datastore-preferences` | 1.1.1 | Apache-2.0 | Preferencias de apariencia |
| `junit:junit` | 4.13.2 | EPL-1.0 | Pruebas unitarias (solo test) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.9.0 | Apache-2.0 | Pruebas de coroutines (solo test) |
| `androidx.test.ext:junit`, `espresso-core`, `ui-test-junit4` | 1.2.1 / 3.6.1 / BOM | Apache-2.0 | Pruebas instrumentadas (solo androidTest) |

## Capa multimedia

| Dependencia | Versión | Licencia | Uso y justificación |
|---|---|---|---|
| `io.coil-kt:coil-compose` | 2.7.0 | Apache-2.0 | Miniaturas asíncronas con caché en memoria y disco para la cuadrícula. Evita escribir un cargador de miniaturas propio y ya respeta `ContentResolver` de MediaStore. |
| `androidx.media3:media3-exoplayer` | 1.4.1 | Apache-2.0 | Reproducción local de video dentro del visor (sin red, sin módulos de streaming). |
| `androidx.media3:media3-ui` | 1.4.1 | Apache-2.0 | `PlayerView` con controles accesibles para el reproductor. |

## Plugins de build

| Plugin | Versión | Licencia |
|---|---|---|
| Android Gradle Plugin | 8.7.3 | Apache-2.0 |
| Kotlin Android / Compose compiler | 2.0.21 | Apache-2.0 |
| KSP | 2.0.21-1.0.28 | Apache-2.0 |
| Gradle wrapper | 8.9 | Apache-2.0 |
