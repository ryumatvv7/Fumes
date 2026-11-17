#!/usr/bin/env bash
# Starts the dev dedicated server, waits until it finishes loading (or fails), then kills it.
# Usage: tools/test-server.sh   (log: run/server_test.log)
cd "$(dirname "$0")/.." || exit 1
mkdir -p run && echo "eula=true" > run/eula.txt
LOG=run/server_test.log
: > "$LOG"
(./gradlew runServer --console=plain > "$LOG" 2>&1 &)
RESULT="TIMEOUT"
for i in $(seq 1 130); do
  if grep -qE "Done \([0-9.]+s\)" "$LOG"; then RESULT="STARTED"; break; fi
  if grep -qE "BUILD FAILED|Crash report|Failed to start the minecraft server|Exception in thread \"main\"" "$LOG"; then RESULT="FAILED"; break; fi
  sleep 3
done
sleep 5
# kill the dev server JVM (devlaunchinjector) started by this project
powershell.exe -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -like '*devlaunchinjector.Main nogui*' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force }" >/dev/null 2>&1
echo "RESULT: $RESULT"
echo "--- errors/warnings ---"
grep -nE "ERROR|WARN|Exception|Caused by" "$LOG" | grep -vE "moonlight:(world_saved_data_type|custom_map_data_types)|server.properties" | head -${1:-40}
