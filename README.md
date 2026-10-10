# My Launcher

A fast, private Android home launcher built with Kotlin, Jetpack Compose and Material 3.
Package: `com.mylauncher.app` - Min Android 8.0 (API 26) - Java 17.

> **Status: Version 4.2 (App drawer redesign, Settings UI, onboarding).** Built on V4.1 performance work and Phase 3 features. The theme engine is not part of this version.

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

### Added in Phase 3 (Smart + Premium)

Everything below is local to the device. No analytics, ads, tracking or cloud, and **no new permissions**.

- **Smart Search 2.0:** ranked results (exact, prefix, word-prefix, contains, package name; ties broken by use count
  then recency), apps inside folders, Android settings shortcuts (public settings intents) and launcher actions.
- **Web search:** an optional "Search the web" result that opens your browser with the query (Google, DuckDuckGo,
  Bing, Brave, Startpage or a custom `https://...{query}` template). The launcher itself never connects to the
  network, so there is no INTERNET permission. Can be turned off.
- **App categories:** 12 categories assigned from package name, label keywords and Android's own app category.
  Rename, hide or reorder categories and move apps between them (Settings > App categories). The drawer has
  Categories / All Apps tabs (see Version 4.2). Hiding a category never hides apps from *All apps*.
- **Smart suggestions:** time-of-day suggestions from your recent launches (14-day decay). Can be turned off.
- **Usage insights:** most used and recently opened apps for today / week / month, from the launcher's own capped
  launch log. No Usage Access permission, so apps opened elsewhere are not counted.
- **Themes:** System / Light / Dark / Material You (Android 12+) / Custom accent, AMOLED and tinted surfaces,
  corner radius, text scale, search bar shape.
- **Wallpaper options:** dim and scrim for readability, drawer opacity, and an experimental blur (Android 12+,
  depends on the device).
- **Private apps:** the former "Hidden apps" screen, now also reachable only through the launcher lock when a PIN is set.
- **App lock:** choose apps that need your PIN or fingerprint when opened *from this launcher*.
- **Backup and restore:** export settings to a single ZIP file you choose (Android file picker) and import it
  again. Import is validated against an allow-list; invalid files are rejected without changing anything.
- **Performance modes:** Balanced, Battery saver (no animations or effects, smaller icon cache, slower search
  debounce) and Smooth. No background services.
- **Advanced app menu:** open, app info, favorites, folders, category, hide/make private, app lock, share link,
  uninstall (always confirmed by Android).
- **Device dashboard:** battery, storage, memory and Android version read from public APIs, no permission.
- **Widgets:** add Android widgets to the home screen through the standard picker, with the usual configure and
  remove flow.

#### Backup contents

Included: launcher settings, theme, wallpaper options, smart settings, category config and overrides, favorites,
folders, home order, hidden/private and locked app lists, gesture settings, widget list.
Never included: the PIN hash and salt, biometric setting, launch history and usage log. After restoring on
another device, set the PIN again. The file is not encrypted, so keep it somewhere you trust: it lists app
package names, including private ones.

## Changelog

### Version 4.3.0 (versionCode 43)

- **Themes:** Settings > Appearance > **Themes** is now a gallery of eight built-in themes (Default, Midnight Dark,
  Clean Light, Ocean Blue, Forest Green, Royal Purple, Warm Amber, Minimal Grey, AMOLED Black) with a small preview,
  a clear "Selected" mark, instant apply and persistence. Themes are code constants built on the existing mode,
  accent and surface settings, so nothing new is stored for them and no setting was reset. **My Custom Theme** has
  its own saved slot (mode, accent, surface) that never overwrites a built-in theme. Text size, shape and drawer
  opacity stay available. Colors reach home, drawer, category cards, search, dialogs and settings through the same
  Material 3 scheme. No new dependencies, no downloads, no theme store, no system-wide theming.
- **App usage timer:** Settings > Privacy and hidden apps > **App usage timer**. Choose installed apps and a daily
  limit (15/30/45/60/90/120 min or custom 1-1440). A separate **timer PIN** (salted PBKDF2 hash, saved attempt
  throttling) is required before timers can be turned on or changed. When time is up, opening the app from My
  Launcher shows "Your allowed usage time for this app has ended. Ask the authorized parent to verify to continue."
  After verification the parent chooses **Reset timer** (full allowance again, default) or **Add 15/30/60 minutes**.
  My Launcher itself can never be limited.

