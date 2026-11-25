# LG Camera 9.10.17 (V50) → AOSP: estado del port

Uso personal. El APK contiene código propietario de LG: no lo redistribuyas.

## Qué se hizo

| Problema en AOSP | Solución |
|---|---|
| 32 clases `com.lge.*` del framework LG (MediaRecorderEx, CamcorderProfileEx, LGContext, MDM, permisos…) | Shims Java en `shims/src/com/lge` → `classes3.dex` |
| Recursos de `lge-res.apk` (paquete `0x02 com.lge`: diálogos, dimens, colores, `config_app_tier`) | `lge-res-lite.apk`: 107 recursos recompilados con aapt2 conservando los IDs originales; se carga con `ResourcesLoader` desde `PortShim.init()` |
| `AudioManagerEx` / `ActivityManagerEx` (subclases LG) | El tipo se reescribe a `AudioManager` / `ActivityManager` |
| Métodos que LG añadió a `IWindowManager`, `IActivityManager`, `IActivityTaskManager`, `SurfaceControl` | Se redirigen a `com.lge.camera.port.Compat` con valores neutros |
| `FloatMath.sqrt` (eliminado de Android) | Se redirige a `PortShim.sqrt` |
| Campo LG `LayoutParams.bypassKeyFlags` | `AppControlUtil.setBypassAttribute` anulado |
| `ShareActionProviderEx`, `IDisplayManagerEx`, `ScreenshotGraphicBuffer` | Clases stub |
| Config por modelo | `/system_ext/etc/camera_config.xml` del stock → `/product/etc/camera_config.xml` |
| HAL oculta las cámaras auxiliares (tele y gran angular) a paquetes no listados | `com.lge.camera` añadido a `vendor.camera.aux.packagelist` y `persist.vendor.camera.privapp.list` |
| Permisos privilegiados (`ro.control_privapp_permissions=enforce`) | `privapp-permissions-com.lge.camera.xml` (8 permisos) |

Firma: clave de plataforma de pruebas de AOSP (`c8a2e9bc…`), la misma de tu build `test-keys`.

## Verificación realizada (sin dispositivo)
- `dexdump` del build-tools 36.1: los 3 dex son válidos.
- `apksigner verify`: correcto (v3).
- IDs de `lge-res-lite`: 107/107 idénticos a `lge-res.apk`.
- Los shims definen los 113 miembros LG que referencia la app.
- `dex2oat` de tu árbol de Lineage 22.2 (Android 15, boot classpath real): sin referencias
  rotas en código alcanzable. Quedan 2 avisos en caminos muertos (comprobado en el smali).

## Prueba en dispositivo (24-11-2026, V50 LM-V500N con Evolution X 15 / Android 15, Magisk)
Funciona: arranque, vista previa, interfaz LG completa (modos Studio/Retrato/Foto/Videos/AI CAM,
ajustes LG), foto (JPEG 4160x2340 con EXIF), cambio de lente normal → tele (cámara 3) → gran
angular (cámara 2) vía zoom óptico lógico del HAL, pantalla de reposo y reanudación.
Pendiente de probar: grabación de vídeo, cámara frontal, Retrato, modo manual, cámara lenta.

Problemas encontrados y resueltos en el teléfono:
- `PropertyUtils` de LG traduce IDs numéricos a propiedades → mapeo con nombres reales del stock.
- Los estilos de la app heredan de `Theme.LGE.White/Default` → temas LG mínimos sintetizados en lge-res-lite.
- La app en Android 11+ solo lee `camera_config.xml` de `/system_ext/etc` o `/system/etc` (no `/product`).
- `vendor.camera.aux.packagelist` admite como máximo 91 caracteres → lista acortada.
- La app asigna los IDs de sus cámaras lógicas (5–9) a continuación de las físicas →
  `getCameraIdList()` devuelve una cámara por sensor físico (0–4), como en el stock.
- Propiedades LG inexistentes en AOSP (`product.lge.secpolicy.camera.disabled`, región…) → valores por defecto.
- ROM con claves privadas: `hidden-api-whitelisted-app` en `/product/etc/sysconfig` (el módulo ya no exige test-keys).
- Log de LG: `adb shell setprop debug.lgcamera.log 1`.

## Cambios en tu árbol de Lineage
- Nuevo: `vendor/lgcamera/` (Android.bp, lgcamera.mk, APK, XML de permisos, camera_config.xml)
- `device/lge/flashlmdd/device.mk`: `inherit-product-if-exists vendor/lgcamera/lgcamera.mk`
- `device/lge/sm8150-common/system_ext.prop`: `com.lge.camera` en `vendor.camera.aux.packagelist`
- `device/lge/sm8150-common/system.prop`: `com.lge.camera` en `persist.vendor.camera.privapp.list`
- Los originales quedan como `*.orig-lgcamera`.

