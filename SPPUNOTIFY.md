# SPPU Result Watch — Complete Project Context for LLMs

## Project Identity

- **App Name**: SPPU Result Watch
- **Package**: `pinak.sppunotify`
- **Application ID**: `pinak.sppunotify`
- **Version**: 1.4.0 (versionCode 6)
- **Min SDK**: 24 | **Target SDK**: 36 | **Compile SDK**: 36
- **Language**: Kotlin 2.1.10
- **UI Framework**: Jetpack Compose (Material 3) + Navigation Compose
- **Architecture**: MVVM + Repository + Hilt DI + Room + WorkManager
- **License**: MIT
- **GitHub**: `github.com/thepinak503/sppuresult-android`
- **Author**: Pinak Dhabu (FOSS Enthusiast)
- **Root Project Dir**: `/home/pinak/AndroidStudioProjects/SPPUResultNotify`

---

## Build Configuration

### Root `settings.gradle.kts`
```kotlin
rootProject.name = "SPPU Result Notify"
include(":app")
pluginManagement with Google/MavenCentral repos.
Uses foojay-resolver-convention 1.0.0
```

### Root `build.gradle.kts`
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}
```

### `app/build.gradle.kts`
- Plugins: android.application, kotlin.android, kotlin.compose, hilt, ksp, kotlin.plugin.serialization 2.1.10
- `namespace = "pinak.sppunotify"`
- `compileSdk = 36`, `minSdk = 24`, `targetSdk = 36`
- `versionCode = 6`, `versionName = "1.4.0"`
- `buildFeatures { compose = true; buildConfig = false }`
- `compileOptions { JavaVersion.VERSION_17 }`
- Release: minifyEnabled, shrinkResources, proguard, signing with debug key
- `ndk { debugSymbolLevel = "none" }`
- Packaging excludes META-INF and kotlin_module files
- Locale filter: only English (`localeFilters += "en"`)
- Dependencies:
  - **Core**: androidx.core.ktx, lifecycle-runtime-ktx, lifecycle-viewmodel-compose, activity-compose
  - **Compose**: BOM 2025.02.00, ui, ui-graphics, ui-tooling-preview, foundation, material3, material-icons-extended, navigation-compose
  - **Hilt**: hilt-android 2.55, hilt-compiler (ksp), hilt-navigation-compose 1.2.0, hilt-work 1.2.0, hilt-compiler (ksp)
  - **Room**: room-runtime 2.6.1, room-ktx, room-compiler (ksp)
  - **WorkManager**: work-runtime-ktx 2.10.0
  - **DataStore**: datastore-preferences 1.1.2
  - **Serialization**: kotlinx-serialization-json 1.7.3
  - **Scraper**: jsoup 1.18.3, coil-compose 2.7.0
  - **Testing**: junit 4.13.2, androidx-junit, espresso-core

### `gradle/libs.versions.toml` (Version Catalog)
- AGP 8.8.1, Kotlin 2.1.10, KSP 2.1.10-1.0.29
- coreKtx 1.15.0, lifecycle 2.8.7, activityCompose 1.10.0
- composeBom 2025.02.00, navigation 2.8.7
- room 2.6.1, work 2.10.0, datastore 1.1.2
- serialization 1.7.3, hilt 2.55, hiltCompose 1.2.0
- jsoup 1.18.3, coil 2.7.0

### `gradle.properties`
- JVM args: -Xmx2048m, UTF-8
- kotlin.code.style=official
- android.useAndroidX=true, android.enableJetifier=false
- android.enableR8.fullMode=true
- android.suppressUnsupportedCompileSdk=36

### `gradle/wrapper/gradle-wrapper.properties`
- Gradle 9.4.1

### `local.properties`
- sdk.dir=/home/pinak/Android/Sdk

---

## AndroidManifest.xml

```xml
<manifest package="pinak.sppunotify">
  <uses-permission android:name="android.permission.INTERNET" />
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
  <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
  <uses-permission android:name="android.permission.WAKE_LOCK" />
  <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
  
  <application android:name=".ResultWatchApp"
    android:networkSecurityConfig="@xml/network_security_config"
    android:allowBackup="true"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:theme="@style/Theme.SPPUResultNotify">
    
    <activity android:name=".MainActivity" android:exported="true" android:label="@string/app_name">
      <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
      </intent-filter>
    </activity>
    
    <service android:name="androidx.work.impl.foreground.SystemForegroundService"
      android:foregroundServiceType="dataSync" tools:node="merge" />
    
    <receiver android:name=".receiver.BootReceiver" android:enabled="true" android:exported="false">
      <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
      </intent-filter>
    </receiver>
    
    <provider android:name="androidx.core.content.FileProvider"
      android:authorities="${applicationId}.fileprovider"
      android:exported="false" android:grantUriPermissions="true">
      <meta-data android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
    </provider>
    
    <provider android:name="androidx.startup.InitializationProvider"
      android:authorities="${applicationId}.androidx-startup"
      android:exported="false" tools:node="merge">
      <meta-data android:name="androidx.work.WorkManagerInitializer"
        android:value="androidx.startup" tools:node="remove" />
    </provider>
  </application>
