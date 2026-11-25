#!/usr/bin/env bash
# Full backup of the stock LG V50: every partition of the active slot plus unslotted ones
# (except userdata). Each image is verified by sha256 on phone and PC, then zstd-compressed.
set -u
OUT=/home/julian/v50-camera/stock-backup
SLOT=$(adb shell getprop ro.boot.slot_suffix | tr -d '\r')
OTHER=$([ "$SLOT" = "_a" ] && echo _b || echo _a)
adb shell "su -c 'mkdir -p /sdcard/v50dump'"
LIST=$(adb shell "su -c 'ls /dev/block/by-name'" | tr -d '\r' | grep -vE "^(userdata|sd[a-f])$|${OTHER}\$")
: > "$OUT/SHA256SUMS"
for p in $LIST; do
    [ -f "$OUT/$p.img.zst" ] && { echo "skip $p"; continue; }
    adb shell "su -c 'dd if=/dev/block/by-name/$p of=/sdcard/v50dump/$p.img bs=4M 2>/dev/null'" < /dev/null
    rs=$(adb shell "su -c 'sha256sum /sdcard/v50dump/$p.img'" < /dev/null | awk '{print $1}')
    adb pull "/sdcard/v50dump/$p.img" "$OUT/$p.img" > /dev/null < /dev/null
    ls=$(sha256sum "$OUT/$p.img" | awk '{print $1}')
    if [ "$rs" != "$ls" ]; then echo "MISMATCH $p"; rm -f "$OUT/$p.img"; continue; fi
    echo "$ls  $p.img" >> "$OUT/SHA256SUMS"
    adb shell "su -c 'rm -f /sdcard/v50dump/$p.img'" < /dev/null
    zstd -q -T0 -6 --rm "$OUT/$p.img" && echo "ok $p $(du -h "$OUT/$p.img.zst" | cut -f1)"
done
adb shell "su -c 'rmdir /sdcard/v50dump'" < /dev/null
echo DONE
