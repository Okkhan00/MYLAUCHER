# 🚀 My Launcher

### A Fast, Private & Premium Android Home Launcher

**My Launcher** is a customizable Android home launcher built with **Kotlin, Jetpack Compose and Material 3**. It combines a clean home screen, intelligent app search, categories, folders, premium themes, privacy controls and app usage timers in one lightweight application.

Designed for Android 8.0 and above, My Launcher keeps your personal settings and app activity on your device.


---

## 📱 App Information

| Property | Details |
|---|---|
| App Name | My Launcher |
| Current Documented Version | 4.3.0 |
| Version Code | 43 |
| Package ID | `com.mylauncher.app` |
| Platform | Android |
| Minimum Android | Android 8.0 (API 26) |
| Language | Kotlin |
| UI Framework | Jetpack Compose |
| Design System | Material 3 |
| Java | JDK 17 |
| Build System | Gradle + GitHub Actions |
| Backend | None |
| Internet Permission | Not required by the launcher itself |

## ✨ Features

### 🏠 Smart Home Screen
- Real Android HOME launcher.
- Customizable favorites with 4, 5 or 6 columns.
- Three clock styles with optional date.
- Search bar and configurable gestures.
- Add, remove and reorder favorite apps.
- Custom wallpaper with readability controls.
- Home screen folders with drag-and-drop ordering.
- Optional battery level and charging status.
- Android widgets through the standard widget picker.

### 🔎 Smart Search 2.0
- Ranked app search with exact and partial matching.
- Search by application name or package name.
- Search apps inside folders.
- Android settings shortcuts and launcher actions.
- Optional web search using Google, DuckDuckGo, Bing, Brave, Startpage or a custom HTTPS search template.
- Recently used and most-used app suggestions.
- Time-based smart suggestions using local launch history.

### 🗂️ Intelligent App Drawer
- Categories and All Apps tabs.
- Automatic category suggestions for installed apps.
- Rename, hide, reorder and customize categories.
- Manually assign apps to categories.
- Alphabetical A–Z navigation.
- Grid and compact list layouts.
- Fast app search with a prebuilt search index.
- Live refresh when apps are installed, removed, updated or disabled.

### 🎨 Premium Themes & Appearance
Choose from eight built-in themes:

1. Default
2. Midnight Dark
3. Clean Light
4. Ocean Blue
5. Forest Green
6. Royal Purple
7. Warm Amber
8. Minimal Grey
9. AMOLED Black

Also includes:

- Material You dynamic colors on Android 12+.
- Custom accent colors and a separate My Custom Theme.
- Light, dark and system appearance modes.
- Adjustable text size and corner radius.
- Search bar shape customization.
- Drawer opacity, wallpaper dimming and scrim.
- Experimental wallpaper blur on supported devices.

### 🔐 Privacy & Security
- Private apps hidden from the launcher interface.
- PIN-protected launcher settings and private-app management.
- Optional biometric authentication.
- App Lock using PIN or fingerprint when launching selected apps through My Launcher.
- Failed PIN attempt throttling.
- Local storage of preferences and security settings.
- No accounts, advertising SDKs, analytics or tracking.

**Important:** Private apps and App Lock are launcher-level protections. They do not prevent access through Android Settings, another launcher, notifications or recent apps.

### ⏱️ App Usage Timer
Set daily limits for selected applications.

- Preset limits from 15 to 120 minutes.
- Custom daily limits from 1 to 1,440 minutes.
- Separate timer PIN with throttling.
- Parent verification when the allowance expires.
- Reset the allowance or add 15, 30 or 60 minutes.
- Optional reminder notification when time runs out.
- Usage tracking stored locally.

**Usage Access is required.** Android does not allow an ordinary launcher to forcibly close or block an app already running outside the launcher. Enforcement is partial and is not tamper-proof.

### 📊 Device Dashboard & Usage Insights
- Battery level and charging status.
- Storage and memory information.
- Android version information.
- Today, weekly and monthly app usage summaries.
- Recently opened and most-used apps.
- Usage insights based on the launcher's own activity log.

No Usage Access permission is needed for ordinary launch history. The App Usage Timer requires it separately.

### 💾 Backup & Restore
- Export launcher configuration to a ZIP file.
- Restore validated settings from a backup.
- Preserve favorites, folders, home ordering, themes, categories and gestures.
- Include widget configuration entries.
- Reject invalid backup files without applying changes.

**Security note:** Backup files are not encrypted. PIN hashes, biometric settings, usage logs and app timer data are excluded. Keep backup files somewhere you trust.

### ⚡ Performance & Customization
- Balanced, Battery Saver and Smooth performance profiles.
- Cached application icons.
- Incremental app-list updates.
- Background processing for expensive list operations.
- Persistent preferences using DataStore.
- Configurable drawer layouts and animations.
- Long-press app actions and official Android app-management screens.