</manifest>
```

---

## ProGuard Rules (`app/proguard-rules.pro`)
- Keep: Service, Application, Activity, ContentProvider, BroadcastReceiver, View subclasses
- Keep: Hilt Dagger classes
- Keep: okhttp3, okio, retrofit2 (dontwarn)
- Keep: kotlinx.serialization, @Serializable classes
- Keep: `pinak.sppunotify.data.remote.**` and `pinak.sppunotify.data.local.**`

---

## Notifications Channels (4)
| Channel ID | Name | Importance | Description |
|---|---|---|---|
| `result_notifications` | Result Updates | HIGH | New results published |
| `download_notifications` | Result Downloads | DEFAULT | Download status |
| `background_sync` | Background Sync | LOW | Background sync service (no badge) |
| `reval_notifications` | Revaluation Updates | HIGH | New revaluation courses |

---

## Resource Files

### `values/strings.xml`
- app_name, disclaimer_title/message, settings/bg_sync strings, sync_service strings

### `values/themes.xml`
- `Theme.SPPUResultNotify` (parent: `android:Theme.Material.Light.NoActionBar`)
- `android:windowIsFrameRatePowerSavingsBalanced` = false (API 35+)

### `values/colors.xml`
- Purple/teal/black/white palette (default Android Studio template colors)

### `xml/network_security_config.xml`
- Cleartext allowed for `unipune.ac.in` (with subdomains)
- Custom RapidSSL TLS RSA CA G1 certificate pinned via `@raw/rapidssl_tls_rsa_ca_g1.pem`
- System certificates for all other domains
- Base config: cleartext NOT permitted

### `xml/backup_rules.xml` / `xml/data_extraction_rules.xml`
- Placeholder/sample rules (not customized)

### `xml/file_paths.xml`
- Cache-path FileProvider for cache directory

### Drawables
- `ic_launcher_background.xml`: Green grid (Android Studio default)
- `ic_launcher_foreground.xml`: Android robot head with gradient
- `ic_notification.xml`: Bell/notification icon (24dp vector)
- Adaptive icons in `mipmap-anydpi-v26/`
- Launcher icons in mipmap-hdpi through xxxhdpi (webp)

### `raw/rapidssl_tls_rsa_ca_g1.pem`
- Custom CA certificate for SPPU HTTPS connections

---

## Complete Kotlin Source Code

### 1. `ResultWatchApp.kt` (Application class)
```kotlin
@HiltAndroidApp
class ResultWatchApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var preferenceManager: PreferenceManager
    @Inject lateinit var workManagerHelper: WorkManagerHelper

    // Creates 4 notification channels on startup
    // Observes preferences -> updates WorkManager sync work
}
```

### 2. `MainActivity.kt` (Entry Activity)
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // Edge-to-edge display
    // Disables frame rate power savings (API 35+ reflection)
    // Sets high refresh rate (up to 144Hz) via display modes + reflection
    // 500ms polling interval for refresh rate
    // Shows SetupPopup overlay chain:
    //   1. No Internet → Retry/Exit
    //   2. Disclaimer (first launch) → Agree/Exit
    //   3. Notification permission request (API 33+)
    // Once accepted + online: composable MainScreen()
}
```

### 3. `AppModule.kt` (Hilt DI)
```kotlin
@Module @InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideResultDatabase(@ApplicationContext context: Context): ResultDatabase
    fun provideRevalCourseDao(db: ResultDatabase): RevalCourseDao
    fun provideDownloadedResultDao(db: ResultDatabase): DownloadedResultDao
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager
}
```

### 4. `ResultEntity.kt`
```kotlin
@Entity(tableName = "results")
data class ResultEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val publishedDate: String,
    val publishedTimestamp: Long = 0L,
    val patternName: String = "",
    val patternId: String = "",
    val department: String = "Other UG",
    val fetchedAt: Long = System.currentTimeMillis()
)
```

### 5. `ResultDao.kt`
```kotlin
@Dao
interface ResultDao {
    @Query("SELECT * FROM results ORDER BY 
      CASE WHEN publishedTimestamp > 0 THEN publishedTimestamp ELSE 0 END DESC,
      substr(publishedDate, 7, 4) DESC,
      CASE substr(publishedDate, 4, 3) WHEN 'Jan' THEN 1 ... WHEN 'Dec' THEN 12 END DESC,
      substr(publishedDate, 1, 2) DESC")
    fun getAllResults(): Flow<List<ResultEntity>>

    @Insert(onConflict = REPLACE) suspend fun insertResults(results: List<ResultEntity>)
    @Query("SELECT id FROM results") suspend fun getAllResultIds(): List<String>
    @Query("SELECT COUNT(*) FROM results") suspend fun getCount(): Int
    @Query("DELETE FROM results") suspend fun clearAll()
}
```

### 6. `RevalCourseEntity.kt`
```kotlin
@Entity(tableName = "reval_courses")
data class RevalCourseEntity(
    @PrimaryKey val eventTarget: String,
    val course: String,
    val subject: String,
    val firstSeenAt: Long = System.currentTimeMillis()
)
```

### 7. `RevalCourseDao.kt`
```kotlin
@Dao
interface RevalCourseDao {
    @Query("SELECT eventTarget FROM reval_courses") suspend fun getAllEventTargets(): List<String>
    @Insert(onConflict = REPLACE) suspend fun insertCourses(courses: List<RevalCourseEntity>)
    @Query("DELETE FROM reval_courses") suspend fun clearAll()
}
```