#### Timer feasibility report

| Question | Answer |
| --- | --- |
| What is implemented | Real foreground time per app, read from Android's usage events; a gate that stops a limited app from being opened from My Launcher once its time is used up; a quiet reminder notification when time runs out while the app is open; parent verification, reset and extra time. |
| What Android prevents | A normal launcher cannot close or cover another app. Without an Accessibility Service, Device Admin, overlay, VPN or Device Owner (all deliberately not used) nothing can interrupt an app that is already open, or opened from recent apps, a notification, a shortcut or another launcher. |
| Permissions | `PACKAGE_USAGE_STATS` (special "Usage Access", granted by the user in system settings after an explanation inside the app) and `POST_NOTIFICATIONS` (Android 13+, optional, only for the reminder). No internet, no accessibility, no admin. |
| Versions / devices | Usage events work on all supported versions (API 26+) when Usage Access is on; some manufacturers hide or restrict the Usage Access screen, and aggressive battery savers may delay or drop the reminder alarm (inexact alarm, lost on reboot). The check is repeated at every launch from My Launcher. Android keeps usage events for about 7 days. |
| Fallback | If Usage Access is off, time cannot be measured: nothing is blocked and Settings shows a warning. If the reminder is late or missing, the next launch from My Launcher is still checked. |

**Enforcement level: partial.** It is real only for apps opened from My Launcher; otherwise it is a reminder
notification plus warnings. It is not tamper-proof: force-stopping or uninstalling My Launcher, choosing another
home app, or clearing its data ends the limits. Biometric verification (optional, off by default) accepts any
fingerprint enrolled on the phone, including a child's. The timer PIN hash lives in My Launcher's private storage
(not Keystore-bound) like the launcher PIN, and a forgotten timer PIN cannot be recovered except by clearing app data.

**Timer privacy:** usage is read and stored on the device only. Timer settings, state and PIN are excluded from
backup files. No upload, analytics, ads or accounts. Monitoring starts only after you set a PIN, turn timers on,
allow Usage Access and add an app, and each step can be undone.

### Version 4.2.0 (versionCode 42)

- **App drawer:** two tabs at the top, **Categories** and **All Apps**. Categories mode shows large cards with an
  icon, the category name, the app count and 3-4 preview icons (fewer on a very narrow card). Cards use an adaptive
  grid (two columns on a typical phone, more on wider screens). Tap a card to open that category's apps; the back
  arrow or the Android back gesture returns to the cards. All Apps keeps the A-Z list/grid and alphabet index.
  Search works the same in both modes and replaces the content while you type. The drawer opens in Categories by
  default; if you had already saved a smart setting, your saved choice is kept.
- **Categories:** assigned automatically from package name, app name and the category the app declares to Android
  (heuristic, not an official Android classification). Names now read Entertainment, Productivity and Other Apps;
  saved category ids, custom names, hidden flags and manual assignments are unchanged. You can still move an app
  to another category from its long-press menu; manual choices are saved and never overwritten.
- **Settings:** regrouped into Home screen, App drawer, Search, Gestures, Appearance, Privacy and hidden apps,
  General and About, with large rows, short explanations and chips (up to three options) or a small picker dialog
  (longer lists). Every control uses the same saved values as before; nothing is reset by opening Settings.
  "Show the welcome guide again" replays onboarding.
- **Onboarding:** Welcome, Set as default launcher (success is shown only when Android reports My Launcher as the
  home app; declining is fine), two basic choices (appearance, drawer view), Finish. Completion is saved with the
  existing `onboardingDone` setting, so it is shown once.
- **Unchanged:** V4.1 performance work (icon cache, search index, off-main-thread lists), hidden/private apps,
  folders, favorites, gestures, App Lock, backup format, permissions (none added), GitHub Actions workflow.
- Tests: 8 new unit tests (category icons and names, saved-config compatibility, card preview count, hidden apps,
  manual category persistence, default drawer mode).
- Not verified in this environment: building the APK, lint, DataStore tests, and running the UI on a device.

### Version 4.1.0 (versionCode 41)

Performance and stability only. No new features, screens or permissions; the Version 3 UI is unchanged.

- **Search:** the drawer now uses the prebuilt search index (lower-cased labels and words are computed once per app
  list, not on every keystroke). Results are identical to before; new tests check that.
