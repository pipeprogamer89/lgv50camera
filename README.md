# LG V50 Camera → AOSP / LineageOS 22.2

[![Android](https://img.shields.io/badge/Android-15-3DDC84?logo=android&logoColor=white)](https://www.android.com)
[![LineageOS](https://img.shields.io/badge/LineageOS-22.2-167C80)](https://lineageos.org)
[![Dispositivo](https://img.shields.io/badge/device-flashlmdd%20(LM--V500N)-informational)]()
[![Release](https://img.shields.io/github/v/release/pipeprogamer89/lgv50camera?label=descarga)](https://github.com/pipeprogamer89/lgv50camera/releases)

Port personal de la **LG Camera 9.10.17** del LG V50 ThinQ (`flashlmdd`) a LineageOS 22.2 /
Android 15, empaquetado como **módulo Magisk**.

**Autor:** [pipecitooficial](https://github.com/pipeprogamer89)

---

## ⚠️ Aviso importante: IA y soporte

Este proyecto se desarrolló con **asistencia de IA** (Claude y DeepSeek) para analizar el APK, el
framework de LG, el HAL, y para escribir y parchear código.

Como autor, **no estoy de acuerdo con el uso de IA para programar**. Publico esto como un proyecto
personal y un experimento técnico, no como una defensa de esa práctica.

> **NO ofrezco soporte, NO respondo dudas, NO doy garantías.** Es un proyecto personal que hago
> público por si a alguien le sirve. Úsalo bajo tu propia responsabilidad.

> **English:** This is an AI-assisted personal project (Claude/DeepSeek). As the author, I do not
> agree with using AI for programming. I publish it as a personal experiment.
> **No support, no questions, no warranty.** Use at your own risk.

---

## Descarga

El módulo listo para flashear está en la sección
**[Releases](https://github.com/pipeprogamer89/lgv50camera/releases)**:

| Archivo | Descripción |
|---|---|
| `LGCamera-V50-magisk.zip` | Módulo Magisk completo (~215 MB) |
| `SHA256SUMS` | Comprobación de integridad |

```sh
sha256sum -c SHA256SUMS
```

Instrucciones completas: **[dist/README.md](dist/README.md)** y **[dist/INSTALL.txt](dist/INSTALL.txt)**.

> El ZIP no se versiona en el repositorio: supera el límite de 100 MB por archivo de GitHub y
> contiene binarios propietarios de LG. Se distribuye únicamente como *release asset*.

### Requisitos

- **LG V50 ThinQ** (`flashlmdd`, LM-V500N)
- **LineageOS 22.2 / Android 15** en build `userdebug/test-keys` (el APK usa la clave de plataforma
  de test-keys; en `release-keys` no está soportado)
- **Magisk** 27+ y bootloader desbloqueado

### Resumen de instalación

1. Quita los módulos anteriores (`lgcamera_v50`, `lgcamera_v50_halfix`) y reinicia.
2. Comprueba `adb shell pm path com.lge.camera`; si apunta a `/data/app/...`, desinstala la
   actualización de la app.
3. Flashea el ZIP en Magisk y reinicia.

---

## Qué incluye

- APK de **LG Camera 9.10.17** con shims de `com.lge.*` y `lge-res-lite` (recursos LG).
- **HAL de cámara parcheado** (`camera.msmnile.so`, `com.qti.chi.override.so`): NULL `pHalStream`,
  8 bits para Cine y fuga CVP de `/dev/video34`.
- `camera_config.xml`, permisos privapp y whitelist de hidden APIs.
- **LUTs Cine** convertidos (film emulator).
- **EllieVision** para AI CAM (firmada con la clave de test-keys). Studio y Retrato usan el HAL.

## Qué funciona

- Arranque, vista previa e interfaz LG completos; foto JPEG/RAW.
- Cambio de lente normal → tele → gran angular por zoom óptico lógico del HAL.
- Cámara frontal, Retrato, Studio, AI CAM, Foto manual.
- Vídeo 1080p/4K según `media_profiles` del vendor; cámara lenta y time-lapse.
- **Cine Video:** la tira de looks cambia la vista previa en vivo y el MP4 se post-procesa con el
  LUT al guardar (MediaCodec + GL), conservando audio y rotación.
- **Video Manual (Pro):** Hi-Fi → AAC (sin crash) y HDR10 → HEVC Main 8 bits SDR (fallback estable).

## Limitaciones conocidas

| Limitación | Motivo |
|---|---|
| Sin HDR10 / 10 bits real | El usecase de 10 bits del HAL devuelve verde en LineageOS |
| Sin audio Hi-Fi 24 bits, ASMR ni zoom de audio | Dependen del stack multimedia propietario de LG |
| Sin viñeta en Cine Video | Solo existía en el nodo `CineEffect` de 10 bits del HAL |
| GIF, Dual Screen, QuickClip y auto-actualización LG | Desactivados |
| Cine Shot, MyAvatar, LGVideoStudio | Apps aparte del firmware stock, no incluidas |

Detalle completo en [dist/README.md](dist/README.md).

---

## Compilar

Entradas necesarias (**no incluidas**, son software propietario):

- `p1/app/LGCameraApp.apk` — APK stock del V50, extraído de tu firmware
- `p1/sys/...` — `lge-res.apk` y framework de LG
- `p1/etc/camera_config.xml`, `p1/x/*.dex`
- Árbol de LineageOS en `/mnt/Datos/backup/Archivos/lineage` (para `framework-res.apk` y las claves
  de test-keys)
- Android SDK build-tools 36.1.0 y `android.jar`

```sh
tools/build.sh        # reconstruye el APK y out/LGCamera-V50-magisk.zip
tools/make-dist.sh    # genera dist/ (módulo combinado + README + SHA256)
```

Las rutas absolutas de `tools/build.sh` (`ROOT`, `LINEAGE`, `SDK`) hay que ajustarlas a tu entorno.
Si vas a modificar el port, lee antes **[AGENTS.md](AGENTS.md)**.

---

## Estructura del repositorio

| Ruta | Contenido |
|---|---|
| `shims/src/` | Clases Java de compatibilidad (`com.lge.*`, `CineCompat`, `CineVideoPost`) |
| `tools/patcher/` | Parcheador de dex (hooks, stubs, literales) |
| `tools/` | Scripts de build, empaquetado, análisis de recursos y captura de logs |
| `cine/` | LUTs originales y convertidos para Cine Video |
| `ellie/` | EllieVision (AI CAM) y sus assets |
| `p0/` | Volcados de texto del stock (props, HAL, vendor tags) |
| `dist-src/` | Fuentes de `README`/`INSTALL` que se publican en `dist/` |
| `docs/` | Documentación técnica del port |

## Documentación

- **[docs/PORT.md](docs/PORT.md)** — estado del port: qué se parcheó, verificación y pruebas en
  dispositivo, cambios necesarios en el árbol de Lineage.
- **[docs/ANALISIS.md](docs/ANALISIS.md)** — análisis de viabilidad: HAL, framework LG, APIs ocultas.
- **[AGENTS.md](AGENTS.md)** — guía para quien (humano o agente) siga trabajando en el port.

---

## Aviso legal

La app de LG, el HAL y los blobs son **software propietario de LG Electronics**. Este repositorio
solo contiene parches, shims y herramientas de interoperabilidad para uso personal. **No se ofrece
ninguna licencia sobre el software de LG.** No redistribuyas el APK ni los blobs con fines
comerciales; lo ideal es que cada usuario extraiga el APK de su propio firmware.

Ver [NOTICE.md](NOTICE.md) para el detalle de componentes de terceros.

## Créditos

- **LG Electronics** por la app y el HAL originales.
- **LineageOS** y la comunidad de `flashlmdd`.
- **Claude** y **DeepSeek** por la asistencia de IA en análisis, parcheo y depuración.
- Probado en un LG V50 ThinQ personal (LM-V500N).

**Proyecto personal. Sin soporte. Sin garantías.**
