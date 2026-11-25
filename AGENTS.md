# AGENTS.md

Guía para **quien siga trabajando en este port**: agentes de IA, colaboradores humanos o tu yo del
futuro. Léela entera antes de tocar nada. Describe la arquitectura real, el flujo de build, los
puntos de parcheo y las trampas que ya costaron horas.

> Este repositorio es un **proyecto personal de [pipecitooficial](https://github.com/pipeprogamer89)**.
> No hay soporte ni issues. Si lo modificas, es tu problema y tu responsabilidad.
>
> Nota del autor sobre la IA: el proyecto se desarrolló con asistencia de IA, y el autor **no está
> de acuerdo con usar IA para programar**. Se publica como experimento técnico. Si eres un agente,
> trabaja con cuidado, no inventes resultados y no afirmes haber probado algo que no probaste.

---

## 1. Contexto mínimo

| Dato | Valor |
|---|---|
| Dispositivo | LG V50 ThinQ `flashlmdd` (LM-V500N) |
| ROM objetivo | LineageOS 22.2 / Android 15, build `userdebug/test-keys` |
| App portada | `com.lge.camera` 9.10.17 (APK stock del firmware `V500NO40d` / Android 12 blobs) |
| Técnica | Shims Java en dex adicionales + parcheo de los dex de la app + recursos LG recompilados |
| Empaquetado | Módulo Magisk con `priv-app` firmado con la clave de plataforma de test-keys |
| Framework | App firmada como *platform* en build `test-keys` (en `release-keys` no está soportado) |

Documentación de fondo: [`docs/ANALISIS.md`](docs/ANALISIS.md) (viabilidad) y
[`docs/PORT.md`](docs/PORT.md) (bitácora técnica del port).

---

## 2. Reglas duras

1. **Nunca subas binarios propietarios de LG.** Nada de APKs, `.dex` del stock, `lge-res.apk`,
   HALs, `system.img` ni dumps del teléfono. `.gitignore` ya los excluye: no lo debilites.
2. **Nada de archivos > 100 MB en un commit.** GitHub los rechaza. El módulo se distribuye como
   *release asset*, no en el repo.
3. **No commitees artefactos de build**: `build/`, `out/`, `backups/`, `dist/*.zip`,
   `dist/module/`, `shims/classes/`, `shims/dex/`, `tools/patcher/*.class`, `*.log`.
4. **No inventes verificaciones.** Si no lo corriste en el dispositivo, escribe "pendiente de
   probar", no "funciona".
5. **Respeta la postura del autor sobre la IA** (ver arriba) al escribir textos públicos.

---

## 3. Arquitectura

### 3.1 Capas

```
LGCameraApp.apk (stock, propietario, NO en el repo)
├── classes.dex, classes2.dex   ← parcheados por tools/patcher/DexPatcher.java
├── classes3.dex                ← shims compilados desde shims/src/
├── assets/lge-res-lite.apk     ← recursos LG recompilados (gen_lgres_lite.py)
└── firma                        ← clave de plataforma de test-keys
```

El cargador de dex las trata como una sola app: los shims viven en el *class loader* de la app, así
que `com.lge.*` y los stubs de `android.*` resuelven sin tocar el framework del sistema.

### 3.2 Shims (`shims/src/`)

| Archivo | Rol |
|---|---|
| `com/lge/camera/port/PortShim.java` | Punto de entrada. `PortShim.init(Context)` se inyecta al principio de `LGCameraApplication.onCreate()`. Carga `lge-res-lite.apk` con `ResourcesLoader`, utilidades (`sqrt`, etc.) y arranque de subsistemas. |
| `com/lge/camera/port/Compat.java` | Redirecciones de APIs LG/eliminadas: `getProp`, `getCameraIdList`, `createRecordingSession`, `closeSession`, `getAllStackInfos`, `isOneHandUxOn`, `keyStoreApplicationContext`… |
| `com/lge/camera/port/CineCompat.java` | Todo Cine Video: motor GPU de preview, LUTs, grabación, Hi-Fi→AAC, post-proceso. |
| `com/lge/camera/port/CineVideoPost.java` | Post-proceso del MP4: MediaCodec HEVC → GLES3 (LUT 3D 36³) → HEVC, audio AAC copiado, rotación conservada. |
| `com/lge/camera/port/Screenshots.java` | Sustituye los screenshots propietarios de LG por `PixelCopy` (Live Photo, transiciones). |
| `com/lge/camera/port/Props.java` | Traducción de IDs numéricos de propiedad de LG a nombres reales del stock. |
| `com/lge/**` | 32 clases del framework LG reimplementadas como stubs o shims funcionales (`MediaRecorderEx`, `CamcorderProfileEx`, `LGContext`, `PropertyUtils`, MDM, `LEDManager`, `WfdManager`…). |
| `android/**` | Stubs de clases LG que vivían en el framework: `ShareActionProviderEx`, `IDisplayManagerEx`, `SurfaceControl$ScreenshotGraphicBuffer`. |

### 3.3 Parcheador de dex (`tools/patcher/DexPatcher.java`)

Reescribe los dex del stock con **dexlib2**. Toda la política vive en tablas estáticas en el bloque
`static {}`. Para añadir un parche, añade una entrada; no toques el motor sin motivo.

| Tabla | Efecto |
|---|---|
| `TYPES` | Renombra un tipo (`AudioManagerEx` → `AudioManager`). |
| `STATIC` | Sustituye una llamada estática por otra (`FloatMath.sqrt` → `PortShim.sqrt`). |
| `VIRTUAL` | Sustituye una llamada virtual por un método del shim (recibe el receptor real). |
| `CALLS` | Redirige **solo los call sites** internos de la app, dejando la definición intacta. |
| `STUBS` | Reemplaza el cuerpo de un método por un valor constante (`true`, `false`, `no-op`). |
| `STRINGS` | Cambia un `const-string` **dentro de un método concreto**. |
| `LITERALS` | Cambia una constante numérica **dentro de un método concreto**. |
| `HOOKS` | Inyecta una llamada al shim al **entrar** en un método (el receptor va como primer `Object`). |

Claves: `Lclase;->metodo(args)ret`. Cada parche aplicado se cuenta en `hits` y se imprime al final:
**si un parche no aparece en el resumen, no se aplicó** (el método cambió de firma o de nombre).

Reglas del motor:
- Se descarta toda la info de debug (bug conocido de dexlib2; los dex se validan con `dexdump`).
- `CALLS` no toca la definición del método: útil cuando la app lo usa internamente y el shim no debe
  sustituirlo a nivel global.
- Añadir un `HOOKS` implica que el shim debe tolerar `null` y excepciones: nunca debe tumbar la app.

### 3.4 Recursos LG (`lge-res-lite.apk`)

La app referencia el paquete `0x02` (`com.lge`) de `lge-res.apk`. No se puede portar entero:

1. `tools/lgres_closure.py` calcula el cierre de recursos necesarios partiendo de
   `lgres_seeds.txt` (IDs semilla).
2. `tools/gen_lgres_lite.py` genera un proyecto aapt2 con esos recursos.
3. `aapt2 link --package-id 0x02 --allow-reserved-package-id --stable-ids` conserva **los IDs
   originales** (crítico: la app los referencia como literales).
4. `tools/patch_arsc.py` re-parenta estilos de la app que heredan de `Theme.LGE.*`.

Si un recurso falta en runtime, el síntoma típico es un `Resources$NotFoundException` con un ID
`0x02......`. Añade el ID a `lgres_seeds.txt`, regenera y reconstruye.

### 3.5 HAL y módulo Magisk

- `camera.msmnile.so` y `com.qti.chi.override.so` parcheados a nivel binario (no en el repo): NULL
  `pHalStream`, 8 bits para Cine, y denegar `/dev/video34` para que las sesiones CVP/ArcSoft que
  fugan el nodo fallen limpiamente.
- `out/magisk-halfix/` contiene ese HAL ya parcheado. `tools/make-dist.sh` lo combina con la app.
- `customize.sh` verifica `flashlmdd` y avisa (sin abortar) si el `sha256` del HAL no coincide.
- `service.sh` copia los LUTs a `/data/vendor/LGCamera` con el contexto SELinux que espera el HAL.

---

## 4. Build

```sh
tools/build.sh        # app parcheada + lge-res-lite + out/LGCamera-V50-magisk.zip
tools/make-dist.sh    # dist/LGCamera-V50-magisk.zip + README/INSTALL/SHA256SUMS
```

`tools/build.sh` **no es portable tal cual**: `ROOT`, `LINEAGE` y `SDK` son rutas absolutas del
autor. Ajústalas a tu máquina.

Dependencias:
- `android.jar` y build-tools **36.1.0** (`aapt2`, `d8`, `dexdump`, `zipalign`, `apksigner`)
- JDK 11+
- Python 3 (scripts `-I`, sin dependencias externas)
- `dexlib2` + `guava` + `jsr305` en `tools/build/classes` y `tools/build/deps` (usados por el
  parcheador)

Entradas **no versionadas** que hay que aportar:
```
p1/app/LGCameraApp.apk            # APK stock del V50
p1/x/classes.dex, classes2.dex    # dex extraídos del APK
p1/sys/system/lge-res/lge-res.apk # recursos LG
p1/etc/camera_config.xml          # config de cámara del stock
```

---

## 5. Verificación (sin dispositivo)

El build ya comprueba parte de esto; si tocas dex o recursos, repite:

```sh
# dex válidos
dexdump build/dex/classes.dex   > /dev/null
dexdump build/dex/classes2.dex  > /dev/null
dexdump build/dex/classes3.dex  > /dev/null

# firma
apksigner verify --print-certs build/LGCameraApp.apk

# referencias externas sin resolver (debe estar vacío para clases alcanzables)
python3 tools/dexrefs.py build/dex/*.dex

# IDs de recursos: 107/107 idénticos al stock
```

Además: `dex2oat` contra el boot classpath real de tu árbol de Lineage detecta referencias rotas en
código alcanzable. Quedan 2 avisos conocidos en caminos muertos; no los "arregles" sin comprobarlo
en smali.

---

## 6. Prueba en dispositivo

```sh
tools/test-run.sh     # instala build/LGCameraApp.apk como update y captura el log
tools/log-run.sh      # logcat filtrado en segundo plano
tools/log-stop.sh     # lo para y resume
```

Logs útiles:

```sh
adb logcat -s LGCameraPort CameraApp MediaRecorderExShim CamcorderProfileExShim \
              CHILGCameraSolution FilmEmulationRenderer AndroidRuntime
adb shell setprop debug.lgcamera.log 1
```

Checklist al validar un cambio: arranque → preview → foto JPEG (EXIF) → cambio de lente
(normal/tele/gran angular) → vídeo → Cine Video (look + MP4 post-procesado) → Video Manual
(Hi-Fi y HDR10) → reposo/reanudación. Si algo crashea, adjunta `logcat` y `/data/tombstones/`.

---

## 7. Trampas conocidas

Estas ya se pagaron una vez. No las repitas.

| Síntoma | Causa / decisión |
|---|---|
| Preview verde o MP4 vacío en Cine | El usecase de 10 bits del HAL no funciona en LineageOS. **No lo intentes reactivar**: Cine va por el pipeline de 8 bits con LUTs. |
| Crater al grabar con Hi-Fi | `MediaRecorder.setAudioEncoder(12)` no existe en AOSP. `CineCompat.setAudioEncoder` mapea 12 → 3 (AAC). |
| Vídeo girado o con geometría espejada | El decoder Qualcomm aplica `KEY_ROTATION`; hay que quitarla y usar `MediaMuxer.setOrientationHint`. Las UV del quad GL no van invertidas. |
| `SurfaceMediaSource could not be initialized` | No entregues la superficie del grabador al motor GPU de preview: en Cine el motor es **solo preview**. |
| Cine graba sin look | El HAL solo aplica el look en 10 bits; por eso se post-procesa el MP4. Si el look activo es "ninguno" (LUT 1) no se procesa. |
| La app no ve la cámara / lista vacía | `getCameraIdList()` debe devolver una cámara por sensor físico. Si añades cámaras lógicas 5–9, rompes el mapeo. |
| Cámaras auxiliares invisibles | `vendor.camera.aux.packagelist` tiene un límite de **91 caracteres**; la lista ya está acortada. |
| `camera_config.xml` ignorado | En Android 11+ la app solo lo lee de `/system_ext/etc` o `/system/etc`, **no** de `/product`. |
| Crashes de sesión al cambiar de modo | La sesión de vídeo se crea **sin salidas JPEG** a propósito (evita el crash de sesión diferida del HAL). No añadas `isLiveSnapshotSupported = true`. |
| Permisos de módulo ilegibles | Magisk deja los archivos como `adb_data_file`. `customize.sh` fija dueño/modo/SELinux; si añades rutas nuevas, añade su `set_perm_recursive`. |

Cosas que **no** merece la pena intentar sin hardware nuevo: HDR10/10 bits real, audio Hi-Fi 24 bits,
viñeta de Cine Video, GIF, Dual Screen, QuickClip.

---

## 8. Convenciones

- **Idioma:** español en docs, comentarios y mensajes de commit; identificadores en inglés.
- **Commits:** una línea de resumen en imperativo + cuerpo con el *por qué*. Si el cambio toca el
  HAL o el parcheador, di qué tabla/entrada cambió.
- **Comentarios en `DexPatcher`:** cada entrada no obvia lleva un comentario que explique el fallo
  original. Mantén ese estilo: el siguiente que lo lea no tiene el teléfono delante.
- **Rutas:** los scripts usan rutas absolutas del autor. Si las haces portables, no rompas el flujo
  del autor; añade variables de entorno con *fallback*.
- **Docs:** `docs/PORT.md` es una bitácora cronológica; añade entradas fechadas al final en vez de
  reescribir el historial.

---

## 9. Mapa rápido de "dónde toco si…"

| Quiero… | Voy a… |
|---|---|
| Añadir/reemplazar una clase del framework LG | `shims/src/com/lge/...` |
| Redirigir una API que ya no existe | tabla `STATIC`/`VIRTUAL` de `DexPatcher` |
| Anular un método de la app | tabla `STUBS` |
| Envolver un método de la app con lógica nueva | tabla `HOOKS` + método `static` en un shim |
| Cambiar un literal o un string concreto | `LITERALS` / `STRINGS` |
| Arreglar un recurso LG que falta | `lgres_seeds.txt` + `gen_lgres_lite.py` |
| Cambiar el empaquetado del módulo | `tools/make-dist.sh` |
| Cambiar textos públicos / instrucciones | `dist-src/` (luego `make-dist.sh` los copia) |
