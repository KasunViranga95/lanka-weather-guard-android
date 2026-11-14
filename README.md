# Lanka Weather Guard

An Android weather and flood app for Sri Lanka, in English, Sinhala and Tamil.

- **Weather:** current conditions, a 24-hour and 7-day forecast, wind, humidity, UV index, and sunrise and sunset for any town.
- **Rivers:** live water levels from 60 river gauges (Irrigation Department, plus HMIS and RiverNet.lk gauges through waterlevels.lk), compared against each gauge's alert and flood levels. Levels are shown in metres (Nagalagam Street is published in feet and converted). Gauges with no official levels use levels the app set from their own past readings, and are marked as estimated.
- **Reservoirs:** storage and spill/release for about 140 reservoirs, from the Irrigation Department and Mahaweli Authority daily reports (through waterlevels.lk).
- **Alerts:** worked out from those river levels and the forecast (heavy rain, strong wind, thunderstorms, landslide risk on hill slopes, heat). They are sent as notifications in the background, about every 30 minutes.
- **Saved places:** follow up to six towns; alerts cover all of them.
- **Home-screen widget:** temperature, rain outlook and the nearest river.
- **Works offline:** shows the last forecast and river readings saved on the phone.
- **Map:** Windy.com live map (rain, radar, clouds, wind, temperature).
- **Safety:** emergency numbers, a flood checklist and links to official warnings.

Data comes from Open-Meteo, the Sri Lanka Irrigation Department, Sri Lanka Water Levels (waterlevels.lk, by Shakeeb Sadikeen; data from the Department of Irrigation, HMIS, RiverNet.lk and the Mahaweli Authority) and Windy.com. Alerts are calculated by the app and are not official warnings.

## How it's built

Lanka Weather Guard is a native Android app, written entirely in **Kotlin**. Here's what's under the hood, and why.

### The app

- **Screens: Jetpack Compose with Material 3.** Every screen (the weather page, rivers and reservoirs, the place picker, settings) is built with Compose, Google's modern way to draw Android UI in code. It keeps the look simple and close to the Google Weather style, and works in both light and dark themes.
- **Keeping data and screens in step: ViewModel, coroutines and StateFlow.** One `WeatherViewModel` holds what's on screen. Network calls run in Kotlin coroutines, so the app never freezes while waiting for a slow connection, and the screens update by themselves when new readings arrive.
- **Talking to the internet: Retrofit, OkHttp and Moshi.** These fetch and read the weather forecasts, the river gauges and the reservoir reports, all of which arrive as JSON.
- **Alerts in the background: WorkManager.** About every 30 minutes, even when the app is closed, it checks the rivers and the forecast for your saved places. If something new or worse is happening, you get a notification, sent once rather than over and over.
- **Home-screen widget: Jetpack Glance.** The widget shows the temperature, the rain outlook and your nearest river at a glance.
- **Live map: Android WebView.** The Map tab shows the Windy.com live map inside the app.
- **Your location: Android's own location service.** "Use my location" asks the phone for a fresh position, then picks the nearest town and the nearest river gauge. Your position stays on the phone.
- **Working offline: simple files on the phone.** The last forecast, river readings and reservoir reports are saved as small JSON files, and settings and saved places in Android's SharedPreferences. There's no database, because the app doesn't need one.
- **Three languages: English, Sinhala and Tamil.** All the text lives side by side in one table (`tools/strings_table.py`), and a small Python script turns it into Android's string files. On Android 13 and newer you can pick the app's language separately from your phone's.
- **Keeping keys out of the code: the Secrets Gradle Plugin.** Keys are read from a local `.env` file that isn't committed.

### The server

- **AI helper proxy: a tiny Cloudflare Worker in JavaScript** ([server/ai-proxy](server/ai-proxy)). It sits between the app and Google Gemini so the API key lives on the server, never inside the app. It's optional, and the helper stays hidden until it's set up.

### Where the data comes from

| What | Source |
|---|---|
| Forecasts and current weather | [Open-Meteo](https://open-meteo.com) |
| Most river levels | Sri Lanka Irrigation Department river-gauge service (ArcGIS) |
| More river gauges (HMIS and RiverNet.lk) and reservoir reports | [Sri Lanka Water Levels](https://waterlevels.lk) (waterlevels.lk), by Shakeeb Sadikeen |
| Live map | [Windy.com](https://www.windy.com) |

### Versions at a glance

Kotlin 2.2.10 · Android Gradle Plugin 9.1.1 · Gradle 9.3.1 · Compose BOM 2024.09.00 · Lifecycle 2.8.7 · Coroutines 1.10.2 · Retrofit 2.12 · Moshi 1.15 · WorkManager 2.10 · Glance 1.1.1. The app runs on Android 7.0 (API 24) and up, and targets Android 16 (API 36).

### How it was developed

The app was built and tested on the Android emulator, driving it with `adb` and UI Automator, and then checked on a real phone before each release. Small Python scripts checked the river data and generated the translations, and every change went through its own branch and pull request on GitHub. Development was done with the help of [Claude Code](https://claude.com/claude-code) as an AI pair programmer.

## Build

1. Open the project in Android Studio and run the `app` configuration.
2. The debug build is signed with `debug.keystore` in the project root (not committed). Copy `~/.android/debug.keystore` there if it is missing.

## AI helper (optional)

The ⋮ menu shows **Ask about the weather** once `AI_PROXY_URL` is set in `.env`. See [server/ai-proxy/README.md](server/ai-proxy/README.md). The Gemini key stays on that server and is never put into the app.

## Translations

All text lives in [tools/strings_table.py](tools/strings_table.py), with English, Sinhala and Tamil side by side. After editing it, run:

```bash
python3 tools/strings_table.py
```

This regenerates `values/`, `values-si/` and `values-ta/strings.xml`. Have a native speaker review new Sinhala and Tamil text before release.

## Releasing on Google Play

- **App ID:** `io.github.kasunviranga95.lankaweatherguard`. It cannot change after the first upload.
- **Privacy policy:** [docs/PRIVACY.md](docs/PRIVACY.md). Use its GitHub address in the Play Console.
- **Signing:** create an upload key and set `KEYSTORE_PATH`, `STORE_PASSWORD` and `KEY_PASSWORD` for the `release` signing config.
- **Version:** increase `versionCode` in `app/build.gradle.kts` for every upload.
- **Code shrinking:** release builds are shrunk with R8 (about 2.6 MB instead of 22 MB). Upload `app/build/outputs/mapping/release/mapping.txt` with each release so Play Console crash reports show readable code. If a new library reads classes by reflection, it may need a rule in `app/proguard-rules.pro`; test the release build before uploading.
- **Data safety form:** approximate location (optional, used on the phone only), no data collected or shared for ads or analytics, AI helper questions sent to Google Gemini through your proxy.
