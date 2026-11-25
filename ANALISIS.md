# Port de LG Camera al V50 en ROMs AOSP: análisis de viabilidad

Fecha: 2026-11-24

## Fuentes analizadas
- V50 stock: `LM-V500`, `flashlmdd_lao_com`, Android 11 `RKQ1.210420.001`, app `com.lge.camera` 9.10.17 (`/product/priv-app/LGCameraApp`)
- Port V60: `LGCameraApp-v72.117.apk` (LG Camera 9.90.5, compileSdk 33, firmada con la clave de plataforma de pruebas de AOSP)
- ROM objetivo: LineageOS 22.2 (Android 15) para flashlmdd, build local `userdebug/test-keys`, blobs de cámara de `V500NO40d` (Android 12)

Datos en `p0/` (volcados de texto) y `p1/` (app, framework y permisos). La APK de `p1/app` se verificó por sha256 contra el teléfono.

## Resultados

| Área | Resultado |
|---|---|
| HAL / vendor tags | **Compatible.** Las 55 vendor tags que la app del V50 usa (y presentes en el HAL stock) aparecen en los blobs de Lineage. Las 124 secciones del HAL stock también. |
| Framework LG (Java) | 32 clases `com.lge.*` externas y unas 95 llamadas. 23 clases ya las implementa como shim el port del V60. Faltan: `appbox.updateproxy.*` (auto-actualización, se puede quitar), `PackageManagerEx`, `LEDManager`/`LGLedRecord`, `WfdManager`. Todo es trivial (stubs). |
| Técnica del porter | Añadió `classes4.dex`/`classes5.dex` con los shims. `MediaRecorderEx extends MediaRecorder` reimplementa solo `setParameter`, `setAudioZooming` y `changeMaxFileSize`. `GifEncoder` es un no-op. `CamcorderProfileEx.getManualSupportedList` es un stub. |
| Librerías nativas de la app | Solo dependen de librerías del NDK, así que son portables. `libLGCameraSolution-jni` no existe ni en stock (la app ya tolera que falte). |
| APIs ocultas (Android 15) | 53 llamadas. Las bloqueadas son `ActivityTaskManager.getService`, `HardwareBuffer.createFromGraphicBuffer` y `CaptureResult.SYNC_FRAME_NUMBER`; `FloatMath.sqrt` está eliminada. Una app firmada con la clave de plataforma queda exenta de los bloqueos, pero las APIs eliminadas fallan siempre. |
| Grabación con extensiones LG | `MediaRecorderEx.setParameter` envía claves como `audio-param-bit-per-sample=` (audio de 24 bits) al stack multimedia de LG en `/system`. En AOSP ese stack no existe: el audio Hi-Fi/24 bits y el zoom de audio probablemente se pierden o necesitan trabajo aparte. |

## Plan
1. Base: la app del V50 (9.10.17), no la del V60. Las vendor tags de la app del V60 apuntan a hardware que el V50 no tiene.
2. Añadir un dex de shims: reutilizar el diseño del V60 y completar las 9 clases que faltan.
3. Parchear con smali `FloatMath.sqrt` y, para la variante "universal" (ROMs firmadas con release-keys), las 3 APIs bloqueadas.
4. Variante A (builds test-keys como la tuya): priv-app firmada con la clave de plataforma de pruebas.
   Variante B (cualquier ROM): módulo Magisk con priv-app y `privapp-permissions` XML, sin depender de APIs ocultas.
5. Pruebas en el dispositivo función por función, con logcat.

## Herramientas que faltan
`jadx`, `apktool`/`baksmali`/`smali`, `apksigner` (las build-tools del SDK en `/mnt/Datos/backup/Archivos/Android/Sdk` probablemente lo incluyen).
