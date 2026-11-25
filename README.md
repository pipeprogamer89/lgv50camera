# LG V50 Camera → AOSP / LineageOS 22.2

Port personal de la **LG Camera 9.10.17** del LG V50 ThinQ (`flashlmdd`) a LineageOS 22.2 /
Android 15, empaquetado como módulo Magisk.

**Autor:** pipecitooficial

> **Aviso:** proyecto personal asistido por IA (Claude/DeepSeek). Como autor **no estoy de acuerdo
> con el uso de IA para programar**. **No ofrezco soporte, no respondo dudas y no doy garantías.**
> Úsalo bajo tu responsabilidad.

---

## Descarga e instalación

El módulo listo para flashear está en:

- `dist/LGCamera-V50-magisk.zip` (~115 MB)
- Instrucciones: `dist/README.md` y `dist/INSTALL.txt`
- Checksum: `dist/SHA256SUMS`

Para GitHub, sube el ZIP como **Release asset** (supera el límite de 100 MB de un commit normal).
No subas al repositorio los blobs propietarios de LG ni los volcados del teléfono.

### Requisitos

- LG V50 ThinQ (`flashlmdd`)
- LineageOS 22.2 / Android 15, build `userdebug/test-keys` (el APK usa la clave de plataforma de
  test-keys; en `release-keys` no está soportado)
- Magisk y bootloader desbloqueado

### Resumen

1. Quita módulos anteriores (`lgcamera_v50`, `lgcamera_v50_halfix`).
2. Comprueba `adb shell pm path com.lge.camera`; si apunta a `/data/app/...`, desinstala la
   actualización de la app.
3. Flashea el ZIP en Magisk y reinicia.

Detalles y limitaciones en `dist/README.md`.

---

## Qué incluye

- APK de LG Camera 9.10.17 con shims de `com.lge.*` y `lge-res-lite` (recursos LG).
- HAL de cámara parcheado (`camera.msmnile.so`, `com.qti.chi.override.so`): NULL pHalStream,
  8 bits para Cine, fuga CVP de `/dev/video34`.
- `camera_config.xml`, permisos privapp y whitelist de hidden APIs.
- LUTs Cine convertidos (film emulator).
- **EllieVision** para AI CAM (firmada con la clave de test-keys). Studio/Retrato usan el HAL.
- **Cine Video:** preview con LUT en GPU y post-proceso del MP4 al guardar (MediaCodec + GL).
- **Video Manual:** Hi-Fi → AAC (sin crash) y HDR10 → HEVC Main 8 bits SDR (fallback).

Limitaciones: sin HDR10/10 bits real, sin audio Hi-Fi 24 bits, sin viñeta en Cine Video, GIF/Dual
Screen/QuickClip desactivados.

---

## Compilar

Entradas necesarias (no incluidas por ser software propietario):

- `p1/app/LGCameraApp.apk` — APK stock del V50 (extraído de tu firmware)
- `p1/sys/...` — `lge-res.apk` y framework de LG
- `p1/etc/camera_config.xml`, `p1/x/*.dex`
- Árbol de LineageOS en `/mnt/Datos/backup/Archivos/lineage` (para `framework-res.apk` y las claves
  de test-keys)
- Android SDK build-tools y `android.jar`

```sh
tools/build.sh        # reconstruye el APK y out/LGCamera-V50-magisk.zip
tools/make-dist.sh    # genera dist/ (módulo combinado + README + SHA256)
```

Estructura:

- `shims/src/` — clases Java de compatibilidad (`com.lge.*`, `CineCompat`, `CineVideoPost`)
- `tools/patcher/` — parcheador de dex (hooks, stubs, literales)
- `tools/build.sh`, `tools/make-dist.sh`
- `dist-src/` — README/INSTALL públicos
- `cine/` — LUTs originales y convertidos

---

## Aviso legal

La app de LG, el HAL y los blobs son **software propietario de LG Electronics**. Este repositorio
solo contiene parches, shims y herramientas de interoperabilidad para uso personal. No se ofrece
licencia sobre el software de LG. No redistribuyas el APK/blobs con fines comerciales; lo ideal es
que cada usuario extraiga el APK de su propio firmware.

**Proyecto personal. Sin soporte. Sin garantías.**
