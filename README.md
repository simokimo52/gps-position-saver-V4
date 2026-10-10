# GPS Position Saver 📍 (Web Edition)

A high-precision, 100% offline-capable GPS bookmarking and position tracking application rewritten in **React**, **TypeScript**, **Vite**, and **Tailwind CSS**, ported from the original Android application.

## 🌟 Key Features

### 1. One-Tap Offline Position Save
- **Instantaneous Acquisition**: Tap the prominent "Save Position" button to obtain a high-accuracy GPS fix with realistic satellite lock acquisition states.
- **Dual Location Engine**: High-accuracy `navigator.geolocation` with fallback simulation and manual coordinate entry for testing in restricted iframe environments.
- **Accuracy Safeguards**: Accuracy indicator badge (green for $\le 15$m, amber for $> 15$m) with altitude and precision meters/feet readout.
- **Background Reverse Geocoding**: Asynchronously resolves street addresses and landmarks via OpenStreetMap Nominatim with graceful offline fallback.

### 2. Rich Place Details & Media Management
- **Photo Attachments**: Directly capture or upload photos with client-side preview, change, and removal.
- **Comprehensive Fields**: Place title, multiline notes, contact phone number (with direct `tel:` dialer), editable address with one-tap re-geocode, and geographic telemetry readout (DD, DMS, DDM, UTM, altitude, accuracy, and timestamps).
- **Category & Group CRUD**: Organize saved places into customized categories with dedicated color palettes and instant assignment chips.

### 3. Interactive Map & List View
- **Dual Tab Interface**: Seamlessly switch between **List** and **Map** views.
- **OpenStreetMap (Leaflet)**: Free, open-source worldwide map rendering with zero API keys or billing required.
- **Category-Colored Markers & Bounding Fit**: Markers represent saved waypoints with category colors; tapping a marker opens a detail popup. "Fit all markers" and "Center on GPS" buttons for smooth navigation.
- **Instant Reactive Search**: Filter places instantaneously by name, address, notes, or category chips, with flexible sorting (Newest, Oldest, Name A-Z, Distance from current location).

### 4. Navigation & Multi-Format Backup / Restore
- **Navigation Hand-off**: Opens directions in Google Maps, Apple Maps, or OpenStreetMap in a new tab.
- **Single Place Sharing**: Share places as formatted text, copy raw coordinates in one tap, or use the Web Share API.
- **Universal Backup & Restore**:
  - **GPX 1.1**: Standard GPS Exchange XML format for outdoor navigators and handheld GPS units.
  - **KML 2.2**: Keyhole Markup Language for Google Earth and GIS software.
  - **CSV**: Spreadsheet-compatible RFC-4180 format.
  - **JSON Full Backup**: Lossless export and restoration of all places, categories, and metadata.

### 5. Time-Limited Live Location Sharing
- **Self-Terminating TTL**: Select durations of 15 min, 30 min, 1 hour, 4 hours, or 8 hours with countdown timer.
- **Live QR Code**: Generates a shareable QR code for camera scanning.
- **Unguessable Tokens**: Public viewer links utilize unique tokens without requiring user accounts or third-party servers.

### 6. Settings, Customization & Privacy
- **Coordinate Formats**: Decimal Degrees (DD), Degrees, Minutes, Seconds (DMS), Degrees & Decimal Minutes (DDM), or Universal Transverse Mercator (UTM).
- **Unit Systems**: Metric (meters, kilometers) or Imperial (feet, miles).
- **Default Category**: Auto-assigns chosen category to new one-tap saves.
- **100% On-Device Privacy**: All waypoints, notes, coordinates, and images remain strictly on local storage.
- **Data Purge Safeguard**: Full database reset with double-confirmation protection.

## 🏗️ Architecture & Technology Stack

- **UI Framework**: React 18 with TypeScript.
- **Styling**: Tailwind CSS v4.
- **Bundler**: Vite.
- **Mapping Engine**: Leaflet with OpenStreetMap tiles.
- **Icons**: Lucide React.
- **QR Generator**: QRCode.js.
- **Local Persistence**: Browser LocalStorage & IndexedDB with initial sample seeds.

## 🚀 Running the App

```bash
npm install
npm run dev
```

Built for Google AI Studio Web Preview listening on port 3000 (`0.0.0.0:3000`).

