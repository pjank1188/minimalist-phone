# minimalist-phone

A custom Android launcher for aggressive dopamine detox, built for the Solana Seeker 2 (Android 16 / API 36).

## Philosophy

The phone should do the functional things well and make distracting apps effectively
impossible to reach. Friction is a feature, not a bug.

## Design decisions

- **Stack:** Kotlin + Jetpack Compose. Target Android 16 (API 36).
- **Home screen:** clock + date + weather, a quiet "n pickups" count of today's unlocks,
  then a plain-text list of allow-listed apps. No icons. A dim `torch` row at the bottom
  toggles the flashlight.
- **No app drawer.** Apps not on the allow-list cannot be opened from the launcher at all.
- **Allow-list is hard-coded in source.** Changing which apps appear requires editing the
  code and rebuilding — deliberate friction.
- **Grayscale** was tried twice and retired twice: system-wide (adb color-correction
  monochromacy with an accessibility service auto-disabling it for the camera, retired
  2026-06-12), then revived as "sunburn" — gray only during pickup doom-loops until the
  heat decayed — and retired again 2026-07-26. Same verdict both times: eliminating the
  doomscroll apps did the heavy lifting, so the color penalty wasn't pulling its
  weight. Re-enable manually anytime with:
  `adb shell settings put secure accessibility_display_daltonizer_enabled 1`
  `adb shell settings put secure accessibility_display_daltonizer 0`

### Time rules (`Schedule.kt`)

`WorkHoursService` (an AccessibilityService) watches the foreground app and bounces
time-blocked apps back to the home screen outside their windows, covering paths the
launcher can't filter (recents, notifications, share sheets). It also counts unlocks
for the home screen's pickup counter. Three rules:

- **Work apps** (Teams, Outlook): Mon–Fri 08:00–18:00 Eastern only.
- **Instagram:** 17:00–18:00 Eastern daily only.
- **Bedtime:** 22:00–06:00 every day, the whole allow-list is hidden and bounced
  except phone, Messages, Gemini, Kindle, clock (alarms), and the 2FA apps — codes
  must be reachable at any hour, and reading in bed is fine.

One-time accessibility setup over adb (sideload-friendly — bypasses the "restricted
setting" UI block) is handled by `./setup.sh`, or by hand:

```
adb shell settings put secure enabled_accessibility_services com.pjank.minimalistphone/com.pjank.minimalistphone.WorkHoursService
adb shell settings put secure accessibility_enabled 1
```

(If other accessibility services are already enabled, append rather than overwrite the
`enabled_accessibility_services` value.)

### Escalating unlock friction (`Friction.kt`)

The first 10 unlocks of the day cost nothing. Past that, each unlock onto the home
screen holds it behind a dead black screen — just the pickup number and a countdown,
taps swallowed — for 2 seconds per pickup over the allowance, capped at 20. First
checks are free; compulsive re-checks pay a toll.

Deliberately friction, not prison: tapping a notification or switching via recents
still opens apps directly, unlocking into a foreground app (e.g. navigation) skips
the toll, and the cap keeps the worst case irrelevant in an emergency. The knobs
are hard-coded in `Friction.kt`.

### Web blocklist (`WebBlocklist.kt`)

Chrome is the one door the launcher can't filter: the YouTube app is pm-disabled but
youtube.com works fine, and Private DNS only covers the domains it was pointed at.
`WorkHoursService` closes the gap by reading Chrome's omnibox (the same way a screen
reader would) and bouncing blocklisted domains home with a toast, subdomains included.
Like the allow-list, the blocklist is hard-coded in source.

Caveats: the omnibox lookup keys off Chrome's `url_bar` view ID — if a Chrome update
ever renames it the feature silently degrades to "no web blocking" (fails open, never
breaks the phone). Chrome Custom Tabs use a different toolbar layout and aren't
covered; the link interstitial below catches most of that traffic anyway. Incognito
should be visible to accessibility services but hasn't been verified on-device.

### Link interstitial

Tapping an http/https link anywhere offers the launcher's `LinkActivity` alongside
Chrome: a black screen showing the bare URL with three ways out — `copy`,
`send to desktop` (share sheet, for reading at a real computer later), or a conscious
`open in chrome`. Links stop being a straight shot into the browser.

## Allow-list

Phone · Messages · Maps · Camera · Calendar · Signal · GroupMe · Fastmail · Notion ·
Apple Music · Kindle · The Grint · Instagram (5–6 PM only)

Work (separated by a gap): Teams · Outlook · Microsoft Authenticator

Utilities (smaller, dimmer section at the bottom of the screen):
Chase · Capital One · Fidelity · YNAB · Delta · Uber · Proton Pass · Okta Verify ·
Claude · Clock · Chrome

Chrome is the sole browser — Reddit/social are blocked device-wide via Private DNS.
The list lives in `AllowList.kt` (work apps resolve from the managed Work profile;
DAVx5 stays installed for iCloud calendar sync but is off the menu).

## Hardening (outside the launcher)

The launcher controls what's *visible*. Distracting apps are disabled at the system
level over adb so they're truly unreachable (not just hidden). `./setup.sh` applies
all of it in one shot (disables, lock-screen settings, accessibility service, default
launcher) — see `DEVICE-SETUP.md` for the few steps that stay manual.

### Disabled on the device (via `adb shell pm disable-user --user 0 <pkg>`)

| Package | App |
|---|---|
| `com.google.android.youtube` | YouTube |
| `com.google.android.apps.youtube.music` | YouTube Music |
| `com.google.android.play.games` | Play Games |
| `com.google.android.videos` | Google TV / Videos |
| `ag.jup.jupiter.android` | Jupiter (crypto) |
| `com.solanamobile.wallet` | Solana Wallet (crypto) |
| `com.solanamobile.dappstore` | Solana dApp Store (crypto) |

Formerly on this list: **Gemini** and the **Google app** (re-enabled — Gemini is
allow-listed at all hours and needs the Google app running), and the **Play Store**
(re-enabled 2026-07-26 so app updates flow again; it has no launcher entry, so it
stays out of sight — see `DEVICE-SETUP.md` for the install deep-link and the caveat
this reopens).

### To re-enable something later

```
adb shell pm enable <package>
```

(adb lives at `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe` on the dev machine.)

## Building

`./gradlew installDebug` locally, or grab the `app-debug` artifact from the GitHub
Actions `build` workflow (runs on every push) and `adb install -r app-debug.apk`.
