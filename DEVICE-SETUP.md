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

Enabled, but off the allow-list: apps auto-update in the background and the Store has
no launcher entry, so there's no casual browsing. (It was fully pm-disabled until
2026-07-26; that also froze updates, and a 41-app backlog settled the trade-off.)
Installing a new app means deep-linking straight to its listing:

```
adb shell am start -a android.intent.action.VIEW -d "market://details?id=<package>"
```

Then add the package to AllowList.kt and `gradlew installDebug` so it shows up.

Caveat: with the Store enabled, a pm-disabled app's listing shows an "Enable" button —
the disable wall is now only as strong as the friction of reaching the Store UI.

## Accessibility service config changes

Android re-reads an accessibility service's config (event types, capabilities) only
when the service restarts. Installing an update usually does that, but if a config
change doesn't seem to take (e.g. web blocking not firing), re-toggle the service.

Reinstalling can also DISABLE the service outright (`enabled_accessibility_services`
goes null) — the tell is the pickup counter vanishing from the home screen, and it
means no pickups/heat/toll AND no app bouncing or web blocking. After any
`gradlew installDebug`, verify and re-enable if needed:

```
adb shell settings put secure enabled_accessibility_services ""
./setup.sh
```

## Other device-side state

- **Gemini** (`com.google.android.apps.bard`) requires the Google app
  (`com.google.android.googlequicksearchbox`) to be enabled — both were disabled in
  the original de-Google cleanup and had to be re-enabled
  (`adb shell pm enable --user 0 <package>`). The Google app has no launcher entry,
  so it stays invisible; don't re-disable it or Gemini breaks.
- **Private DNS** blocks Reddit/social device-wide (Chrome is the sole browser).
- **Work profile** (user 10) is managed by Company Portal; work apps install there,
  and adb shell cannot query it — scope package queries with `--user 0`.
- **DAVx5** bridges the shared iCloud calendar into Google Calendar via CalDAV; it
  needs its account re-added after a reset even though it has no launcher entry.
