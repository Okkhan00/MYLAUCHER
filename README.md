# My Launcher

A fast, private Android home launcher built with Kotlin, Jetpack Compose and Material 3.
Package: `com.mylauncher.app` - Min Android 8.0 (API 26) - Java 17.

> **Status: Phase 2.** Core launcher plus folders, drag reorder, gestures, launcher lock, quick actions and launch history.

## Features

- Real HOME launcher (`MAIN` + `HOME` + `DEFAULT` intent filter), selectable in Android Settings
- Launcher Setup screen: detects the current default launcher, requests the HOME role
  (RoleManager on Android 10+, system settings on Android 8-9) and only reports success once Android confirms it
- Home screen: auto-updating clock (3 styles), optional date, search pill, favorite apps grid (4/5/6 columns)
- Favorites: add, remove, reorder (move earlier / later), persisted
- App drawer: all launchable apps from PackageManager, alphabetical, instant search
  (app name and package name, case-insensitive, partial), A-Z fast-scroll strip, grid or compact list
- Live refresh when apps are installed, removed or updated
- Long-press menu: Open, App info, Add/Remove favorite, Hide, Uninstall (official system screens)
- Launcher-level hidden apps: hidden from home, drawer and search; manage under Settings > Hidden apps
- Gestures: swipe up = drawer, swipe down = search, double tap = search, long press = edit menu
- Settings: theme (system/light/dark), icon size, labels, grid columns, clock style, date, search bar,
  animations, drawer layout
- Onboarding (skippable, shown once), empty states, your own wallpaper shows through
- Edit menu: add app, add folder, reorder, quick actions, change wallpaper (official picker), launcher settings

### Added in Phase 2

- **Folders:** create, rename, delete, open, add/remove apps, drag to reorder, 2x2 preview icons, empty-folder state.
  An app lives in only one place (home screen or one folder), so it can never be duplicated. Apps can also be
  added to a folder from the long-press menu.
- **Drag and drop:** long-press empty home space > *Reorder home screen*, then drag apps and folders. Inside an
  open folder use *Reorder*. The new order is saved when you lift your finger.
- **Configurable gestures:** swipe up, swipe down and double tap can open the drawer, search, quick actions,
  settings, or do nothing. Long press always opens the edit menu.
- **Quick actions:** Android settings, Wi-Fi, Bluetooth, Battery, Display, Sound, App settings (official intents).
- **Launcher lock:** PIN (4-8 digits, salted PBKDF2-HMAC-SHA256) with optional biometrics (AndroidX Biometric).
  Protects launcher settings and hidden apps. It does **not** lock your phone. After 5 wrong PINs the lock pauses
  for 30 seconds (the counter lives in memory and resets if Android restarts the app).
- **Launch history:** "Recently used" and "Most used" rows in the drawer, based only on apps opened from this
  launcher. Can be turned off and cleared in Settings. Rows are hidden on short screens.
- **Battery:** optional level and charging state under the clock (read from the system, no permission).

## Requirements

- Building on GitHub Actions needs nothing local.
- Building locally: JDK 17, Android SDK (platform 35), Gradle 8.10+.

## Build with GitHub Actions (no Android Studio needed)

1. Create a GitHub repository and upload this whole folder (keep `.github/workflows/android-build.yml`).
2. Open the **Actions** tab. The workflow runs on every push, or start it with **Run workflow**.
3. When it finishes, open the run and download the artifact **MyLauncher-debug-apk**.

The workflow sets up JDK 17 and the Android SDK, accepts licenses, runs unit tests, builds
`assembleDebug` and uploads `app/build/outputs/apk/debug/app-debug.apk`. Lint runs but does not block.
The Gradle wrapper JAR is a binary file, so the workflow generates the wrapper (`gradle wrapper`)
when it is missing.

## Build locally

```
gradle wrapper --gradle-version 8.10.2   # once, creates gradlew
./gradlew test assembleDebug
```

## Install the APK

Copy `app-debug.apk` to your phone, open it, and allow "Install unknown apps" for your file manager or browser.

## Set as default launcher

Open My Launcher > Settings > Default launcher > **Set My Launcher as default**, or go to
Android Settings > Apps > Default apps > Home app. To go back, pick your previous launcher in the same place.

## Permissions

None. App listing uses a `<queries>` entry instead of `QUERY_ALL_PACKAGES`. No INTERNET permission.

## Architecture

```
app/src/main/java/com/mylauncher/app/
  MainActivity.kt, MyLauncherApplication.kt
  data/            AppRepository (PackageManager + icon cache), AppContainer
  data/model/      AppInfo, LauncherSettings
  data/preferences/ DataStore-backed LauncherPreferences, list codec
  launcher/        LauncherApp (root), LauncherViewModel, OnboardingScreen
  launcher/home/   HomeScreen, ClockBlock, AddAppsDialog
  launcher/appdrawer/ DrawerScreen, AlphabetIndex
  launcher/apps/   AppActions, AppActionsDialog, AppListFilter
  launcher/search/ AppSearch (pure, unit tested)
  launcher/folders/ FolderDialog, FolderDialogs, FolderActions
  launcher/gestures/ SwipeResolver
  launcher/setup/  DefaultLauncher, SetupScreen
  security/        LockScreen
  security/pin/    PinHasher, PinThrottle
  security/biometric/ BiometricHelper
  settings/        SettingsScreen, HiddenAppsScreen, SecurityScreen
  ui/              theme + reusable components
```

State lives in DataStore, so it survives restarts, rotation and process death.
Corrupted preference files are replaced with defaults; invalid values fall back to defaults.

## Privacy

No analytics, no ads, no network access, no tracking SDKs. App lists, searches and layout never leave the device.
Backups are disabled so hidden-app choices are not copied to cloud backups.

## Known Android limitations

- **Hidden apps are launcher-level only.** They are not OS-level app hiding: apps stay installed, remain in
  Android Settings, and can be opened by other launchers.
- Android decides whether a launcher can become the default; the app can only ask.
- Locking the screen is not a gesture option: Android only allows it with an Accessibility Service.
- "Recently used" means apps opened from this launcher; Android does not let ordinary apps read other apps' usage.
- Notification badges, widgets and weather are not implemented. Badges need notification access, widget hosting
  is a large feature of its own, and weather would need the INTERNET permission.
- The launcher lock only guards launcher-controlled screens. Anyone with the unlocked phone can still use Android
  Settings, switch launchers, or open hidden apps from there.

## Release signing (later)

`app/build.gradle.kts` reads `SIGNING_KEYSTORE_PATH`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS` and
`SIGNING_KEY_PASSWORD` from environment variables. Store them as GitHub Secrets and decode a base64 keystore in a
workflow step (outline at the bottom of the workflow file). Never commit keystores or passwords.

## Testing

Unit tests (`./gradlew test`) cover preference storage, favorites, hidden apps, folders and their codec, home
ordering, search and filtering, gesture settings and swipe detection, PIN hashing and throttling, launch history
and battery math. Android UI tests are not included yet.

## Roadmap

Widget hosting, notification badges (opt-in), UI tests, icon packs, backup/restore of layout.
