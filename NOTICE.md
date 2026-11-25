# NOTICE

Este repositorio contiene **parches, shims y herramientas de interoperabilidad** para la cámara del LG V50.

## Componentes del firmware

Los siguientes componentes proceden del firmware del dispositivo. **No se versionan en este
repositorio**: para compilar el módulo se aportan localmente, como indica el README.

| Componente | Origen |
|---|---|
| `LGCameraApp.apk` (`com.lge.camera` 9.10.17) | `/product/priv-app/LGCameraApp` del firmware del LG V50 |
| `lge-res.apk` y framework de LG | `/system` del firmware del LG V50 |
| `camera.msmnile.so`, `com.qti.chi.override.so` | Blobs de cámara Qualcomm/LG (`/vendor`) |
| LUTs originales de Cine Video | `/vendor/etc/camera/cinema` del firmware |
| `EllieVision` | App de LG para el reconocimiento de escenas de AI CAM |

**No se concede licencia alguna sobre estos componentes.** Cada usuario debe extraerlos de su propio
firmware y usarlos solo en su dispositivo.

El ZIP publicado en *Releases* contiene el resultado empaquetado del port: la app parcheada, el HAL
modificado y EllieVision firmada con la clave de plataforma. Se publica como **proyecto personal, sin
ánimo comercial y sin garantías**, y no implica licencia alguna sobre el software de LG. No
redistribuyas ese ZIP con fines comerciales.

## Componentes de terceros (incluidos)

| Componente | Licencia | Uso |
|---|---|---|
| [dexlib2 / smali](https://github.com/google/smali) (`tools/build/classes/`) | BSD 3-Clause | Reescritura de los dex de la app |
| [Guava](https://github.com/google/guava) (`tools/build/deps/`) | Apache 2.0 | Dependencia de dexlib2 |
| [JSR-305](https://github.com/google/jsr305) (`tools/build/deps/`) | BSD 3-Clause | Anotaciones de dexlib2 |
| Android SDK build-tools | — (no redistribuido) | `aapt2`, `d8`, `apksigner`, `zipalign` |

Las cabeceras y licencias originales de esos componentes se conservan donde existen.

## Código propio

El código original de este repositorio (shims, parcheador, scripts, documentación) se publica
**sin garantías y sin soporte**, tal como se describe en el [README](README.md). El autor no concede
permiso para presentar este trabajo como propio ni para responsabilizarle de su uso.

## Marcas

LG, LG V50 ThinQ y LG Camera son marcas de LG Electronics. Android y LineageOS pertenecen a sus
respectivos titulares. Este proyecto no está afiliado ni patrocinado por ninguno de ellos.
