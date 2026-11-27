#!/usr/bin/env bash
# Emulator screenshots for MAS STORE (CI only).
mkdir -p shots
exec > >(tee shots/log.txt) 2>&1
set -x
PKG=com.mas.store
adb wait-for-device
adb root > /dev/null 2>&1 || true
sleep 3
adb wait-for-device
adb shell "setprop persist.sys.locale ar-EG" || true
adb shell "setprop persist.sys.language ar" || true
adb shell "setprop persist.sys.country EG" || true
adb shell stop || true
sleep 2
adb shell start || true
sleep 45
adb wait-for-device
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard || true
adb install -r apk/*.apk || exit 1
adb shell settings put global hide_error_dialogs 1 || true
adb shell cmd uimode night yes || true
shot() { adb exec-out screencap -p > "shots/$1.png"; }
go() { adb shell am start -S -n $PKG/.MainActivity --es ob skip "$@" > /dev/null 2>&1; sleep 9; }
go; shot 01-today-dark
adb shell dumpsys gfxinfo $PKG reset > /dev/null 2>&1
for k in 1 2 3 4 5 6; do adb shell input swipe 540 1800 540 600 250; sleep 0.4; adb shell input swipe 540 600 540 1800 250; sleep 0.4; done
adb shell dumpsys gfxinfo $PKG | grep -E 'Total frames|Janky frames|50th|90th|95th|99th' > shots/jank.txt 2>&1 || true
adb shell input swipe 540 1800 540 700 500; sleep 2; shot 02-today-scrolled
go --es tab games; shot 03-games
go --es tab apps; shot 04-apps
go --es tab search; shot 05-search
go --es tab settings; shot 05b-settings-dark
go --es demo update; shot 05c-update-dark
go --es demo onboard; shot 05e-onboard-dark
go --es demo force; shot 05d-force-dark
go --es open alsalah; shot 06-detail
go --es open alsalah --es demo viewer; shot 06b-viewer
adb shell input swipe 540 1800 540 600 500; sleep 2; shot 07-detail-scrolled
adb shell input swipe 540 1800 540 300 500; sleep 2; shot 08-detail-bottom
adb shell cmd uimode night no || true
sleep 3
go; shot 09-today-light
go --es tab apps; shot 10-apps-light
go --es tab search; shot 11-search-light
go --es tab settings; shot 11b-settings-light
go --es demo update; shot 11c-update-light
go --es open alsalah; shot 12-detail-light
adb shell input swipe 540 1800 540 600 500; sleep 2; shot 13-detail-scrolled-light
adb shell cmd uimode night yes || true
adb shell appops set $PKG REQUEST_INSTALL_PACKAGES allow || true
adb shell am start -S -n $PKG/.MainActivity --es open alsalah --es get 1 > /dev/null 2>&1
sleep 4; shot 14-download-progress
for i in $(seq 1 30); do
  sleep 4
  adb shell uiautomator dump /sdcard/u.xml > /dev/null 2>&1
  adb pull /sdcard/u.xml /tmp/u.xml > /dev/null 2>&1
  if adb shell dumpsys window | grep -qi 'packageinstaller'; then echo "installer shown after $i"; break; fi
done
shot 15-installer
adb shell pm list packages | grep -E 'com.mas.alsalah' > shots/installed.txt || true
ls -la shots
adb logcat -d -s AndroidRuntime:E > shots/crash.log 2>&1 || true; [ -s shots/crash.log ] || rm -f shots/crash.log
exit 0
