# NOTICE

Este repositorio contiene **parches, shims y herramientas de interoperabilidad**. No contiene ni
distribuye el software propietario sobre el que trabaja.

## Componentes propietarios (no incluidos)

Los siguientes componentes son **propiedad de LG Electronics** y se obtienen del firmware del
dispositivo. No se incluyen en este repositorio ni en sus *releases* con fines de redistribución:

| Componente | Origen |
|---|---|
| `LGCameraApp.apk` (`com.lge.camera` 9.10.17) | `/product/priv-app/LGCameraApp` del firmware del LG V50 |
| `lge-res.apk` y framework de LG | `/system` del firmware del LG V50 |
| `camera.msmnile.so`, `com.qti.chi.override.so` | Blobs de cámara Qualcomm/LG (`/vendor`) |
| LUTs originales de Cine Video | `/vendor/etc/camera/cinema` del firmware |
| `EllieVision` | App de LG para el reconocimiento de escenas de AI CAM |

**No se concede licencia alguna sobre estos componentes.** El usuario debe extraerlos de su propio
firmware. Queda prohibida su redistribución comercial.

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
