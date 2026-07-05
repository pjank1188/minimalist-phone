# Device setup notes

Device-side state on the Seeker that lives outside this repo. If the phone is ever
factory-reset (or swapped), re-apply these by hand — installing the launcher alone
won't restore them.

## Lock screen (minimalist pass)

Notifications appear but sensitive content stays hidden until unlock; no media
player card on the lock screen:

```
adb shell settings put secure lock_screen_allow_private_notifications 0
adb shell settings put secure media_controls_lock_screen 0
```

The black wallpaper itself is NOT in this list — the launcher paints it on first
launch (see `ensureBlackWallpaper` in MainActivity.kt).

## Play Store

Disabled in the personal profile so there's no app browsing. Installing a new app
means temporarily enabling it, installing, and disabling again:

```
adb shell pm enable --user 0 com.android.vending
adb shell am start -a android.intent.action.VIEW -d "market://details?id=<package>"
# ...install from the phone...
adb shell pm disable-user --user 0 com.android.vending
```

Then add the package to AllowList.kt and `gradlew installDebug` so it shows up.

## Other device-side state

- **Private DNS** blocks Reddit/social device-wide (Chrome is the sole browser).
- **Work profile** (user 10) is managed by Company Portal; work apps install there,
  and adb shell cannot query it — scope package queries with `--user 0`.
- **DAVx5** bridges the shared iCloud calendar into Google Calendar via CalDAV; it
  needs its account re-added after a reset even though it has no launcher entry.
