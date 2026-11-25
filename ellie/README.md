# EllieVision y AI CAM

Para empaquetar AI CAM, coloca aquí la copia de EllieVision de tu firmware firmada con la clave de plataforma de tu ROM de prueba, con el nombre `EllieVision-platform.apk`. `tools/make-dist.sh` copia ese APK completo a `system/product/priv-app/EllieVision/EllieVision.apk` dentro del módulo.

Solo esta guía se versiona en el directorio. El APK, su contenido extraído (DEX, modelos, bibliotecas, recursos y firmas) y los informes generados al analizarlo se mantienen en local.
