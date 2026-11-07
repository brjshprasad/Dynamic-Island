# Galaxy Launcher & Dynamic Island PRO

A privacy-focused, zero-telemetry Android customization suite featuring a custom **Multi-Page Galaxy Launcher** with hardware gesture controls, alongside an isolated **Dynamic Island PRO** system overlay designed for native punch-hole camera cutouts.

---

## 🌟 Key Features

### 🏝️ Dynamic Island PRO (`com.custom.dynamicisland`)

- **Native Camera-Level Alignment**: Perfectly positioned at the front punch-hole camera level, matching device hardware contours.
- **100% Offline & Private**: Zero adware, zero spyware, zero analytics, and **0% `android.permission.INTERNET`**.
- **Interactive Island Architecture**:
  - **Expand on Touch**: Tap any compact state to expand into full control cards with fluid spring animations.
  - **Expand-Only Swipe Navigation**: Horizontal swipe to switch between concurrent active apps (Media, Navigation, Calls) is strictly enabled only when expanded, preventing accidental triggers.
  - **Zero Ghost Notifications**: Discards group summaries and background system noise; imprints instantly vanish when notifications are dismissed or apps are closed.
  - **Turn-by-Turn Navigation**: Real-time maneuver warnings for flyovers, bifurcations, forks, roundabouts, turns, and U-turns supporting **Google Maps**, **Mapple (MapmyIndia)**, **Waze**, and any navigation client.
  - **Auto-Shrink**: Automatically shrinks navigation alerts back to compact pill after 4 seconds of inactivity.
  - **Lock Screen Integration**: Renders over the Samsung Keyguard (`setShowWhenLocked`, `setTurnScreenOn`) allowing full interaction and periodic turn updates without unlocking the device.
  - **Media Controls**: Real-time album art, playback controls (previous, play/pause, next), track title, artist, and playback state sync.

### 📱 Galaxy Launcher (`com.custom.galaxylauncher`)

- **Multi-Page Home Screen**: Customizable layout supporting multi-page app arrangements and widgets.
- **Smart App Drawer & Search**:
  - Tapping search box immediately opens the app drawer with keyboard focused.
  - Back gesture minimizes the keyboard while preserving app drawer and frequent app recommendations.
  - Top two rows dynamically highlight frequently used applications; remaining apps are sorted alphabetically.
- **Precision Hardware Gestures**:
  - **Chop-Chop Flashlight**: Dual sharp chops with an arc distance threshold of ≥45° in quick succession toggles the flashlight (pocket jitter immune).
  - **Wrist Twist Camera**: Double wrist twist with an arc rotation threshold of ≥60° instantly wakes and opens the camera.

---

## 📦 Project Structure

```
├── app/                           # Galaxy Launcher application module
│   └── src/main/java/com/custom/galaxylauncher/
│       ├── data/                  # App repository, usage stats, settings
│       ├── gesture/               # Sensor event listeners (Chop-Chop & Wrist Twist)
│       └── ui/                    # Compose UI (HomeScreen, Drawer, Search)
├── dynamicisland/                 # Standalone Dynamic Island PRO module
│   └── src/main/java/com/custom/dynamicisland/
│       ├── model/                 # IslandState, ManeuverType, IslandSettings
│       ├── repository/            # Settings data store
│       ├── service/               # OverlayService, NotificationListener, Controller
│       └── ui/                    # Compose Overlay View, LockScreenActivity, Settings
├── build.gradle.kts               # Root Gradle build configuration
├── settings.gradle.kts            # Project settings
├── GalaxyLauncher-v1.0.0-clean.apk # Pre-built Galaxy Launcher release APK
└── DynamicIsland-v1.0.0-clean.apk  # Pre-built Dynamic Island PRO release APK
```

---

## 🛠️ Building from Source

### Prerequisites
- Android Studio Ladybug / Meerkat or later
- JDK 17+
- Android SDK Platform 34 (Android 14) / 35 (Android 15)

### Command-Line Build

Clone the repository and build release APKs using Gradle:

```bash
# Build Galaxy Launcher APK
./gradlew :app:assembleRelease

# Build Dynamic Island APK
./gradlew :dynamicisland:assembleRelease

# Run all unit tests
./gradlew :dynamicisland:testDebugUnitTest
```

---

## 🚀 Installation & Setup

### 1. Install via ADB
```bash
# Install Galaxy Launcher
adb install -r GalaxyLauncher-v1.0.0-clean.apk

# Install Dynamic Island PRO
adb install -r DynamicIsland-v1.0.0-clean.apk
```

### 2. Grant Permissions for Dynamic Island PRO

Dynamic Island requires Accessibility, Overlay, and Notification Listener access to display system overlays and sync media/navigation:

```bash
# Grant System Alert Window (Display over other apps)
adb shell pm grant com.custom.dynamicisland android.permission.SYSTEM_ALERT_WINDOW

# Enable Notification Listener Service
adb shell cmd notification allow_listener com.custom.dynamicisland/com.custom.dynamicisland.service.IslandNotificationListener

# Enable Accessibility Service
adb shell settings put secure enabled_accessibility_services com.custom.dynamicisland/com.custom.dynamicisland.service.IslandAccessibilityService
adb shell settings put secure accessibility_enabled 1
```

---

## 🔒 Security & Privacy

| Feature | Dynamic Island PRO | Standard Dynamic Island Clones |
| :--- | :---: | :---: |
| **Internet Permission** | ❌ **None (0% Offline)** | ✅ Required (Ad networks, telemetry) |
| **Data Collection** | ❌ **Zero** | ⚠️ Analytics, crash reports, tracking |
| **Background Services** | ⚡ **Event-driven only** | ⚠️ Persistent network polling |
| **Open Source** | ✅ **100% Transparent** | ❌ Proprietary binaries |

---

## 📄 License

Licensed under the [Apache License, Version 2.0](LICENSE).