---

## 🔒 Privacy First

My Launcher is designed to work locally.

- No user accounts.
- No advertising.
- No analytics or tracking SDKs.
- No launcher backend.
- No automatic upload of app lists or search queries.
- No INTERNET permission required by the launcher itself.
- Web searches open your browser instead of sending queries through a launcher server.

Optional Usage Access and notifications are only used for the App Usage Timer features described above.

---

## 📥 Download & Install

### Download the latest APK

**Recommended:** [Download from GitHub Releases](https://github.com/Okkhan00/MyLaucher/releases/latest)

For the direct APK URL, use:

[MyLauncher.apk](https://github.com/Okkhan00/MyLauncher/releases/download/latest/MyLauncher.apk)

### Installation steps

1. Download the APK to your Android phone.
2. Open the downloaded file.
3. If prompted, allow your browser or file manager to install unknown apps.
4. Tap **Install**.
5. Open My Launcher.
6. Follow the setup guide to make it your default home launcher.

### Set My Launcher as the default

Open **My Launcher → Settings → Default launcher → Set My Launcher as default**.

Alternatively, open Android Settings and navigate to **Apps → Default apps → Home app**, then select My Launcher. Menu names may vary by device.

To return to your previous launcher, select it from the same Android settings screen.

---

## 🛠️ Build with GitHub Actions

You do not need Android Studio or a locally installed Android SDK to use a correctly configured GitHub Actions workflow.

### Build steps

1. Upload the complete project to your GitHub repository.
2. Keep the `.github/workflows/android-build.yml` workflow file.
3. Open the repository's **Actions** tab.
4. Select the Android build workflow.
5. Run the workflow or push a new commit to trigger it.
6. Wait for unit tests and the APK build to finish.
7. Download the `MyLauncher-debug-apk` artifact from the completed workflow run.

The expected APK output is:

`app/build/outputs/apk/debug/app-debug.apk`

A successful workflow should run the unit tests, build the debug APK and upload the artifact. Lint may report warnings without failing the build, depending on the workflow configuration.

### Automatic GitHub Releases

For a permanent direct APK link, configure the workflow to publish `MyLauncher.apk` to a GitHub Release using the tag `latest`.

The workflow needs repository contents write permission:

`permissions: contents: write`

The workflow should publish the APK only after the build succeeds. Keep the artifact upload as a fallback if release publishing fails.

**Note:** The release URL works only when the repository contains a release with the expected tag and APK filename.

---

## 🧰 Technology Stack

- Kotlin
- Jetpack Compose
- Material 3
- AndroidX DataStore
- AndroidX Biometric
- Android Usage Stats APIs
- Android App Widgets APIs
- Gradle
- JDK 17
- GitHub Actions

---

## 🧪 Testing & Quality

The project includes unit tests for:

- Search ranking and filtering.
- Favorites, folders and home ordering.
- Hidden apps and category assignments.
- Gesture resolution.
- PIN hashing and throttling.
- Launch history and battery calculations.
- Backup encoding, ZIP validation and restore rules.
- Widget configuration.
- Theme colors and contrast.
- App lock policies.
- Usage timer calculations, renewals and PIN throttling.

**Build status:** A successful full Android build and on-device verification must be confirmed through GitHub Actions and real-device testing. The documented features alone do not guarantee that every feature has been tested on every device.

---

## ⚠️ Known Limitations

- Hidden apps remain installed and may be visible in Android Settings or other launchers.
- App Lock protects launches initiated through My Launcher only.
- The launcher cannot force-close apps or enforce tamper-proof parental controls.
- Usage insights cover activity recorded by My Launcher, not all activity across the phone.
- Usage timers require the user to grant Usage Access in Android settings.
- Timer notifications can be delayed by manufacturer battery restrictions.
- Biometric authentication accepts fingerprints enrolled on the device.
- Experimental wallpaper blur depends on Android version and device support.
- Restored widgets may need to be added again manually.
- Weather and notification badges are not currently implemented.
- Some manufacturers customize default-launcher and Usage Access settings.

---

## 🗺️ Roadmap

Potential future improvements:

- Automated UI testing.
- More icon packs and customization options.
- Optional notification badges.
- Improved widget management.
- Further accessibility and performance improvements.

---

## 👨‍💻 Project Details

**Project:** My Launcher  
**Package:** `com.mylauncher.app`  
**Documented release:** V4.3.0  
**Built with:** Kotlin + Jetpack Compose + Material 3

---

## 📄 License

Add the project's actual license here before distributing the source code publicly. Do not claim an open-source license unless a valid `LICENSE` file is included in the repository.

---


**My Launcher**

*Your home screen. Your control. Your privacy.*

**Powered by AZI CREATIONS**
