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

Enabled and on the home screen's utilities section (as of 2026-07-26 — it was fully
pm-disabled before, which also froze all app updates; a 41-app backlog settled the
trade-off). Auto-updates run in the background. Installing a new app is just using
the Store, then adding the package to AllowList.kt and `gradlew installDebug` so it
appears on the home screen.

Caveat: a pm-disabled app's listing shows an "Enable" button, so the disable wall on
YouTube etc. is now one conscious tap tall. If that proves too tempting, the fallback
is bouncing the Store UI via WorkHoursService (background updates would keep working).

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
