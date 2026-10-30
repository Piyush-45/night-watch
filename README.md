# Night Watch

Android app that shows every time the phone was unlocked, and which apps were opened, highlighting unlocks during "quiet hours" (default 11 PM – 7 AM).

## How it works
- Reads Android's own usage-event log (`UsageStatsManager.queryEvents`): `KEYGUARD_HIDDEN` = unlocked, `ACTIVITY_RESUMED/PAUSED` = apps used, `KEYGUARD_SHOWN` / screen off = locked.
- No background service needed: Android records these events even if the app is closed.
- Each time the app opens, it saves finished sessions locally (30 days) because Android only keeps raw events for a few days.
- Needs Android 9+ and the "Usage access" permission.

## Get the APK
Every push to `main` builds an APK with GitHub Actions and publishes it under **Releases** → download `night-watch.apk` on the phone and install it (allow "install unknown apps").

## Limits
- Shows *that* the phone was unlocked, not *who* unlocked it.
- Smart Lock auto-unlocks also count.
- If usage access is turned off, the app shows a warning.
