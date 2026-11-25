# LG V50 Camera para AOSP / LineageOS 22.2

**Autor:** pipecitooficial  
**Módulo:** `LGCamera-V50-magisk.zip`  
**Base:** LG Camera 9.10.17 del LG V50 (LM-V500N), portada a LineageOS 22.2 / Android 15.  
**Repositorio:** <https://github.com/pipeprogamer89/lgv50camera>  
**Descarga:** <https://github.com/pipeprogamer89/lgv50camera/releases>

Verifica la descarga antes de flashear:

```sh
sha256sum -c SHA256SUMS
```

---

## Aviso importante: IA y soporte

Este proyecto se desarrolló con **asistencia de IA** (Claude y DeepSeek) para analizar el APK, el
framework de LG, el HAL y para escribir/parchear código.

Como autor, **no estoy de acuerdo con el uso de IA para programar**. Publico esto como un proyecto
personal y un experimento técnico, no como una defensa de esa práctica.

**NO ofrezco soporte, NO respondo dudas, NO doy garantías.** Es un proyecto personal que hago
público por si a alguien le sirve. Úsalo bajo tu propia responsabilidad.

> **English:** This is an AI-assisted personal project (Claude/DeepSeek). As the author, I do not
> agree with using AI for programming. I publish it as a personal experiment. **No support, no
> questions, no warranty.** Use at your own risk.

---

## Requisitos

- **LG V50 ThinQ** (`flashlmdd`). No está pensado para otros modelos.
- **LineageOS 22.2 / Android 15** (probado en una build `userdebug/test-keys`).
- **Magisk** (probado con Magisk 27+).
- Bootloader desbloqueado y root.
- Haber hecho backup. El módulo no formatea nada, pero es un port no oficial.

> **Importante sobre la firma:** el APK está firmado con la clave de plataforma de **test-keys** de
> AOSP. En una ROM oficial `release-keys` el APK no tendrá la firma de plataforma y es probable que
> falle. Este módulo está pensado para builds propias/test-keys. En release-keys no está soportado.

---

## Instalación

1. **Quita módulos anteriores** de la cámara si los tienes:
   - `lgcamera_v50`
   - `lgcamera_v50_halfix`
   Reinicia después de quitarlos.
2. Comprueba si la app está instalada como actualización de usuario:
   ```sh
   adb shell pm path com.lge.camera
   ```
   Si devuelve una ruta `/data/app/...`, esa copia tapa el APK del módulo. Opciones:
   - Ajustes → Apps → Cámara → **Desinstalar actualizaciones**, o
   - `adb install -r LGCameraApp.apk` con el APK del módulo.
3. Flashea `LGCamera-V50-magisk.zip` en Magisk.
4. Reinicia.
5. Abre la cámara y concede permisos.

El módulo monta:
- `/product/priv-app/LGCameraApp` (app + permisos + `camera_config.xml`)
- `/vendor/lib64/hw/camera.msmnile.so` y `com.qti.chi.override.so` (HAL parcheado)
- `/vendor/etc/camera/film/` (LUTs Cine para el film emulator del HAL)
- `/data/vendor/LGCamera` en cada arranque (filtros descargados para el HAL)

---

## Qué incluye

- **LG Camera 9.10.17** + shims de `com.lge.*` + `lge-res-lite`.
- **EllieVision** (firmada con la clave de plataforma de test-keys) para el reconocimiento de
  escenas de **AI CAM**. Studio y Retrato no necesitan apps extra: usan el HAL/ArcSoft.
- HAL parcheado, `camera_config.xml`, permisos privapp, hidden-api whitelist y LUTs Cine.

## Qué funciona (probado)

- Arranque de la app, vista previa e interfaz LG completos.
- Foto JPEG (y RAW donde el modo lo permite), cambio de lente normal / tele / gran angular por zoom.
- Cámara frontal, Retrato, Studio, AI CAM, Foto manual.
- Vídeo normal 1080p/4K (según `media_profiles` del vendor), cámara lenta, time-lapse.
- **Cine Video**:
  - La tira de looks (Noir, Clásico, Thriller, etc.) cambia la **vista previa** al instante.
  - Al parar la grabación, el MP4 se **post-procesa** con el LUT (decode HEVC → GL → encode HEVC,
    audio AAC copiado, rotación conservada). Verás `Procesando vídeo Cine…` y luego
    `Vídeo Cine guardado`.
  - Tiempo aproximado: 1–2 s para 5 s de vídeo; ~4 s para 20 s.
  - Si el post-proceso falla, el vídeo original se conserva.
- **Video Manual (Pro)**:
  - **Hi-Fi**: se graba AAC normal (el encoder Hi-Fi de LG no existe en AOSP). Ya no crashea.
  - **HDR10**: se graba **HEVC Main de 8 bits (SDR, BT.2020)**. No es HDR10 real, pero es estable.
- **Víñeta / Fuerza** en Cine Video: Fuerza se aplica al look; **Viñeta no** (solo existía en el
  nodo CineEffect HDR10 del HAL, no aplicable en 8 bits).

---

## Limitaciones conocidas

- **HDR10 real / 10 bits**: no es posible en LineageOS con este HAL/gralloc. La opción graba un
  fallback SDR de 8 bits.
- **Audio Hi-Fi 24 bits, ASMR y zoom de audio**: dependen del stack multimedia de LG; se graba AAC.
- **Viñeta de Cine Video**: no se aplica (ver arriba).
- **GIF, LG Dual Screen, QuickClip, auto-actualización LG, stickers**: desactivados o no probados.
- **Cine Shot** (`com.lge.cinemagraph`), **MyAvatar** y **LGVideoStudio** no están incluidos; son
  apps aparte del firmware stock. AI CAM, Studio y Retrato sí están cubiertos.
- **4K/60**: depende de los perfiles de vídeo del vendor; puede no estar disponible.
- El APK de LG es software propietario. No lo redistribuyas con fines comerciales. Si publicas el
  módulo, hazlo bajo tu responsabilidad; lo ideal es que cada usuario use el APK de su propio
  firmware.

---

## Logs y diagnóstico

```sh
adb logcat -s LGCameraPort CameraApp CHILGCameraSolution FilmEmulationRenderer
```

- Si la app crashea, adjunta `adb logcat` y `/data/tombstones/`.
- El post-proceso de Cine Video deja trazas con la etiqueta `LGCameraPort`.
- No hay soporte. No respondo issues ni mensajes privados.

---

## Desinstalar

1. Desactiva/elimina el módulo en Magisk.
2. Reinicia.
3. Si quieres volver a la cámara stock, reinstala la ROM o el vendor original. El módulo no modifica
   las particiones de forma permanente.

---

## Créditos

- LG por la app y el HAL originales (software propietario).
- LineageOS y la comunidad de flashlmdd.
- Claude y DeepSeek por la asistencia de IA en análisis, parcheo y depuración.
- Probado en un LG V50 ThinQ personal.

**Proyecto personal. Sin soporte. Sin garantías.**