### 8. `DownloadedResultEntity.kt`
```kotlin
@Entity(tableName = "downloaded_results")
data class DownloadedResultEntity(
    @PrimaryKey val id: String,
    val title: String,
    val profileName: String,
    val fileName: String,
    val filePath: String,
    val mimeType: String,
    val downloadDate: Long = System.currentTimeMillis()
)
```

### 9. `DownloadedResultDao.kt`
```kotlin
@Dao
interface DownloadedResultDao {
    @Query("SELECT * FROM downloaded_results ORDER BY downloadDate DESC")
    fun getAllDownloadedResults(): Flow<List<DownloadedResultEntity>>
    @Insert(onConflict = REPLACE) suspend fun insertDownloadedResult(result: DownloadedResultEntity)
    @Delete suspend fun deleteDownloadedResult(result: DownloadedResultEntity)
    @Query("SELECT * FROM downloaded_results WHERE id = :id") suspend fun getDownloadedResultById(id: String): DownloadedResultEntity?
}
```

### 10. `ResultDatabase.kt` (Room DB v5)
```kotlin
@Database(
    entities = [ResultEntity::class, RevalCourseEntity::class, DownloadedResultEntity::class],
    version = 5, exportSchema = false
)
abstract class ResultDatabase : RoomDatabase() {
    abstract val dao: ResultDao
    abstract val revalDao: RevalCourseDao
    abstract val downloadedDao: DownloadedResultDao

    companion object {
        // MIGRATION 1→2: Add patternName, patternId columns
        // MIGRATION 2→3: Add department column
        // MIGRATION 3→4: Create reval_courses table
        // MIGRATION 4→5: Create downloaded_results table
    }
}
```

### 11. `PreferenceManager.kt` (DataStore)
```kotlin
@Singleton
class PreferenceManager @Inject constructor(@ApplicationContext private val context: Context) {
    // Keys: notifications_enabled, result_sync_interval_min, reval_sync_interval_min,
    //       watchlist_keywords, user_profiles, active_profile_id
    
    val preferencesFlow: Flow<UserPreferences>  // maps all keys to UserPreferences data class
    
    suspend fun updateNotificationsEnabled(enabled: Boolean)
    suspend fun updateResultSyncInterval(intervalMinutes: Int)
    suspend fun updateRevalSyncInterval(intervalMinutes: Int)
    suspend fun addKeyword(keyword: String)
    suspend fun removeKeyword(keyword: String)
    suspend fun saveProfile(name: String, seatNo: String, motherName: String)
    suspend fun deleteProfile(profileId: String)
    suspend fun setActiveProfile(profileId: String)
}

data class UserPreferences(
    val notificationsEnabled: Boolean,
    val resultSyncInterval: Int,      // Default 15 min
    val revalSyncInterval: Int,        // Default 60 min
    val watchlistKeywords: Set<String>,
    val profiles: List<UserProfile>,
    val activeProfileId: String?
)
```

### 12. `UserProfile.kt` (Serializable)
```kotlin
@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val seatNo: String,
    val motherName: String
)

object ProfileSerializer {
    fun serializeList(profiles: List<UserProfile>): Set<String>
    fun deserializeList(profileStrings: Set<String>): List<UserProfile>
}
// Profiles are stored as JSON-encoded strings in DataStore Set<String>
```

### 13. `ResultDto.kt`
```kotlin
data class ResultDto(
    val id: String,
    val title: String,
    val url: String,
    val published: String,
    val patternName: String = "",
    val patternId: String = "",
)
```

### 14. `ResultScraper.kt` (Jsoup-based scraper)
```kotlin
@Singleton
class ResultScraper @Inject constructor() {
    companion object {
        const val BASE_URL = "https://onlineresults.unipune.ac.in"
        const val DASHBOARD_URL = "$BASE_URL/Result/Dashboard/Default"
        
        fun parseDateToTimestamp(dateStr: String): Long
    }

    suspend fun checkServerHealth(): ServerStatus       // HEAD request to BASE_URL
    suspend fun scrapeLatestResults(): List<ResultDto>   // Multi-session parallel scraping
    suspend fun fetchCaptcha(): CaptchaData?              // POST RFCTLN -> JSON {CaptchaImageSTR, OrgCaptchaText}
    suspend fun validateCaptcha(userText: String, orgText: String): Boolean  // POST VALCHCT
    suspend fun submitResult(patternName, patternId, seatNo, motherName, captchaText, orgCaptchaText, captchaImageStr): SubmitResult?
    
    // Internal:
    // - fetchSessionPeriods(): GET GetSession -> JSON array of Exam_Period IDs
    // - extractPatternParam(html, index): Regex parse of Javascript Enterdetails() calls
}
```

### 15. `RevaluationDto.kt`
```kotlin
data class RevalCourse(val course: String, val subject: String, val eventTarget: String)
data class RevalResult(val html: String)
```

### 16. `RevaluationScraper.kt` (HttpURLConnection-based)
```kotlin
@Singleton
class RevaluationScraper @Inject constructor() {
    const val REVAL_URL = "https://pun.unipune.ac.in/revalresult/"
    
    suspend fun scrapeCourses(): List<RevalCourse>
    // ASP.NET postback-based pagination (up to 100 pages)
    // Maintains cookies across requests
    // Parses __doPostBack() event targets
    // Extracts courses from GridViewRowStyle/AlternatingRowStyle table rows
}
```

