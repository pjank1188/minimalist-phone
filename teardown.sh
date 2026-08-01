#!/usr/bin/env bash
#
# Reverses setup.sh: uninstalls the launcher and restores the stock Android
# experience. The repo keeps the whole experiment reproducible — coming back is
# `./gradlew installDebug` + `./setup.sh`.
#
# Prereqs: phone connected with USB debugging authorized.
#
# Usage: ./teardown.sh

set -euo pipefail

PKG="com.pjank.minimalistphone"
SERVICE="$PKG/$PKG.WorkHoursService"

# Find adb: $ADB override, PATH, then the default Windows SDK spot (Git Bash).
if [[ -n "${ADB:-}" ]]; then
    :
elif command -v adb >/dev/null 2>&1; then
    ADB=adb
elif [[ -n "${LOCALAPPDATA:-}" && -x "$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" ]]; then
    ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
else
    echo "error: adb not found — put it on PATH or set ADB=/path/to/adb" >&2
    exit 1
fi

echo "== waiting for device"
"$ADB" wait-for-device

echo "== re-enabling the apps setup.sh disabled"
ENABLE=(
    com.google.android.youtube             # YouTube
    com.google.android.apps.youtube.music  # YouTube Music
    com.google.android.play.games          # Play Games
    com.google.android.videos              # Google TV / Videos
    ag.jup.jupiter.android                 # Jupiter (crypto)
    com.solanamobile.wallet                # Solana Wallet (crypto)
    com.solanamobile.dappstore             # Solana dApp Store (crypto)
)
for pkg in "${ENABLE[@]}"; do
    if "$ADB" shell pm enable --user 0 "$pkg" >/dev/null 2>&1; then
        echo "   enabled   $pkg"
    else
        echo "   skipped   $pkg (not installed)"
    fi
done

echo "== lock screen back to Android defaults"
"$ADB" shell settings put secure lock_screen_allow_private_notifications 1
"$ADB" shell settings put secure media_controls_lock_screen 1

echo "== color correction off (in case a grayscale experiment left it on)"
"$ADB" shell settings put secure accessibility_display_daltonizer_enabled 0

echo "== removing WorkHoursService from enabled accessibility services"
# Append-aware, like setup.sh: strips only our entry, keeps any others.
current="$("$ADB" shell settings get secure enabled_accessibility_services | tr -d '\r')"
if [[ "$current" != "null" && -n "$current" ]]; then
    cleaned="$(printf '%s' "$current" | tr ':' '\n' | grep -vFx "$SERVICE" | paste -sd: -)"
    "$ADB" shell settings put secure enabled_accessibility_services "\"$cleaned\""
fi

echo "== uninstalling the launcher (stock launcher takes over on the next Home press)"
"$ADB" uninstall "$PKG" >/dev/null 2>&1 && echo "   uninstalled $PKG" \
    || echo "   skipped (not installed)"

echo
echo "done. still manual (Settings app on the phone):"
echo "  - Private DNS back to Automatic (Network & internet) to unblock Reddit/social"
echo "  - Developer options off (System > Developer options) — also turns off USB"
echo "    debugging; do this LAST, adb stops working after"
echo "  - wallpaper: the launcher painted it black; pick a new one if you want"
echo "untouched on purpose: Play Store (enabled), Gemini + Google app (enabled),"
echo "work profile, DAVx5 calendar sync."
