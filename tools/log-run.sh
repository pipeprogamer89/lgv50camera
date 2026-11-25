#!/usr/bin/env bash
# Starts a camera-focused logcat capture in the background.
#   tools/log-run.sh [name]
# Writes port-capture/live-<name>-<HHMMSS>.txt and .tags.txt, and leaves the pid in
# /tmp/opencode/lgcam/logcat.pid so tools/log-stop.sh can stop it.
set -u

ROOT=/home/julian/v50-camera
NAME=${1:-test}
STAMP=$(date +%H%M%S)
OUT=$ROOT/port-capture/live-$NAME-$STAMP.txt
TAGS=$ROOT/port-capture/live-$NAME-$STAMP.tags.txt
mkdir -p /tmp/opencode/lgcam

adb logcat -c

# Full main/system/crash buffer: enough for HAL and app failures without the
# enormous kernel/radio noise.
nohup adb logcat -v threadtime -b main,system,crash > "$OUT" 2>/dev/null &
PID=$!
echo "$PID" > /tmp/opencode/lgcam/logcat.pid
echo "$OUT" > /tmp/opencode/lgcam/logcat.file

# Filtered, low-volume view meant to be read live.
nohup adb logcat -v threadtime \
    CameraApp:V LGCameraPort:V MediaRecorderExShim:V CamcorderProfileExShim:V \
    AndroidRuntime:E ActivityManager:I \
    CamX:V CHIUSECASE:V CHIMULTICAMCONTROLLER:V CHILGCameraSolution:V \
    CHILGCameraSolutionCineEffect:V CHILGCameraSolutionFilmEmulator:V LG-AWB:V \
    *:S > "$TAGS" 2>/dev/null &
TPID=$!
echo "$TPID" >> /tmp/opencode/lgcam/logcat.pid

echo "log:  $OUT"
echo "tags: $TAGS"