### 17. `ServerStatus.kt`
```kotlin
data class ServerStatus(
    val isOnline: Boolean,
    val responseTimeMs: Long,
    val lastChecked: Long = System.currentTimeMillis()
) {
    val statusLevel: StatusLevel  // HEALTHY (<2s), SLOW (>2s), DOWN
}
enum class StatusLevel { HEALTHY, SLOW, DOWN }
```

### 18. `CircularRss.kt`
```kotlin
data class CircularRssItem(
    val title: String,
    val link: String,
    val description: String,
    val pubDate: String
)
```

### 19. `ResultRepository.kt`
```kotlin
@Singleton
class ResultRepository @Inject constructor(
    private val scraper: ResultScraper,
    private val db: ResultDatabase,
) {
    val results: Flow<List<ResultEntity>> = db.dao.getAllResults()       // Offline-first
    val serverStatus: StateFlow<ServerStatus?>

    suspend fun updateServerStatus()
    suspend fun fetchResults(): List<ResultDto>     // Scrape + diff + insert + return new items
    suspend fun getCachedCount(): Int
}
```

### 20. `RevalRepository.kt`
```kotlin
@Singleton
class RevalRepository @Inject constructor(
    private val scraper: RevaluationScraper,
    private val dao: RevalCourseDao,
) {
    suspend fun checkForNewCourses(): List<RevalCourse>  // Scrape + diff + insert + return new
}
```

### 21. `CircularRepository.kt`
```kotlin
@Singleton
class CircularRepository @Inject constructor() {
    // 3 RSS feeds:
    // - Exam Circulars
    // - Important Circulars  
    // - Academic Calendar
    
    suspend fun fetchAllCirculars(): List<CircularRssItem>
    // Jsoup XML parsing, dedup by link
}
```

### 22. `VaultRepository.kt`
```kotlin
@Singleton
class VaultRepository @Inject constructor(private val dao: DownloadedResultDao) {
    val downloadedResults: Flow<List<DownloadedResultEntity>>
    suspend fun saveDownloadedResult(result: DownloadedResultEntity)
    suspend fun deleteResult(result: DownloadedResultEntity)
}
```

### 23. `BootReceiver.kt`
```kotlin
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var workManagerHelper: WorkManagerHelper
    @Inject lateinit var preferenceManager: PreferenceManager
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_BOOT_COMPLETED) {
            // Reschedule sync work after reboot based on current preferences
        }
    }
}
```

### 24. `WorkManagerHelper.kt`
```kotlin
@Singleton
class WorkManagerHelper @Inject constructor(private val workManager: WorkManager) {
    companion object {
        const val RESULT_SYNC_WORK_NAME = "ResultSyncWork"
        const val REVAL_SYNC_WORK_NAME = "RevalSyncWork"
    }
    
    fun updateSyncWork(preferences: UserPreferences)
    private fun scheduleResultSync(intervalMinutes: Int)  // PeriodicWorkRequest with NetworkType.CONNECTED
    private fun scheduleRevalSync(intervalMinutes: Int)    // PeriodicWorkRequest with NetworkType.CONNECTED
    fun cancelAllSync()
}
```

### 25. `ResultSyncWorker.kt` (Foreground HiltWorker)
```kotlin
@HiltWorker
class ResultSyncWorker @AssistedInject constructor(
    context: Context, workerParams: WorkerParameters,
    private val repository: ResultRepository,
    private val preferenceManager: PreferenceManager
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        // Set foreground with dataSync type
        // Fetch new results
        // Filter by watchlist keywords
        // Send notification for each match
    }
}
```

### 26. `RevalSyncWorker.kt` (Foreground HiltWorker)
```kotlin
@HiltWorker
class RevalSyncWorker @AssistedInject constructor(
    context: Context, workerParams: WorkerParameters,
    private val repository: RevalRepository
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        // Set foreground with dataSync type
        // Check new reval courses
        // Send notification if count > 0 (retry up to 3 times)
    }
}
```

### 27. `NotificationHelper.kt`
```kotlin
class NotificationHelper(private val context: Context) {
    fun showRevalNotification(count: Int)           // CHANNEL_REVAL, HIGH priority
    fun showResultNotification(title: String, message: String)  // CHANNEL_RESULTS with group summary
    fun showDownloadNotification(success: Boolean, fileName: String)  // CHANNEL_DOWNLOADS
}
```

### 28. `DepartmentClassifier.kt`
```kotlin
object DepartmentClassifier {
    val departments = listOf("All", "FE", "SE", "TE", "BE", "MBA", "MCA", "M.Sc", 
        "M.A./M.Com", "B.Sc", "B.Com", "BBA/BCA", "B.A.", "B.Pharm", "Other UG", "Other PG", "Law", "Diploma")
    
    fun classify(title: String): String
    // Uses ~30 regex patterns to classify result titles into departments
    // Handles: FE/SE/TE/BE codes, MBA/MCA/M.Sc/M.A./M.Com, Law, Diploma,
    //   B.Sc/B.Com/B.A./BBA/BCA/B.Pharm, Other UG/PG, Master of... patterns
}
```

