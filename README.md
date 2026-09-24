# Takeout Index — Android app

A hardened WebView wrapper around the Takeout Index web UI. It does not
reimplement search, labels, the reader, or thumbnails — it loads the server's own
mobile UI and adds what a browser tab cannot: server management, a persistent
login session, caching, and native navigation.

## What it does

- **Server address entered once.** First launch shows a setup screen. The
  address is saved; later launches go straight to the UI.
- **Multiple servers.** Add several, switch between them, long-press to remove.
  Reachable any time from the toolbar overflow → *Servers*.
- **Stays logged in until logout.** The session cookie is persisted across app
  restarts. Logging out is done with the web UI's own Logout control.
- **Thumbnail caching.** Uses the WebView HTTP cache with the server's cache
  headers, so thumbnails load from disk on repeat views. *Clear cache* is in the
  overflow menu.
- **HTTP on LAN and HTTPS remotely.** Plain HTTP works for a local server; an
  `https://` address is certificate-validated (a self-signed cert requires
  installing it as a user certificate on the device).
- **Pull to refresh**, native back navigation, external links open in the system
  browser.

## Build (Android Studio — recommended)

1. **Android Studio** → *Open* → select this folder.
2. Studio downloads the Gradle wrapper, the Android Gradle Plugin, and SDK
   components on first sync. Accept any SDK/license prompts.
3. Connect a device (USB debugging on) or start an emulator.
4. *Run* ▶, or *Build → Build APK(s)*.

The signed-debug release APK is written to:

```
app/build/outputs/apk/release/app-release.apk
```

(`assembleRelease` is configured with the debug signing key so it installs
without a keystore. Add a real signing config before wider distribution.)

## Build (command line)

Requires the Android SDK and `ANDROID_HOME` set, plus a JDK 17.

```bash
# One-time, if gradle-wrapper.jar is not present:
gradle wrapper

./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

## First run

1. Enter the server address, e.g. `192.168.50.143:5150` (scheme optional;
   defaults to `http://`). Optionally name it.
2. *Connect*. The server's login page loads; enter username and password there.
3. The session persists — subsequent launches open directly to the app.

## Requirements

- Minimum Android 8.0 (API 26).
- The Takeout Index server reachable from the phone's network.

## Notes

- Credentials are never stored by the app; only the address list and the session
  cookie are persisted (the cookie by the system WebView, in app-private
  storage).
- For an HTTPS server with a self-signed certificate: install the certificate on
  the device (Settings → Security → Install a certificate → CA certificate). The
  app trusts user-installed CAs.
