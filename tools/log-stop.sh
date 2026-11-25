#!/usr/bin/env bash
# Stops the captures started by tools/log-run.sh and prints a short summary.
set -u
f=/tmp/opencode/lgcam/logcat.pid
if [ -f "$f" ]; then
    while read -r pid; do kill "$pid" 2>/dev/null; done < "$f"
    rm -f "$f"
fi
out=$(cat /tmp/opencode/lgcam/logcat.file 2>/dev/null)
echo "stopped ${out:-logcat}"
[ -n "$out" ] && ls -la "$out" "${out%.txt}.tags.txt" 2>/dev/null
