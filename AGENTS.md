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
| Local DB | Room v10 (9 migrations: 1→10) | `ResultDatabase.kt` — 6 tables: `results`, `reval_courses`, `downloaded_results`, `circulars`, `exam_dates`, `notification_history` |
| Prefs | DataStore Preferences | `PreferenceManager.kt` — profiles, sync intervals, auto-update toggle |
| Scraping | Jsoup (results, circulars), HttpURLConnection (reval) | `ResultScraper.kt`, `RevaluationScraper.kt`, `CircularRepository.kt` |
| Background | WorkManager (PeriodicWorkRequest + foreground) | `ResultSyncWorker.kt`, `RevalSyncWorker.kt`, `ExamDateSyncWorker.kt`, `CircularSyncWorker.kt` |
| Updates | Remote JSON from GitHub | `RemoteConfigRepository.kt`, `UpdateManager.kt` (Auto-download + Install) |
| Web standalone | `index.html` at project root | Vanilla HTML/CSS/JS, separate from Android app |

**Entrypoints**: `ResultWatchApp.kt` (Application), `MainActivity.kt` (Activity).

## Key Quirks

- **Release builds use debug signing** (`signingConfigs.getByName("debug")` in `release` block).
- **Locale hard-filtered** to English and Marathi: `localeFilters += "en", "mr"`.
- **WorkManager initializer removed** from manifest — custom init via `HiltWorkerFactory` in `ResultWatchApp`.
- **Network security**: cleartext allowed for `unipune.ac.in` only; custom RapidSSL CA cert pinned.
- **KSP room.schemaLocation** = `$projectDir/schemas`.
- **Profiles** stored as JSON strings in DataStore `Set<String>` (see `UserProfile.kt`).
- **High refresh rate** (120/144Hz) enabled via reflection on `Window.setFrameRatePowerSavingsBalanced`.
- **Auto-Update**: App checks `config.json` on GitHub and can auto-download/install updates.

## Dependencies (notable versions)

- AGP 9.2.1, Kotlin 2.3.21, KSP 2.3.9
- Compose BOM 2026.05.01, Hilt 2.59.2, Room 2.8.4, WorkManager 2.11.2
- Jsoup 1.22.2, Coil 3.4.0, kotlinx-serialization 1.11.0
- Gradle 9.4.1
- Min SDK 24, Target/Compile SDK 36/37

## Reference

See `SPPUNOTIFY.md` for full source-level project context (1081 lines, comprehensive).