## Reconstruir
    tools/build.sh

## Probar
- ROM compilada: `m bacon` (o `brunch flashlmdd`) incluye la cámara en `/product/priv-app`.
- ROM ya instalada (test-keys): flashea `out/LGCamera-V50-magisk.zip` en Magisk y reinicia.

Logs útiles:

    adb logcat -s LGCameraPort CameraApp MediaRecorderExShim CamcorderProfileExShim AndroidRuntime

## Riesgos conocidos
- Audio Hi-Fi / ASMR / zoom de audio: dependen del stack multimedia de LG; probablemente graben audio normal.
- GIF, LG Dual Screen, QuickClip y auto-actualización LG: desactivados.
- Perfiles de vídeo LG propios: se mapean a los de AOSP (1080p/720p); los 4K/60 dependen de media_profiles del vendor.

## Cine Video (25-11-2026)

Estado: **vista previa con filtros funcionando; el vídeo grabado sale sin el look**.

Causa real: LG aplica los looks de Cine Video en el HAL, pero solo en el usecase de 10 bits
(`UsecaseVideoEIS3PreviewEIS2CineTwoStreams`). En LineageOS ese usecase devuelve verde (preview y
vídeo), incluso con el parche de 8 bits en `com.qti.chi.override.so` y forzando el tipo del nodo
`com.lge.node.camerasolution.so`. Verificado en dispositivo: preview y MP4 verdes.

Solución implementada (preview):
- `CinemaModuleBase.isSupportedFilterMenu()` → true y no se desactiva `key_film_emulator`.
- `CineCompat.onCinemaInit()` arranca el motor GPU de la app (`FilmEmulatorEngine`) con el look que
  muestra la tira de filtros (LUT identidad `10_Cine 01_.dat` si está en "ninguno").
- `CineCompat.onCinemaLut()` traduce el filtro Cine (índice+1) al LUT de film
  `cinefilters/<n+9>_Cine <nn>_.dat` y lo cambia en vivo.
- La grabación se deja al pipeline normal: el motor GPU es solo de preview (el MediaRecorder de
  AOSP no entrega superficie utilizable y el surface persistente da EGL_BAD_ALLOC).
- El look se guarda en `lgcamera_port`/`cine_lut` para restaurarlo al entrar.

Pendiente (vídeo):
- Quemar el LUT en el MP4 (post-proceso MediaCodec+GL) o sustituir MediaRecorder por un grabador
  MediaCodec propio con superficie de entrada del encoder.
- Viñeta: solo existe en el nodo CineEffect del HAL; no aplicable en 8 bits.

### Vídeo: post-proceso del LUT (25-11-2026)
- `CinemaModuleBase.stopRecorder()` (hook) toma el MP4 recién grabado y el LUT activo.
- `CineVideoPost`: MediaCodec HEVC decode → GLES3 (LUT 3D 36^3) → MediaCodec HEVC encode
  (~24 Mbps), audio AAC copiado con MediaExtractor/MediaMuxer, rotación conservada.
- Reemplaza el archivo original, reescanea MediaStore y muestra toasts. Si falla, deja el original.
- Tiempo medido: ~1.1 s para 5 s a 1080p.
- Solo se procesa si el look activo no es "ninguno" (LUT 1).

#### Corrección de geometría (25-11-2026)
- El decoder Qualcomm aplicaba `KEY_ROTATION` al buffer; se elimina esa clave antes de configurar
  el decoder (la rotación va solo en `MediaMuxer.setOrientationHint`).
- El quad GL tenía las coordenadas de textura invertidas verticalmente; corregidas
  `(0,0),(1,0),(0,1),(1,1)`. Verificado con correlación 0.983 entre frame original y procesado.

## Manual Video (modo Pro): Hi-Fi y HDR10 (25-11-2026)

- **Crash al grabar con Hi-Fi**: `MediaRecorder.setAudioEncoder(12)` (encoder propietario LG) lanza
  `IllegalArgumentException: Invalid audio encoder` y mata la app. `CineCompat.setAudioEncoder`
  redirige 12 → 3 (AAC). El audio Hi-Fi de 24 bits no existe en AOSP: se graba AAC normal.
- **HDR10**: la app pide el perfil HEVC Main10 y el usecase de 10 bits; en LineageOS no es posible.
  Se mantiene la opción pero se graba **HEVC Main 8 bits** (SDR, primarios BT.2020) mediante el
  parche de `VideoRecorder.createPersistentSurface` (0x1000→1) y los stubs de HDR10. No crashea.
  Verificado en Video Manual: HDR10 on + Hi-Fi on → HEVC Main 4K, yuv420p, audio AAC.
