# Verificación

## Comandos locales

```bash
./gradlew testDebugUnitTest   # pruebas unitarias JVM
./gradlew lintDebug           # lint Android
./gradlew assembleDebug       # APK de depuración instalable
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk` y se puede instalar con
`adb install -r app/build/outputs/apk/debug/app-debug.apk` (Android 10+).

## Integración continua

`.github/workflows/android.yml` ejecuta, en cada push y pull request:

1. Pruebas unitarias (`testDebugUnitTest`).
2. Lint (`lintDebug`).
3. `assembleDebug`.
4. Comprobación de identidad del APK con `aapt2 dump badging`:
   `applicationId = org.nexlibre.nexaphotos`, `versionName = 0.1.0`.
5. Publicación del APK y de los informes como artefactos.

## Resultados registrados

| Fecha | Commit | Ejecución | Resultado |
|---|---|---|---|
| 2026-10-25 | `a5c9367` (fundación) | [run 40210757079](https://github.com/Spaceapk/Nexa-Photos/actions/runs/40210757079) | ✅ pruebas, lint y APK |
| 2026-10-25 | `4d8b16c` (capa multimedia) | [run 40211647882](https://github.com/Spaceapk/Nexa-Photos/actions/runs/40211647882) | ✅ pruebas, lint, `assembleDebug` y verificación de identidad del APK |
| 2026-10-25 | `dbab2f6` (HEAD del PR) | [run 40212686916](https://github.com/Spaceapk/Nexa-Photos/actions/runs/40212686916) | ✅ |
| 2026-10-25 | `ff88458` (revisión visual) | [run 40237687582](https://github.com/Spaceapk/Nexa-Photos/actions/runs/40237687582) | ✅ incluye `PaletteContrastTest` y `VisualRulesTest` |
| 2026-10-25 | `3adbab3` (HEAD previo del PR) | [run 40238118738](https://github.com/Spaceapk/Nexa-Photos/actions/runs/40238118738) | ✅ |
| 2026-10-25 | `d040636` (permisos por tipo, paginación, álbumes, zoom, Apache-2.0) | [run 40239222323](https://github.com/Spaceapk/Nexa-Photos/actions/runs/40239222323) | ✅ pruebas, lint, `assembleDebug`, identidad del APK |

En esa última ejecución se comprobó con `aapt2 dump badging`: `package: name='org.nexlibre.nexaphotos'`,
`versionName='0.1.0'`, `versionCode='1'`, y el APK quedó publicado como artefacto
`Nexa-Photos-debug-13` (18,7 MB).

## Pruebas unitarias

El workflow resume los resultados en el paso `Summarize unit tests` y en el *summary* de la
ejecución (clases, pruebas por clase y total). Cubre, entre otras: cursor de paginación, consultas
MediaStore, integridad de páginas con fechas nulas/cero/negativas, permisos por tipo (incluido el
caso mixto de Android 14+), repositorio, estado del ViewModel, agrupación por día, conteos de álbum
truncados, compartir, zoom acotado y contraste de las cuatro paletas.

## Comprobación manual del APK

```bash
aapt2 dump badging app/build/outputs/apk/debug/app-debug.apk | head -3
apksigner verify --print-certs app/build/outputs/apk/debug/app-debug.apk
```

Resultado esperado: firma de depuración (`Android Debug`), sin claves del proyecto.

## Qué no se verifica automáticamente

- Comportamiento real de MediaStore y permisos en dispositivo: requiere pruebas manuales en
  Android 10, 13 y 14+ (acceso parcial). Ver `docs/DEVICE-TESTS.md`.
- Reproducción de video con códecs concretos.
