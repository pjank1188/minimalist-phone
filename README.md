# minimalist-phone

A custom Android launcher for aggressive dopamine detox, built for the Solana Seeker 2 (Android 16 / API 36).

## Philosophy

The phone should do the functional things well and make distracting apps effectively
impossible to reach. Friction is a feature, not a bug.

## Design decisions

- **Stack:** Kotlin + Jetpack Compose. Target Android 16 (API 36).
- **Home screen:** clock + date, then a plain-text list of allow-listed apps. No icons.
- **No app drawer.** Apps not on the allow-list cannot be opened from the launcher at all.
- **Allow-list is hard-coded in source.** Changing which apps appear requires editing the
  code and rebuilding — deliberate friction.
- **Grayscale** was tried system-wide (adb color-correction monochromacy with an
  accessibility service auto-disabling it for the camera) and retired 2026-06-12 —
  eliminating the doomscroll apps did the heavy lifting, so the color penalty wasn't
  pulling its weight. Re-enable manually anytime with:
  `adb shell settings put secure accessibility_display_daltonizer_enabled 1`
  `adb shell settings put secure accessibility_display_daltonizer 0`

### Work-hours enforcement

`WorkHoursService` (an AccessibilityService) watches the foreground app and bounces
time-blocked work apps (see `Schedule.kt`) back to the home screen outside work
hours, covering paths the launcher can't filter (recents, notifications). One-time
setup over adb (sideload-friendly — bypasses the "restricted setting" UI block):

```
adb shell settings put secure enabled_accessibility_services com.pjank.minimalistphone/com.pjank.minimalistphone.WorkHoursService
adb shell settings put secure accessibility_enabled 1
```

(If other accessibility services are already enabled, append rather than overwrite the
`enabled_accessibility_services` value.)

## Allow-list

Phone · Messages · Maps · Camera · Calendar · Signal · GroupMe · Gmail ·
Apple Music · Teams · Outlook · Microsoft Authenticator

Utilities (smaller, dimmer section at the bottom of the screen):
Chase · Capital One · Fidelity · Delta · Uber · AWS Events · Proton Pass · Clock

No browser. The list lives in `AllowList.kt` (work apps resolve from the managed
Work profile; DAVx5 stays installed for iCloud calendar sync but is off the menu).

## Hardening (outside the launcher)

The launcher controls what's *visible*. Distracting apps are disabled at the system
level over adb so they're truly unreachable (not just hidden).

### Disabled on the device (via `adb shell pm disable-user --user 0 <pkg>`)

| Package | App |
|---|---|
| `com.android.chrome` | Chrome (no browser) |
| `com.google.android.youtube` | YouTube |
| `com.google.android.apps.youtube.music` | YouTube Music |
| `com.google.android.play.games` | Play Games |
| `com.google.android.videos` | Google TV / Videos |
| `com.android.vending` | Play Store (blocks reinstalls; also pauses auto-updates) |
| `com.google.android.apps.bard` | Gemini |
| `com.google.android.googlequicksearchbox` | Google app / Discover feed (also disables Assistant) |
| `ag.jup.jupiter.android` | Jupiter (crypto) |
| `com.solanamobile.wallet` | Solana Wallet (crypto) |
| `com.solanamobile.dappstore` | Solana dApp Store (crypto) |

### To re-enable something later

```
adb shell pm enable <package>
```

(adb lives at `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe` on the dev machine.)

## Status

Pre-scaffold. Setting up the Android dev environment first.
