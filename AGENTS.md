# SPPU Result Watch — Agent Guide

## Build & Run

```bash
./gradlew assembleDebug                                      # debug APK
./gradlew :app:assembleDebug                                 # explicit module
./gradlew check                                              # lint & tests
```
APK at `app/build/outputs/apk/debug/app-debug.apk`.

- **Lint**: Fails on missing translations for Marathi (`mr`). Fix: Use `tools:ignore="MissingTranslation"` or complete `strings.xml`.
- Tests are **placeholders** only — `ExampleUnitTest` / `ExampleInstrumentedTest`. No real test suite exists.
- `buildConfig = false` — do not reference `BuildConfig`.

## Project Structure

Single-module Android app (`:app`). Package `pinak.sppunotify`.

 Layer | Tech | Key files |
---|---|---|
 UI | Jetpack Compose + Navigation Compose | `MainActivity.kt` (entry), `MainScreen.kt` (NavHost with 9 tabs), `SharedTransitionLayout` |
 DI | Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltWorker`) | `AppModule.kt`, `ResultWatchApp.kt` |
 Local DB | Room v11 (10 migrations: 1→11) | `ResultDatabase.kt` — 7 tables: `results`, `reval_courses`, `downloaded_results`, `circulars`, `exam_dates`, `notification_history`, `sync_logs` |
 Prefs | DataStore Preferences | `PreferenceManager.kt` — profiles, sync intervals, auto-update toggle |
 Scraping | Jsoup (results, circulars), HttpURLConnection (reval) | `ResultScraper.kt`, `RevaluationScraper.kt`, `CircularRepository.kt` |
 Background | WorkManager (PeriodicWorkRequest + foreground) | `ResultSyncWorker.kt`, `RevalSyncWorker.kt`, `ExamDateSyncWorker.kt`, `CircularSyncWorker.kt` |
 Updates | Remote JSON from GitHub | `RemoteConfigRepository.kt`, `UpdateManager.kt` (Auto-download + Install) |
 Web standalone | `index.html` at project root | Vanilla HTML/CSS/JS, separate from Android app |

**Entrypoints**: `ResultWatchApp.kt` (Application), `MainActivity.kt` (Activity).

## Key Quirks

- **Release builds use debug signing** (`signingConfigs.getByName("debug")` in `release` block).
- **Locale hard-filtered** to English and Marathi: `localeFilters += "en", "mr"`.
- **WorkManager initializer removed** from manifest — custom init via `HiltWorkerFactory` in `ResultWatchApp`.
- **Network security**: cleartext allowed for `unipune.ac.in` only; custom RapidSSL and DigiCert CA certs pinned in `network_security_config.xml`.
- **KSP room.schemaLocation** = `$projectDir/schemas`.
- **Profiles** stored as JSON strings in DataStore `Set<String>`, encrypted via `CryptoManager` (AES/GCM).
- **High refresh rate** (120/144Hz) enabled via reflection on `Window.setFrameRatePowerSavingsBalanced` in `MainActivity`.
- **Auto-Update**: App checks `config.json` on GitHub and can auto-download/install updates.
- **Deep Links**: Supports `sppuwatch://notify` for home, reval, circulars, and result details.

## Dependencies (notable versions)

- AGP 9.2.1, Kotlin 2.3.21, KSP 2.3.9
- Compose BOM 2026.05.01 (includes `ExperimentalSharedTransitionApi`)
- Hilt 2.59.2, Room 2.8.4, WorkManager 2.11.2
- Jsoup 1.22.2, Coil 3.4.0, kotlinx-serialization 1.11.0
- Gradle 9.4.1
- Min SDK 23, Target/Compile SDK 37 (Android 16 support)

## Recent Changes (v1.5.0)
- **UI Redesign**: 
  - Enhanced Result Cards with departmental icons (Engineering, Pharmacy, etc.) for better scannability.
  - Added "Result Frequency" chart in details view to track publication trends.
  - Unified Background Sync settings into a single frequency control.
  - Added Profile Quick-Switcher in the Result View screen for faster form filling.
- **Cleanup**:
  - Removed redundant FAB actions in Home screen (simplified to Pull-to-refresh).
  - Removed standalone `index.html` from project root.
  - Re-enabled `BuildConfig` for better version tracking in code.
- **Backend**: Consolidated WorkManager sync logic to use a global interval.

## Reference

See `SPPUNOTIFY.md` for full source-level project context.
