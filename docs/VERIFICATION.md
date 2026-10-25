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
