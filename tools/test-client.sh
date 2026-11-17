#!/usr/bin/env bash
# Starts the dev client, waits until resources finished loading (or it crashes), then kills it.
# Only checks that the game reaches the title screen and collects resource errors. Log: run/client_test.log
cd "$(dirname "$0")/.." || exit 1
LOG=run/client_test.log
mkdir -p run
: > "$LOG"
(./gradlew runClient --console=plain > "$LOG" 2>&1 &)
RESULT="TIMEOUT"
for i in $(seq 1 100); do
  if grep -qE "Created: [0-9]+x[0-9]+x[0-9]+ minecraft:textures/atlas/particles" "$LOG"; then RESULT="LOADED"; break; fi
  if grep -qE "BUILD FAILED|Crash report|---- Minecraft Crash Report|Exception in thread \"main\"" "$LOG"; then RESULT="FAILED"; break; fi
  sleep 3
done
sleep 12
powershell.exe -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -like '*devlaunchinjector.Main*' -and \$_.CommandLine -notlike '*nogui*' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force }" >/dev/null 2>&1
echo "RESULT: $RESULT"
echo "--- errors/warnings mentioning the mod ---"
grep -nE "ERROR|WARN|Exception|Caused by|Missing|Unable to" "$LOG" | grep -iE "fumes|Crash|Exception" | cut -c1-300 | head -${1:-60}
