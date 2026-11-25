#!/usr/bin/env bash
# Rebuilds the LG V50 camera port from the stock APK.
# Inputs : p1/app/LGCameraApp.apk (stock), p1/sys/system/lge-res/lge-res.apk, p1/etc/camera_config.xml
# Outputs: build/LGCameraApp.apk, vendor/lgcamera in the Lineage tree, out/LGCamera-V50-magisk.zip
set -euo pipefail

ROOT=/home/julian/v50-camera
LINEAGE=/mnt/Datos/backup/Archivos/lineage
SDK=/mnt/Datos/backup/Archivos/Android/Sdk
BT=$SDK/build-tools/36.1.0
AJ=$SDK/platforms/android-36/android.jar
FWRES=$LINEAGE/out/target/product/flashlmdd/system/framework/framework-res.apk
SEC=$LINEAGE/build/make/target/product/security
T=$ROOT/tools
B=$ROOT/build
DEXLIB_CP="$T/build/classes:$T/build/deps/guava-jre.jar:$T/build/deps/jsr305.jar"

mkdir -p "$B/dex"

echo "== 1/6 lge-res-lite (LG framework resources, package 0x02)"
python3 -I "$T/gen_lgres_lite.py" "$B/lgres-src" $(cat "$ROOT/lgres_seeds.txt")
rm -rf "$B/lgres-flat" && mkdir "$B/lgres-flat"
"$BT/aapt2" compile --dir "$B/lgres-src/res" -o "$B/lgres-flat/"
"$BT/aapt2" link -o "$B/lge-res-lite.apk" -I "$FWRES" --manifest "$B/lgres-src/AndroidManifest.xml" \
    --package-id 0x02 --allow-reserved-package-id --stable-ids "$B/lgres-src/stable-ids.txt" \
    --min-sdk-version 30 --target-sdk-version 30 "$B"/lgres-flat/*.flat

echo "== 2/6 shims (com.lge.* framework + compat)"
rm -rf "$ROOT/shims/classes" "$ROOT/shims/dex" && mkdir -p "$ROOT/shims/classes" "$ROOT/shims/dex"
javac --release 11 -nowarn -cp "$AJ" -d "$ROOT/shims/classes" $(find "$ROOT/shims/src" -name '*.java')
"$BT/d8" --min-api 30 --lib "$AJ" --output "$ROOT/shims/dex" $(find "$ROOT/shims/classes" -name '*.class')
cp "$ROOT/shims/dex/classes.dex" "$B/dex/classes3.dex"

echo "== 3/6 patch app dex"
javac -nowarn -cp "$DEXLIB_CP" -d "$T/patcher" "$T/patcher/DexPatcher.java"
for d in classes classes2; do
    java -Xmx6g -cp "$T/patcher:$DEXLIB_CP" DexPatcher "$ROOT/p1/x/$d.dex" "$B/dex/$d.dex"
done
for d in classes classes2 classes3; do "$BT/dexdump" "$B/dex/$d.dex" > /dev/null; done
echo "   dexdump verification OK"

echo "== 4/6 assemble + align + sign"
python3 -I - "$ROOT" <<'EOF'
import sys, zipfile
root = sys.argv[1]
src = zipfile.ZipFile(root + '/p1/app/LGCameraApp.apk')
out = zipfile.ZipFile(root + '/build/LGCameraApp-unsigned.apk', 'w')
repl = {'classes.dex': root + '/build/dex/classes.dex', 'classes2.dex': root + '/build/dex/classes2.dex'}
for i in src.infolist():
    n = i.filename
    if n.startswith('META-INF/') and (n == 'META-INF/MANIFEST.MF' or n.split('.')[-1] in ('SF', 'RSA', 'DSA', 'EC', 'LG_RSA', 'LG_SF')):
        continue
    data = open(repl[n], 'rb').read() if n in repl else src.read(n)
    zi = zipfile.ZipInfo(n, date_time=i.date_time); zi.compress_type = i.compress_type; zi.external_attr = i.external_attr
    out.writestr(zi, data)
for name, path, ct in (('classes3.dex', root + '/build/dex/classes3.dex', zipfile.ZIP_DEFLATED),
                       ('assets/lge-res-lite.apk', root + '/build/lge-res-lite.apk', zipfile.ZIP_STORED)):
    zi = zipfile.ZipInfo(name, date_time=(2009, 1, 1, 0, 0, 0)); zi.compress_type = ct
    out.writestr(zi, open(path, 'rb').read())
out.close()
EOF
"$BT/zipalign" -P 16 -f 4 "$B/LGCameraApp-unsigned.apk" "$B/LGCameraApp-aligned.apk"
"$BT/apksigner" sign --key "$SEC/platform.pk8" --cert "$SEC/platform.x509.pem" --out "$B/LGCameraApp.apk" "$B/LGCameraApp-aligned.apk"
"$BT/apksigner" verify "$B/LGCameraApp.apk"
rm -f "$B/LGCameraApp-unsigned.apk" "$B/LGCameraApp-aligned.apk" "$B/LGCameraApp.apk.idsig"

echo "== 5/6 Lineage module (vendor/lgcamera)"
cp "$B/LGCameraApp.apk" "$LINEAGE/vendor/lgcamera/LGCameraApp.apk"

echo "== 6/6 Magisk module"
M=$ROOT/out/magisk
cp "$B/LGCameraApp.apk" "$M/system/product/priv-app/LGCameraApp/LGCameraApp.apk"
# Cine Video looks as LG "downloaded" film filters (LUTs converted from the stock vendor cinema/)
if [ -d "$ROOT/cine/cinema" ]; then
    python3 -I "$T/cine2film.py" "$ROOT/cine/cinema" "$B/cine-lut" > /dev/null
    rm -rf "$M/system/product/etc/lgcamera/cinefilters" && mkdir -p "$M/system/product/etc/lgcamera/cinefilters"
    # 10_Cine 01_ is the identity LUT (copy of the stock 0_film.dat). It keeps the app's GPU film
    # engine running in Cine Video with no visible grade, so a Cine look can be switched live.
    cp "$ROOT/cine/film/0_film.dat" "$M/system/product/etc/lgcamera/cinefilters/10_Cine 01_.dat"
    for n in $(seq 2 16); do
        cp "$B/cine-lut/cine_$n.dat" "$M/system/product/etc/lgcamera/cinefilters/$((n + 9))_Cine $(printf %02d $n)_.dat"
    done
fi
(cd "$M" && rm -f ../LGCamera-V50-magisk.zip && zip -qr -0 ../LGCamera-V50-magisk.zip .)
ls -la "$B/LGCameraApp.apk" "$ROOT/out/LGCamera-V50-magisk.zip"
