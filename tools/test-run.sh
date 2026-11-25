#!/usr/bin/env bash
# Install build/LGCameraApp.apk as an update of the system app and launch it, saving the log.
set -u
LOG=${1:-/home/julian/v50-camera/build/run.log}
adb install -r -d /home/julian/v50-camera/build/LGCameraApp.apk | tail -1
adb shell am force-stop com.lge.camera
adb logcat -c
adb shell am start -n com.lge.camera/.app.CameraActivity >/dev/null
sleep ${WAIT:-10}
echo "pid: $(adb shell pidof com.lge.camera)"
adb logcat -d > "$LOG"
grep -E 'FATAL EXCEPTION' -A40 "$LOG" | grep -E 'FATAL|Exception|Error|Caused by|at com\.lge' | head -25
