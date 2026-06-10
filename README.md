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
- **Grayscale** is a system setting (Developer Options → Simulate color space → Monochromacy),
  not handled by the launcher.

## Allow-list (v1)

Phone · Messages · Maps · Camera · Clock · Calendar · Authenticator · Music

No browser.

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
