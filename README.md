# Night Watch

Android app that shows every time the phone was unlocked, and which apps were opened, highlighting unlocks during "quiet hours" (default 11 PM – 7 AM).

## How it works
- Reads Android's own usage-event log (`UsageStatsManager.queryEvents`): `KEYGUARD_HIDDEN` = unlocked, `ACTIVITY_RESUMED/PAUSED` = apps used, `KEYGUARD_SHOWN` / screen off = locked.
- No background service needed: Android records these events even if the app is closed.
- Each time the app opens, it saves finished sessions locally (30 days) because Android only keeps raw events for a few days.
- Needs Android 9+ and the "Usage access" permission.

## Selfie on unlock (v0.2)
A foreground service waits for unlocks; during quiet hours it takes 2 front-camera photos (~1s and ~4s after unlock), stored privately in the app.

## Sleep guard (v0.3)
If a social/video app (Instagram, YouTube…) stays open for X min with no app switch, a "Still watching?" banner appears. No tap within Y min → the screen is locked via a device-admin `force-lock` policy (the only policy requested). Checks run every 20s only while the screen is on.
To uninstall: Settings → Sleep guard → Auto-lock permission → Remove, then uninstall.

## Get the APK
Every push to `main` builds an APK with GitHub Actions and publishes it under **Releases** → download `night-watch.apk` on the phone and install it (allow "install unknown apps").

## Limits
- Shows *that* the phone was unlocked, not *who* unlocked it.
- Smart Lock auto-unlocks also count.
- If usage access is turned off, the app shows a warning.
