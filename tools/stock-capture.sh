#!/usr/bin/env bash
# Records logcat continuously and snapshots camera state every 10 s while the user exercises
# each LG camera feature on stock firmware. Stops after $1 seconds (default 900).
# Note: do NOT call "dumpsys activity top" here: it makes the LG camera pause itself.
C=/home/julian/v50-camera/stock-capture
DUR=${1:-900}
adb logcat -b main,system,events,crash -v threadtime >> "$C/logcat.txt" &
LP=$!
end=$(( $(date +%s) + DUR ))
while [ "$(date +%s)" -lt "$end" ]; do
    t=$(date +%H%M%S)
    adb shell dumpsys media.camera > "$C/dumps/mc_$t.txt" 2>/dev/null < /dev/null
    sleep 10
done
kill $LP
echo DONE