- **Scrolling / icons:** an icon tile that is already in the icon cache is drawn immediately without starting a
  coroutine. Drawer list and grid items declare a content type so Compose can reuse item layouts.
- **Recomposition:** the long-press menu, folder actions and launch callbacks are remembered, so the home and drawer
  screens are no longer rebuilt every time the root screen recomposes. The Material color scheme is built once per
  theme change instead of on every recomposition.
- **Main thread:** home entries, visible apps, recent / most used apps and suggestions are computed on a background
  dispatcher. Changing an unrelated setting (icon size, theme) no longer recomputes the history lists.
- **App launch:** the app is started first and the launch is recorded afterwards. If it cannot be opened
  (uninstalled or disabled) nothing is recorded and only that package is re-checked, so a stale entry disappears quickly.
- **Startup / memory:** the performance profile (icon cache size, refresh debounce) is applied only when the
  performance mode actually changes.
- Already present from earlier work and kept: bounded size-based icon cache, per-package incremental refresh on
  install / update / remove / enable / disable, on-disk app-list snapshot for instant startup, memory trimming.
- Tests: 7 new unit tests (search index equivalence, app-list update helpers, snapshot codec).

Not measured: frame times were not profiled on a device in this environment, so no specific speed-up figures are claimed.

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

None are needed for the core launcher. App listing uses a `<queries>` entry instead of `QUERY_ALL_PACKAGES`. No INTERNET permission.

Optional, only for the App usage timer (V4.3): `PACKAGE_USAGE_STATS` (Usage Access, granted by you in system settings)
to measure time in limited apps, and `POST_NOTIFICATIONS` (Android 13+) for the "time is up" reminder.

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

No analytics, no ads, no network access, no tracking SDKs. Web search hands the query to your browser; the launcher never sends it. App lists, searches and layout never leave the device.
Backups are disabled so hidden-app choices are not copied to cloud backups.

## Known Android limitations

- **Hidden apps are launcher-level only.** They are not OS-level app hiding: apps stay installed, remain in
  Android Settings, and can be opened by other launchers.
- Android decides whether a launcher can become the default; the app can only ask.
- Locking the screen is not a gesture option: Android only allows it with an Accessibility Service.
- "Recently used" means apps opened from this launcher; Android does not let ordinary apps read other apps' usage.
- Notification badges and weather are not implemented. Badges need notification access and weather would need the
  INTERNET permission.
- **App lock and private apps are launcher-level only.** Other launchers, Android Settings, notifications and recent
  apps can still open those apps. The PIN is salted PBKDF2 and is not bound to the Android Keystore.
- Usage insights only cover apps opened from this launcher.
- Wallpaper blur is experimental and device dependent. Widgets depend on the widget provider apps installed.
- Restoring a backup restores widget *entries*; Android may require you to re-add widgets that cannot be rebound.
- The launcher lock only guards launcher-controlled screens. Anyone with the unlocked phone can still use Android
  Settings, switch launchers, or open hidden apps from there.

## Release signing (later)

`app/build.gradle.kts` reads `SIGNING_KEYSTORE_PATH`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS` and
`SIGNING_KEY_PASSWORD` from environment variables. Store them as GitHub Secrets and decode a base64 keystore in a
workflow step (outline at the bottom of the workflow file). Never commit keystores or passwords.

## Testing

Unit tests (`./gradlew test`) cover preference storage, favorites, hidden apps, folders and their codec, home
ordering, search and filtering, gesture settings and swipe detection, PIN hashing and throttling, launch history
and battery math. Phase 3 adds tests for search ranking, web search URLs, categories, suggestions, insights,
theme math, app lock policy, backup codec/JSON/ZIP validation, widgets codec, performance profiles and device
formatting. V4.3 adds tests for built-in themes (including text contrast in light and dark) and the timer rules
(foreground-time replay, resets, extra time, renewal, codec, PIN throttling). Android UI tests are not included yet.

Note: the Phase 3 code was written without access to the Android SDK. The pure Kotlin logic was compiled and its
tests run separately, but the full Gradle build (`./gradlew test lintDebug assembleDebug`) must be run on GitHub
Actions or locally to confirm Compose/Android code compiles and the DataStore tests pass. The same is true for V4.3:
the usage-event reading, alarm, notification and all new screens have not been built or run on a device.

## Roadmap

Widget hosting, notification badges (opt-in), UI tests, icon packs, backup/restore of layout.
