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
go() { adb shell am start -S -n $PKG/.MainActivity "$@" > /dev/null 2>&1; sleep 9; }
go; shot 01-today-dark
go --es tab games; shot 02-games
go --es tab apps; shot 03-apps
go --es tab search; shot 04-search
go --es open alsalah; shot 05-detail
adb shell input swipe 540 1700 540 700 400; sleep 2; shot 06-detail-scrolled
adb shell cmd uimode night no || true
sleep 3
go; shot 07-today-light
go --es open alsalah; shot 08-detail-light
adb shell cmd uimode night yes || true
adb shell appops set $PKG REQUEST_INSTALL_PACKAGES allow || true
adb shell am start -S -n $PKG/.MainActivity --es open alsalah --es get 1 > /dev/null 2>&1
sleep 4; shot 09-download-progress
for i in $(seq 1 30); do
  sleep 4
  adb shell uiautomator dump /sdcard/u.xml > /dev/null 2>&1
  adb pull /sdcard/u.xml /tmp/u.xml > /dev/null 2>&1
  if adb shell dumpsys window | grep -qi 'packageinstaller'; then echo "installer shown after $i"; break; fi
done
shot 10-installer
adb shell pm list packages | grep -E 'com.mas.alsalah' > shots/installed.txt || true
ls -la shots
adb logcat -d -s AndroidRuntime:E > shots/crash.log 2>&1 || true; [ -s shots/crash.log ] || rm -f shots/crash.log
exit 0