### 29. `FileSaver.kt`
```kotlin
object FileSaver {
    fun saveToUri(context: Context, bytes: ByteArray, uri: Uri): Boolean
    fun saveToDownloads(context: Context, bytes: ByteArray, fileName: String, mimeType: String): Uri?
    // API 29+: MediaStore.Downloads
    // Pre-API 29: Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)
}
```

### 30. `Theme.kt`
```kotlin
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun SPPUResultWatchTheme(themeMode: ThemeMode = SYSTEM, dynamicColor: Boolean = true, content: @Composable () -> Unit)
// - SYSTEM/LIGHT/DARK mode selection
// - Android 12+ dynamic color support (with overridden dark bg/surface)
// - Transparent status/nav bars
// - Light/dark appearance control
// - SPPU blue custom palette fallback
```

### 31. `Color.kt`
```kotlin
// Light: SppuBlue(#1A5276), BlueSecondary(#2E86C1), Accent(#F39C12)
// Dark: Primary(#5DADE2), PrimaryContainer(#1A5276)
val PrimaryLight = Color(0xFF1A5276)      val PrimaryDark = Color(0xFF5DADE2)
val SecondaryLight = Color(0xFF5D6D7E)    val SecondaryDark = Color(0xFFAEB6BF)
val BackgroundDark = Color(0xFF121212)     val SurfaceDark = Color(0xFF1E1E1E)
```

### 32. `Type.kt` — Default Material Typography (bodyLarge customized)

---

## UI Screens

### 33. `MainScreen.kt` — Navigation Host
```kotlin
sealed class Screen(val route, val label, val icon, val selectedIcon)
// 8 screens: Home("Results"), Revaluation("Reval"), Circulars, Calculator("Calc"),
//            Vault, Links, Settings, About

@Composable fun MainScreen()
// NavHost with 8 tab composables + 2 detail routes (details/{resultId}, resultView/{resultId})
// Animated transitions: slide horizontally between tabs, scale+fade for detail pages
// Portrait: rounded bottom bar with 7 tabs + separate About button
// Landscape: vertical sidebar with all 8 tabs
// Re-tap active tab scrolls to top
// SharedTransitionLayout for hero animations on result cards
```

### 34. `HomeScreen.kt` — Main Results List
```kotlin
@Composable fun HomeScreen(viewModel: HomeViewModel, onResultClick, listState, sharedTransitionScope, animatedVisibilityScope)
// Components:
// - CenterAlignedTopAppBar: "SPPU Result Watch" title + server status indicator (green/yellow/red dot)
// - Action icons: Sort (ModalBottomSheet), Refresh (rotating icon)
// - SearchBar: expands with recommended departments (FlowRow chips) + instant result previews
// - Department FilterChips LazyRow (All, FE, SE, TE, BE, ...)
// - Result count + sort order label
// - PullToRefreshBox with LazyColumn of ResultCards
// - Empty state with animated pulsing search icon
// - LazyScrollbar for fast scrolling
// - Error state: AlertDialog for network/server errors
```

### 35. `HomeViewModel.kt`
```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: ResultRepository) : ViewModel()
// State: searchQuery, selectedDepartment("All"), sortOrder(NEWEST_FIRST), isRefreshing, totalCount
// serverStatus from repository
// departments from DepartmentClassifier.departments
// results: StateFlow — combines raw results + search + department + sort using combine
//   - Fuzzy search with token matching (character subsequence matching)
//   - Ranking: title(100x) > patternName(50x) > publishedDate(25x)
//   - Sort: NEWEST_FIRST, OLDEST_FIRST, NAME_A_Z, NAME_Z_A
// On refresh: fetch from scraper, show snackbar with count of new results
// Error handling: 502/503/504/timeout specific messages
```

### 36. `ResultDetailsScreen.kt` — Result Detail View
```kotlin
@Composable fun ResultDetailsScreen(result, onBackClick, onOpenBrowser, onViewInApp, sharedTransitionScope, animatedVisibilityScope)
// Components:
// - LargeTopAppBar with exit-until-collapsed scroll behavior
// - Share action button (ACTION_SEND with title + URL)
// - Pulsing card (1.0 → 1.015 scale) with "OFFICIAL ANNOUNCEMENT" badge
// - Shared element transitions (card, title, date)
// - Info card: "Redirecting to official SPPU portal"
// - "View Result in App" and "Open in Browser" buttons
```

### 37. `DetailsViewModel.kt`
```kotlin
@HiltViewModel
class DetailsViewModel @Inject constructor(repository: ResultRepository, savedStateHandle: SavedStateHandle) : ViewModel()
// Finds result by ID from repository.results flow
```

### 38. `ResultViewScreen.kt` — Individual Result View (Seat No + Captcha)
```kotlin
@Composable fun ResultViewScreen(viewModel: ResultViewViewModel, onBackClick)
// Components:
// - Result title + published date
// - Seat No OutlinedTextField
// - Mother Name OutlinedTextField
// - CAPTCHA image display (Base64 → Bitmap) with "Try a different captcha" link
// - Captcha text OutlinedTextField (max 5 chars)
// - "Check Result" Button (with loading state)
// - Uses CreateDynamicDocument contract for SAF file save
// - Notification on download complete
// - Error dialogs for captcha failures / server busy
```

