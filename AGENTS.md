# SPPU Result Watch — Agent Guide

## Build & Run

```bash
./gradlew assembleDebug                                      # debug APK
./gradlew :app:assembleDebug                                 # explicit module
```
APK at `app/build/outputs/apk/debug/app-debug.apk`.

- Tests are **placeholders** only — `ExampleUnitTest` / `ExampleInstrumentedTest`. No real test suite exists.
- No lint, typecheck, or CI config beyond default Android Studio.
- `buildConfig = false` — do not reference `BuildConfig`.

## Project Structure

Single-module Android app (`:app`). Package `pinak.sppunotify`.

| Layer | Tech | Key files |
|---|---|---|
| UI | Jetpack Compose + Navigation Compose | `MainActivity.kt` (entry), `MainScreen.kt` (NavHost with 8 tabs) |
| DI | Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltWorker`) | `AppModule.kt`, `ResultWatchApp.kt` |
| Local DB | Room v5 (4 migrations: 1→5) | `ResultDatabase.kt` — 3 tables: `results`, `reval_courses`, `downloaded_results` |
| Prefs | DataStore Preferences | `PreferenceManager.kt` — profiles stored as JSON in `Set<String>` |
| Scraping | Jsoup (results), HttpURLConnection (reval) | `ResultScraper.kt`, `RevaluationScraper.kt` |
| Background | WorkManager (PeriodicWorkRequest + foreground) | `ResultSyncWorker.kt`, `RevalSyncWorker.kt`, `WorkManagerHelper.kt` |
| Updates | Remote JSON from GitHub | `RemoteConfigRepository.kt`, `UpdateManager.kt`, `UpdateReceiver.kt` |
| Web standalone | `index.html` at project root | Vanilla HTML/CSS/JS, separate from Android app |

**Entrypoints**: `ResultWatchApp.kt` (Application), `MainActivity.kt` (Activity).

## Key Quirks

- **Release builds use debug signing** (`signingConfigs.getByName("debug")` in `release` block).
- **Locale hard-filtered** to English only: `localeFilters += "en"`.
- **WorkManager initializer removed** from manifest — custom init via `HiltWorkerFactory` in `ResultWatchApp`.
- **Network security**: cleartext allowed for `unipune.ac.in` only; custom RapidSSL CA cert pinned.
- **KSP room.schemaLocation** = `$projectDir/schemas`.
- **Profiles** stored as JSON strings in DataStore `Set<String>` (see `UserProfile.kt`).
- **High refresh rate** (120/144Hz) enabled via reflection on `Window.setFrameRatePowerSavingsBalanced`.
- **`service/` directory exists but is empty**.

## Dependencies (notable versions)

- AGP 8.8.1, Kotlin 2.1.10, KSP 2.1.10-1.0.29
- Compose BOM 2025.02.00, Hilt 2.55, Room 2.6.1, WorkManager 2.10.0
- Jsoup 1.18.3, Coil 2.7.0, kotlinx-serialization 1.7.3
- Gradle 9.4.1
- Min SDK 24, Target/Compile SDK 36

## Reference

See `SPPUNOTIFY.md` for full source-level project context (1081 lines, comprehensive).
