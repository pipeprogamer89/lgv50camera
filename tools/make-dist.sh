#!/usr/bin/env bash
# Builds the public Magisk distribution from out/magisk (app) + out/magisk-halfix (HAL).
# Output: dist/LGCamera-V50-magisk.zip + README/INSTALL/SHA256SUMS
set -euo pipefail
ROOT=/home/julian/v50-camera
DIST=$ROOT/dist
M=$DIST/module
rm -rf "$M" "$DIST"/*.zip "$DIST/SHA256SUMS" 2>/dev/null || true
mkdir -p "$M"

# --- app, resources, permissions, camera config
cp -a "$ROOT/out/magisk/system" "$M/"
# --- EllieVision (AI CAM scene detection). Platform-signed copy; no privileged permissions.
mkdir -p "$M/system/product/priv-app/EllieVision"
cp "$ROOT/ellie/EllieVision-platform.apk" "$M/system/product/priv-app/EllieVision/EllieVision.apk"
# --- patched HAL + film LUTs
mkdir -p "$M/system/vendor"
cp -a "$ROOT/out/magisk-halfix/system/vendor/." "$M/system/vendor/"
# --- Magisk machinery
cp -a "$ROOT/out/magisk/META-INF" "$M/"

cat > "$M/module.prop" <<'EOF'
id=lgcamera_v50
name=LG V50 Camera (AOSP port)
version=9.10.17-port2
versionCode=2
author=pipecitooficial
description=LG V50 stock camera 9.10.17 for LineageOS 22.2 / Android 15. AI-assisted personal project, no support. See README.
EOF

cat > "$M/customize.sh" <<'EOF'
#!/system/bin/sh
[ "$(getprop ro.product.vendor.device)" = "flashlmdd" ] || [ "$(getprop ro.product.device)" = "flashlmdd" ] || abort "! Only for LG V50 (flashlmdd)"
[ "$(getprop ro.build.version.sdk)" -ge 33 ] || abort "! Needs Android 13 or newer"

ui_print "- LG Camera V50: app + HAL fixes"
ui_print "- Back up your data. This is a personal, AI-assisted project without support."

# The HAL patch is based on the V500NO40d blobs. Other firmwares may differ: warn, do not abort.
C=$(sha256sum /vendor/lib64/hw/camera.msmnile.so 2>/dev/null | cut -d' ' -f1)
H=$(sha256sum /vendor/lib64/hw/com.qti.chi.override.so 2>/dev/null | cut -d' ' -f1)
ui_print "- camera.msmnile.so: $C"
ui_print "- com.qti.chi.override.so: $H"
case "$C" in
  35cc106c8fd5daae529ba9aa97c34433dc1da765ca310798029950fae1372dfa|219d2f02aaa252cebc13615c9c9235dad77c3442de9fb2d8bba8f63fdcaed1ce) ;;
  *) ui_print "! Unknown camera.msmnile.so; the patch may not match your firmware" ;;
esac

# Magisk leaves module files as adb_data_file, which apps cannot read.
set_perm_recursive $MODPATH/system/product 0 0 0755 0644 u:object_r:system_file:s0
set_perm_recursive $MODPATH/system/system_ext 0 0 0755 0644 u:object_r:system_file:s0
set_perm_recursive $MODPATH/system/vendor 0 2000 0755 0644 u:object_r:vendor_file:s0
set_perm_recursive $MODPATH/system/vendor/etc 0 2000 0755 0644 u:object_r:vendor_configs_file:s0
ui_print "- Done. Reboot."
EOF

cat > "$M/post-fs-data.sh" <<'EOF'
#!/system/bin/sh
MODDIR=${0%/*}
resetprop -n --file "$MODDIR/system.prop"
# CVP/ArcSoft sessions leak /dev/video34; deny it so they fail cleanly instead of exhausting V4L2.
chown root:root /dev/video34 2>/dev/null
chmod 0600 /dev/video34 2>/dev/null
EOF

cat > "$M/service.sh" <<'EOF'
#!/system/bin/sh
# The LG HAL film-emulator (used for stills) reads "downloaded" filters from /data/vendor/LGCamera.
# Mirror the Cine LUTs there at boot, with the SELinux label the HAL expects.
SRC=/data/adb/modules/lgcamera_v50/system/product/etc/lgcamera/cinefilters
D=/data/vendor/LGCamera
[ -d "$SRC" ] || exit 0
mkdir -p "$D"
cp -f "$SRC"/*.dat "$D"/ 2>/dev/null
chown -R cameraserver:camera "$D" 2>/dev/null
chmod 0775 "$D" 2>/dev/null
chmod 0664 "$D"/*.dat 2>/dev/null
chcon -R u:object_r:vendor_camera_data_file:s0 "$D" 2>/dev/null
EOF
chmod 0755 "$M/customize.sh" "$M/post-fs-data.sh" "$M/service.sh"
cp "$ROOT/out/magisk/system.prop" "$M/system.prop"

(cd "$M" && zip -qr -0 "$DIST/LGCamera-V50-magisk.zip" .)
cp "$ROOT/dist-src/README.md" "$DIST/README.md"
cp "$ROOT/dist-src/INSTALL.txt" "$DIST/INSTALL.txt"
(cd "$DIST" && sha256sum LGCamera-V50-magisk.zip > SHA256SUMS)
ls -la "$DIST"