### 39. `ResultViewViewModel.kt`
```kotlin
@HiltViewModel
class ResultViewViewModel @Inject constructor(
    scraper: ResultScraper, repository: ResultRepository, 
    vaultRepository: VaultRepository, preferenceManager: PreferenceManager,
    savedStateHandle: SavedStateHandle
) : ViewModel()
// State: result, captchaBitmap, orgCaptchaText, captchaImageStr, isLoading, error, resultBytes, resultMimeType, savedSeatNo, savedMotherName, activeProfileName
// loadResult() → find by resultId → loadCaptcha()
// loadCaptcha() → fetch base64 → decode to Bitmap
// submitForm() → validate fields → validateCaptcha → submitResult → show save dialog → save to vault
// onResultSaved() → save DownloadedResultEntity via VaultRepository
```

### 40. `RevaluationScreen.kt`
```kotlin
@Composable fun RevaluationScreen(listState)
// Scrapes revaluation courses from pun.unipune.ac.in/revalresult/
// SearchBar with quick tag chips + instant match previews
// Sort: Default, A-Z, Z-A
// LazyColumn of RevalCourseCards with course name, subject, "Open" button
// LazyScrollbar
// Pull-to-refresh via refresh button
```

### 41. `CircularsScreen.kt`
```kotlin
@HiltViewModel
class CircularsViewModel @Inject constructor(repository: CircularRepository) : ViewModel()
// Fetches 3 RSS feeds (Exam Circulars, Important Circulars, Academic Calendar)

@Composable fun CircularsScreen(viewModel: CircularsViewModel)
// PullToRefreshBox with LazyColumn of CircularCards
// Each card: title, pubDate, Description icon, OpenInNew icon
```

### 42. `CalculatorScreen.kt` — SGPA/CGPA to Percentage
```kotlin
@Composable fun CalculatorScreen()
// Conversion patterns:
// - Standard: (Grade - 0.75) * 10
// - Circular 322/2020: GPA * 8.9
// - Engineering 2022: GPA * 8.8
// - Range-Based (Circular 332): tiered formulas for different GPA ranges
// Grade letter (O/A+/A/B+/B/C/P/F) and class (First Class with Distinction, etc.)
```

### 43. `LinksScreen.kt`
```kotlin
@Composable fun LinksScreen(onBackClick, isTopLevel, scrollState)
// Categorized list of 25 SPPU-related URLs
// Categories: Main, Results, Exam, Syllabus (2016-2026), PhD Syllabus, Other, Circulars
// Advisory card: "Use Desktop Site mode" recommendation
// LazyScrollbar
```

### 44. `SettingsScreen.kt`
```kotlin
@Composable fun SettingsScreen(scrollState, viewModel: SettingsViewModel)
// Sections:
// Background Sync — toggle + Result interval (15/30/60 min) + Reval interval (30/60/120 min)
// Saved Profiles — add/delete/select active (auto-fills seat no + mother name)
// Smart Watchlist — add/remove keywords, only notify for matching results
// App Theme — SYSTEM/LIGHT/DARK with radio buttons
// Battery Optimization — status + direct intent to system settings
// Notifications Permission — (API 33+) direct intent to notification settings
```

### 45. `SettingsViewModel.kt`
```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(preferenceManager: PreferenceManager) : ViewModel()
// Delegates to PreferenceManager for all CRUD operations
```

### 46. `VaultScreen.kt` — Downloaded Results
```kotlin
@Composable fun VaultScreen(viewModel: VaultViewModel)
// Lists saved PDFs with title, profile name, download date
// Open in viewer (ACTION_VIEW with URI permission)
// Delete with confirmation
// Empty state: "No marksheets saved yet"
```

### 47. `VaultViewModel.kt`
```kotlin
@HiltViewModel
class VaultViewModel @Inject constructor(repository: VaultRepository) : ViewModel()
// Exposes downloadedResults flow, handles deletion
```

### 48. `AboutScreen.kt`
```kotlin
@Composable fun AboutScreen(scrollState)
// Developer photo (Coil AsyncImage from GitHub avatar)
// Name + title
// GitHub link
// "About SPPU Result Watch" card with feature list
// "NOT AN OFFICIAL APP" warning card
// "Data Privacy & Handling" card (expandable technical details)
// "Legal Disclaimer" card (expandable full legal text)
// "Open Source" card
// Version + Copyright
```

### 49. `LazyScrollbar.kt`
```kotlin
@Composable fun LazyScrollbar(listState: LazyListState, modifier: Modifier)
// Custom drag-to-scroll scrollbar for LazyColumn
// Calculates thumb position from scroll state
// Supports direct drag gestures
// Canvas-drawn rounded rect thumb + track
```

---

## Architecture Summary

