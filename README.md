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
- **Data safety form:** approximate location (optional, used on the phone only), no data collected or shared for ads or analytics, AI helper questions sent to Google Gemini through your proxy.
