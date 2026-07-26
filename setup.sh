#!/usr/bin/env bash
#
# One-shot device provisioning over adb. Re-applies everything that lives outside the
# repo (see DEVICE-SETUP.md) after a factory reset or on a fresh device. Idempotent —
# safe to re-run anytime.
#
# Prereqs: the launcher APK is already installed (`./gradlew installDebug`), and the
# phone is connected with USB debugging authorized. Runs in Git Bash / WSL on the
# Windows dev machine or any Unix shell.
#
# Usage: ./setup.sh

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

if ! "$ADB" shell pm path "$PKG" >/dev/null 2>&1; then
    echo "error: $PKG is not installed — run ./gradlew installDebug first" >&2
    exit 1
fi

echo "== disabling distracting apps (personal profile)"
# Chrome is deliberately NOT here — it's the sole browser (Private DNS blocks
# Reddit/social device-wide; that's configured by hand in Settings, not over adb).
DISABLE=(
    com.google.android.youtube             # YouTube
    com.google.android.apps.youtube.music  # YouTube Music
    com.google.android.play.games          # Play Games
    com.google.android.videos              # Google TV / Videos
    # The Play Store (com.android.vending) is deliberately NOT here: it stays enabled
    # so apps auto-update, and it's on the home screen's utilities list.
    # Gemini (com.google.android.apps.bard) and the Google app
    # (com.google.android.googlequicksearchbox) are deliberately NOT here: Gemini is
    # allow-listed at all hours and needs the Google app enabled (see DEVICE-SETUP.md).
    ag.jup.jupiter.android                 # Jupiter (crypto)
    com.solanamobile.wallet                # Solana Wallet (crypto)
    com.solanamobile.dappstore             # Solana dApp Store (crypto)
)
for pkg in "${DISABLE[@]}"; do
    if "$ADB" shell pm disable-user --user 0 "$pkg" >/dev/null 2>&1; then
        echo "   disabled  $pkg"
    else
        echo "   skipped   $pkg (not installed or already disabled)"
    fi
done

echo "== play store: enabled (auto-updates; utilities list on the home screen)"
"$ADB" shell pm enable --user 0 com.android.vending >/dev/null 2>&1 || true

echo "== lock screen: hide sensitive notifications and the media player card"
"$ADB" shell settings put secure lock_screen_allow_private_notifications 0
"$ADB" shell settings put secure media_controls_lock_screen 0

echo "== enabling WorkHoursService (accessibility)"
# Append-aware: keeps any accessibility services that are already enabled.
current="$("$ADB" shell settings get secure enabled_accessibility_services | tr -d '\r')"
if [[ "$current" == "null" || -z "$current" ]]; then
    "$ADB" shell settings put secure enabled_accessibility_services "$SERVICE"
elif [[ ":$current:" != *":$SERVICE:"* ]]; then
    "$ADB" shell settings put secure enabled_accessibility_services "$current:$SERVICE"
else
    echo "   already enabled"
fi
"$ADB" shell settings put secure accessibility_enabled 1

echo "== setting the launcher as the default home app"
"$ADB" shell cmd package set-home-activity "$PKG/$PKG.MainActivity"

echo
echo "done. still manual (see DEVICE-SETUP.md):"
echo "  - Private DNS (Settings > Network) to block Reddit/social device-wide"
echo "  - DAVx5 account re-add for the shared iCloud calendar"
echo "  - Work profile enrollment via Company Portal"