```
ResultWatchApp (@HiltAndroidApp)
├── MainActivity (@AndroidEntryPoint)
│   └── MainScreen (Compose Navigation)
│       ├── HomeScreen → HomeViewModel
│       │   └── ResultRepository → ResultScraper + Room DB (results table)
│       ├── ResultDetailsScreen → DetailsViewModel (lookup by ID)
│       ├── ResultViewScreen → ResultViewViewModel
│       │   └── ResultScraper (captcha + submit) + VaultRepository (save)
│       ├── RevaluationScreen (inline ViewModel)
│       │   └── RevaluationScraper (HttpURLConnection)
│       ├── CircularsScreen → CircularsViewModel
│       │   └── CircularRepository (RSS feeds)
│       ├── CalculatorScreen (standalone)
│       ├── LinksScreen (standalone)
│       ├── VaultScreen → VaultViewModel
│       │   └── VaultRepository → Room DB (downloaded_results)
│       ├── SettingsScreen → SettingsViewModel
│       │   └── PreferenceManager (DataStore)
│       └── AboutScreen (standalone)
├── BootReceiver (re-schedule WorkManager on reboot)
├── ResultSyncWorker (periodic foreground → ResultRepository)
├── RevalSyncWorker (periodic foreground → RevalRepository)
└── AppModule (DI: Room DB, DAOs, WorkManager)
```

## Data Flow

### Result Browsing Flow
1. HomeScreen loads → HomeViewModel.init → ResultRepository.results Flow (from Room DB)
2. ViewModel combines results + search + filter + sort → StateFlow
3. Pull-to-refresh → viewModel.refresh() → ResultScraper scrapes SPPU portal → new items diffed → inserted into Room → Flow auto-updates
4. Tap result → nav to ResultDetailsScreen (shared element animation)

### Individual Result Viewing Flow
1. ResultViewScreen loads → fetches CAPTCHA from SPPU server
2. User enters seat no, mother name, captcha text
3. Captcha validated → result submitted → bytes returned
4. SAF file picker → save to user-chosen location
5. Save metadata to Vault (Room: downloaded_results table)

### Background Sync Flow
1. App open → PreferenceManager observed → WorkManagerHelper schedules PeriodicWorkRequests
2. ResultSyncWorker runs every N min (foreground, dataSync type)
3. Scrapes results → diffs → keyword filters → notifications
4. RevalSyncWorker runs every N min → scrapes revaluation courses → diffs → notifications
5. BootReceiver re-enqueues work after device reboot

---

## Key Technical Details

### Network Layer
- **Jsoup** for SPPU online result portal (HTML scraping + session management)
- **HttpURLConnection** for revaluation portal (ASP.NET postback handling)
- Custom RapidSSL certificate pinned in network_security_config.xml
- Cleartext allowed for `unipune.ac.in` (HTTP links in syllabus/circulars)
- User-Agent: Chrome 122 on Windows

### Security
- All personal data (seat no, mother name) sent ONLY to SPPU servers via HTTPS
- No analytics, no telemetry, no third-party network calls
- HiltWorker for DI in WorkManager
- FileProvider for cached content sharing

### Performance
- High refresh rate (up to 144Hz) via reflection on `Window.setFrameRatePowerSavingsBalanced`
- `isFrameRatePowerSavingsBalanced = false` in themes.xml (API 35+)
- R8 full mode + resource shrinking in release build
- NDK debug symbols stripped in release
- English-only locale filter
- buildConfig disabled

### UI/Animation
- Shared Element Transitions (sharedBounds/sharedElement) between HomeScreen and ResultDetailsScreen
- LazyScrollbar with drag-to-scroll
- Spring animations (Low Bouncy damping) for card presses and icon scales
- AnimatedVisibility with slide + fade for popups and list items
- Pull-to-refresh with Material3 PullToRefreshBox
- Server status indicator (green/yellow/red dot)
- Pulsing card animation on detail screen

### Data Persistence
- Room DB (version 5): results, reval_courses, downloaded_results tables
- DataStore Preferences: settings, profiles, watchlist keywords
- Profiles stored as JSON-serialized strings in Set<String>
- Full backup rules (uncommented/placeholder)

---

## Gradle Dependencies Galaxy

| Dependency | Version | Usage |
|---|---|---|
| AGP | 8.8.1 | Android build |
| Kotlin | 2.1.10 | Language + Compose plugin |
| KSP | 2.1.10-1.0.29 | Symbol processing (Room, Hilt) |
| Compose BOM | 2025.02.00 | Compose UI suite |
| Hilt | 2.55 | Dependency injection |
| Room | 2.6.1 | Local database |
| WorkManager | 2.10.0 | Background sync |
| DataStore | 1.1.2 | Preferences |
| Jsoup | 1.18.3 | HTML parsing |
| Coil | 2.7.0 | Image loading |
| kotlinx-serialization | 1.7.3 | Profile serialization |

---

## Project File Tree (non-generated)

