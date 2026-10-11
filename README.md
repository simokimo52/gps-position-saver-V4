# GPS Position Saver 📍

A high-precision, 100% offline-capable GPS bookmarking and position tracking application.
Fully **native Android** — Kotlin + Jetpack Compose. No WebView.

## Tech Stack

- **Kotlin 2.0** + **Jetpack Compose** (Material 3, Compose BOM 2024.10.01)
- **Room** — places, categories, live-share sessions
- **DataStore** — app settings (coordinate format, units, default category)
- **Google Play Services FusedLocationProvider** — live high-accuracy GPS
- **osmdroid 6.1** — offline-capable OpenStreetMap + Esri satellite layers
- **ZXing** — live-share and app QR codes
- **Coil** — photo attachments
- **Nominatim** — background reverse geocoding (graceful offline fallback)

## Features

- **One-tap position save** with live GPS lock, accuracy, altitude, and reverse geocoded address
- **Manual coordinate entry** for offline/test use
- **Rich place details**: name, address (re-geocode), phone (dialer), notes, photos, favorite
- **Categories**: color-coded CRUD with instant chips and a default category
- **Places list**: instant search, category filter, sort (distance, date, name)
- **Interactive map**: osmdroid with standard/satellite layers, category-colored markers, fit-bounds, center-on-GPS
- **Track Recorder**: record your route as you move — live polyline, elapsed time, distance, and max speed; pause/resume; save, rename, and export each track to GPX
- **Navigation hand-off** via geo URI (Google/OSM)
- **Sharing**: formatted text share sheet, copy coordinates/link
- **Backup & Restore**: GPX 1.1, KML 2.2, CSV, JSON via Android SAF
- **Time-limited live location sharing**: TTL countdown, live QR, copy/send link (token-based, no account needed)
- **Settings**: coordinate format (DD/DMS/DDM/UTM), metric/imperial, default category, app QR
- **Privacy**: all data stored on-device; backups disabled by default

## Build

```bash
# JDK 21 required
./gradlew :app:assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk

./gradlew :app:assembleRelease
# APK at app/build/outputs/apk/release/app-release.apk
```

Unit tests for geo math and export/import codecs:

```bash
./gradlew :app:testDebugUnitTest
```

Both build types are signed with `debug.keystore` at the repo root (git-ignored,
credentials `android` / `androiddebugkey`). The debug key is public and intended
for sideloading only — **do not upload these APKs to Google Play**.

## Continuous integration

`.github/workflows/android.yml` builds and tests on GitHub's runners. It runs on
pushes and pull requests to `main`, on any `v*` tag, and on demand via
*Actions → Android CI → Run workflow*.

Each run:

1. installs JDK 21 and the Android 35 platform + build-tools,
2. **restores or generates `debug.keystore`** (see below),
3. runs `:app:testDebugUnitTest`,
4. builds the debug and release APKs,
5. verifies every APK signature with `apksigner`, and
6. uploads them as the `gps-position-saver-apk` artifact (30-day retention).
   Tag pushes additionally publish a GitHub Release with the APKs attached.

### Signing from CI

`debug.keystore` is deliberately kept out of version control, so the workflow
needs it another way. Set a repository secret to keep APK signatures stable:

```bash
# 1. Encode your existing local keystore (Git Bash / macOS / Linux)
base64 -w0 debug.keystore
```

2. Add it as repository secret `ANDROID_KEYSTORE_BASE64`
   (*Settings → Secrets and variables → Actions → New repository secret*).

With that secret set, CI-built APKs carry the **same signature as your local
builds** and upgrade straight over an existing install. Without it the workflow
still succeeds, but generates a throwaway key each run — those APKs install fine
yet cannot upgrade an APK from a previous run, so you must uninstall first.

## Permissions

- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` — required for GPS
- `ACCESS_BACKGROUND_LOCATION` — declared for future foreground service use
- `INTERNET` — map tiles and reverse geocoding

## Architecture

```
app/src/main/java/com/simodev/gpspositionsaver/
├── MainActivity.kt        # single Compose activity
├── GpsSaverApp.kt         # Application: manual DI container
├── AppContainer.kt
├── data/
│   ├── model/             # Place, Group, ShareSession, Track, TrackPoint, enums
│   ├── db/                # Room database + DAOs (v2: tracks + track_points)
│   ├── PlacesRepository.kt
│   └── SettingsStore.kt   # DataStore
├── location/              # LocationRepository, GeocoderClient
├── geo/                   # GeoUtils, TrackComputer (distance, bearing, stats)
├── io/                    # GPX/KML/CSV/JSON codecs + import parser + GPX track export
├── platform/              # QR, share/copy/dial, SAF transfers, photo store
└── ui/
    ├── theme/
    ├── components/        # cards, chips, dialogs, OsmMapView (markers + track polylines)
    ├── AppViewModel.kt    # single source of truth for UI state + track recorder
    ├── GpsApp.kt          # scaffold + navigation + permissions
    └── screens/           # Home, Places, Map, Live, Tracks, Settings, PlaceDetail
```

## Database versions

| Version | Change |
|---------|--------|
| 1 | places, groups, sessions |
| 2 | + tracks, track_points (route recording) — migrated in place, no data loss |