```
SPPUResultNotify/
├── .gitignore
├── LICENSE                                    (MIT)
├── README.md
├── index.html                                (Standalone web app version - 599 lines)
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── local.properties                          (sdk.dir)
├── gradle/
│   ├── libs.versions.toml
│   ├── gradle-daemon-jvm.properties
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties          (Gradle 9.4.1)
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/pinak/sppunotify/
│       │   │   ├── MainActivity.kt                     (448 lines)
│       │   │   ├── ResultWatchApp.kt                   (121 lines)
│       │   │   ├── di/AppModule.kt                     (48 lines)
│       │   │   ├── data/
│       │   │   │   ├── local/
│       │   │   │   │   ├── ResultEntity.kt
│       │   │   │   │   ├── ResultDao.kt
│       │   │   │   │   ├── RevalCourseEntity.kt
│       │   │   │   │   ├── RevalCourseDao.kt
│       │   │   │   │   ├── DownloadedResultEntity.kt
│       │   │   │   │   ├── DownloadedResultDao.kt
│       │   │   │   │   ├── ResultDatabase.kt
│       │   │   │   │   ├── PreferenceManager.kt        (128 lines)
│       │   │   │   │   └── UserProfile.kt
│       │   │   │   ├── remote/
│       │   │   │   │   ├── ResultDto.kt
│       │   │   │   │   ├── ResultScraper.kt            (249 lines)
│       │   │   │   │   ├── RevaluationDto.kt
│       │   │   │   │   ├── RevaluationScraper.kt       (165 lines)
│       │   │   │   │   ├── ServerStatus.kt
│       │   │   │   │   └── CircularRss.kt
│       │   │   │   └── repository/
│       │   │   │       ├── ResultRepository.kt
│       │   │   │       ├── RevalRepository.kt
│       │   │   │       ├── CircularRepository.kt
│       │   │   │       └── VaultRepository.kt
│       │   │   ├── receiver/BootReceiver.kt
│       │   │   ├── ui/
│       │   │   │   ├── MainScreen.kt                   (413 lines)
│       │   │   │   ├── screens/
│       │   │   │   │   ├── HomeScreen.kt               (603 lines)
│       │   │   │   │   ├── HomeViewModel.kt            (180 lines)
│       │   │   │   │   ├── ResultDetailsScreen.kt      (227 lines)
│       │   │   │   │   ├── DetailsViewModel.kt
│       │   │   │   │   ├── ResultViewScreen.kt         (338 lines)
│       │   │   │   │   ├── ResultViewViewModel.kt      (204 lines)
│       │   │   │   │   ├── RevaluationScreen.kt        (374 lines)
│       │   │   │   │   ├── CircularsScreen.kt          (130 lines)
│       │   │   │   │   ├── CircularsViewModel.kt
│       │   │   │   │   ├── CalculatorScreen.kt         (214 lines)
│       │   │   │   │   ├── LinksScreen.kt              (153 lines)
│       │   │   │   │   ├── SettingsScreen.kt           (411 lines)
│       │   │   │   │   ├── SettingsViewModel.kt
│       │   │   │   │   ├── VaultScreen.kt              (115 lines)
│       │   │   │   │   ├── VaultViewModel.kt
│       │   │   │   │   ├── AboutScreen.kt              (492 lines)
│       │   │   │   │   └── LazyScrollbar.kt            (125 lines)
│       │   │   │   └── theme/
│       │   │   │       ├── Color.kt
│       │   │   │       ├── Theme.kt                    (102 lines)
│       │   │   │       └── Type.kt
│       │   │   ├── util/
│       │   │   │   ├── DepartmentClassifier.kt
│       │   │   │   ├── FileSaver.kt
│       │   │   │   └── NotificationHelper.kt           (126 lines)
│       │   │   └── worker/
│       │   │       ├── ResultSyncWorker.kt
│       │   │       ├── RevalSyncWorker.kt
│       │   │       └── WorkManagerHelper.kt
│       │   └── res/
│       │       ├── drawable/ (3 vectors)
│       │       ├── mipmap-*/ (launcher icons)
│       │       ├── raw/ (RapidSSL certificate)
│       │       ├── values/ (strings, colors, themes)
│       │       └── xml/ (network_security, backup, file_paths)
│       ├── test/ (ExampleUnitTest)
│       └── androidTest/ (ExampleInstrumentedTest)
└── gradlew, gradlew.bat
```

---

## Tests

### `ExampleUnitTest.kt` (unit)
```kotlin
class ExampleUnitTest {
    @Test fun addition_isCorrect() { assertEquals(4, 2 + 2) }
}
```

### `ExampleInstrumentedTest.kt` (instrumented)
```kotlin
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("pinak.sppunotify", appContext.packageName)
    }
}
```

---

## Key Design Decisions & Notes

1. **Scraping, not API**: SPPU doesn't provide a public API. The app scrapes HTML from `onlineresults.unipune.ac.in` and `pun.unipune.ac.in/revalresult/` using Jsoup.

2. **Offline-first**: Results are cached in Room DB. The app shows cached results immediately and refreshes in the background.

3. **No data sent to third parties**: The app communicates ONLY with SPPU servers. No analytics, crash reporting, or telemetry.

4. **CAPTCHA handling**: The SPPU portal requires CAPTCHA for individual result viewing. The app fetches base64-encoded CAPTCHA images via XHR and passes them through for the user to solve.

5. **ASP.NET postback**: The revaluation portal uses ASP.NET WebForms with `__doPostBack()` and `__EVENTTARGET`/`__EVENTARGUMENT` for pagination. The scraper simulates this.

6. **Certificate pinning**: A custom RapidSSL root CA certificate is bundled for reliable HTTPS connections to SPPU's portal.

7. **High refresh rate**: Uses reflection (`Window.setFrameRatePowerSavingsBalanced`) to request the highest available display refresh rate (120Hz/144Hz) for smooth scrolling.

8. **Profiles**: Users can save multiple profiles (name, seat no, mother's name) to auto-fill the result viewing form.

9. **Smart Watchlist**: Users can define keywords to only receive notifications for specific results matching those keywords.

10. **index.html** at project root is a standalone web-based version of the app functionality.
