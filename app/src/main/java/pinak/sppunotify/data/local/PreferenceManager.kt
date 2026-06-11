package pinak.sppunotify.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    object PreferencesKeys {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val RESULT_SYNC_INTERVAL = intPreferencesKey("result_sync_interval_min")
        val REVAL_SYNC_INTERVAL = intPreferencesKey("reval_sync_interval_min")
        val EXAM_DATE_SYNC_INTERVAL = intPreferencesKey("exam_date_sync_interval_min")
        val CIRCULAR_SYNC_INTERVAL = intPreferencesKey("circular_sync_interval_min")
        val WATCHLIST_KEYWORDS = stringSetPreferencesKey("watchlist_keywords")
        val SUBSCRIBED_DEPARTMENTS = stringSetPreferencesKey("subscribed_departments")
        val USER_PROFILES = stringSetPreferencesKey("user_profiles")
        val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val  APP_LANGUAGE = stringPreferencesKey("app_language")
        val LAST_ANNOUNCEMENT_ID = stringPreferencesKey("last_announcement_id")
        val WAS_SERVER_DOWN = booleanPreferencesKey("was_server_down")
        val DEVELOPER_MODE = booleanPreferencesKey("developer_mode")
        val SYNC_RESULTS_ENABLED = booleanPreferencesKey("sync_results_enabled")
        val SYNC_REVAL_ENABLED = booleanPreferencesKey("sync_reval_enabled")
        val SYNC_EXAM_DATES_ENABLED = booleanPreferencesKey("sync_exam_dates_enabled")
        val SYNC_CIRCULARS_ENABLED = booleanPreferencesKey("sync_circulars_enabled")
        val AUTO_UPDATE_ENABLED = booleanPreferencesKey("auto_update_enabled")
        val PRIORITY_KEYWORDS = stringSetPreferencesKey("priority_keywords")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val preferencesFlow: Flow<UserPreferences> = dataStore.data.map { preferences ->
        val encryptedStrings = preferences[PreferencesKeys.USER_PROFILES] ?: emptySet()
        val profiles = ProfileSerializer.deserializeEncryptedList(encryptedStrings)

        UserPreferences(
            notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
            resultSyncInterval = preferences[PreferencesKeys.RESULT_SYNC_INTERVAL] ?: 15,
            revalSyncInterval = preferences[PreferencesKeys.REVAL_SYNC_INTERVAL] ?: 60,
            examDateSyncInterval = preferences[PreferencesKeys.EXAM_DATE_SYNC_INTERVAL] ?: 180,
            circularSyncInterval = preferences[PreferencesKeys.CIRCULAR_SYNC_INTERVAL] ?: 240,
            watchlistKeywords = preferences[PreferencesKeys.WATCHLIST_KEYWORDS] ?: emptySet(),
            subscribedDepartments = preferences[PreferencesKeys.SUBSCRIBED_DEPARTMENTS] ?: emptySet(),
            profiles = profiles,
            activeProfileId = preferences[PreferencesKeys.ACTIVE_PROFILE_ID],
            themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM",
            appLanguage = preferences[PreferencesKeys.APP_LANGUAGE] ?: "en",
            lastAnnouncementId = preferences[PreferencesKeys.LAST_ANNOUNCEMENT_ID] ?: "",
            wasServerDown = preferences[PreferencesKeys.WAS_SERVER_DOWN] ?: false,
            developerMode = preferences[PreferencesKeys.DEVELOPER_MODE] ?: false,
            syncResultsEnabled = preferences[PreferencesKeys.SYNC_RESULTS_ENABLED] ?: true,
            syncRevalEnabled = preferences[PreferencesKeys.SYNC_REVAL_ENABLED] ?: true,
            syncExamDatesEnabled = preferences[PreferencesKeys.SYNC_EXAM_DATES_ENABLED] ?: true,
            syncCircularsEnabled = preferences[PreferencesKeys.SYNC_CIRCULARS_ENABLED] ?: true,
            autoUpdateEnabled = preferences[PreferencesKeys.AUTO_UPDATE_ENABLED] ?: true,
            priorityKeywords = preferences[PreferencesKeys.PRIORITY_KEYWORDS] ?: emptySet(),
            onboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        )
    }

    suspend fun updateOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun updateDeveloperMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEVELOPER_MODE] = enabled
        }
    }

    suspend fun updateWasServerDown(isDown: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.WAS_SERVER_DOWN] = isDown
        }
    }

    suspend fun updateLastAnnouncementId(id: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_ANNOUNCEMENT_ID] = id
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateResultSyncInterval(intervalMinutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.RESULT_SYNC_INTERVAL] = intervalMinutes
        }
    }

    suspend fun updateRevalSyncInterval(intervalMinutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.REVAL_SYNC_INTERVAL] = intervalMinutes
        }
    }

    suspend fun updateExamDateSyncInterval(intervalMinutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.EXAM_DATE_SYNC_INTERVAL] = intervalMinutes
        }
    }

    suspend fun updateCircularSyncInterval(intervalMinutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CIRCULAR_SYNC_INTERVAL] = intervalMinutes
        }
    }

    suspend fun addKeyword(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.WATCHLIST_KEYWORDS] ?: emptySet()
            preferences[PreferencesKeys.WATCHLIST_KEYWORDS] = current + trimmed
        }
    }

    suspend fun removeKeyword(keyword: String) {
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.WATCHLIST_KEYWORDS] ?: emptySet()
            preferences[PreferencesKeys.WATCHLIST_KEYWORDS] = current - keyword
        }
    }

    suspend fun toggleSubscribedDepartment(department: String) {
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.SUBSCRIBED_DEPARTMENTS] ?: emptySet()
            preferences[PreferencesKeys.SUBSCRIBED_DEPARTMENTS] = if (department in current) {
                current - department
            } else {
                current + department
            }
        }
    }

    suspend fun saveProfile(name: String, seatNo: String, motherName: String) {
        dataStore.edit { preferences ->
            val encryptedStrings = preferences[PreferencesKeys.USER_PROFILES] ?: emptySet()
            val profiles = ProfileSerializer.deserializeEncryptedList(encryptedStrings).toMutableList()

            val newProfile = UserProfile(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                seatNo = seatNo.trim(),
                motherName = motherName.trim()
            )
            profiles.add(newProfile)

            preferences[PreferencesKeys.USER_PROFILES] = ProfileSerializer.serializeEncryptedList(profiles)
            if (preferences[PreferencesKeys.ACTIVE_PROFILE_ID] == null) {
                preferences[PreferencesKeys.ACTIVE_PROFILE_ID] = newProfile.id
            }
        }
    }

    suspend fun deleteProfile(profileId: String) {
        dataStore.edit { preferences ->
            val encryptedStrings = preferences[PreferencesKeys.USER_PROFILES] ?: emptySet()
            val profiles = ProfileSerializer.deserializeEncryptedList(encryptedStrings).filter { it.id != profileId }
            preferences[PreferencesKeys.USER_PROFILES] = ProfileSerializer.serializeEncryptedList(profiles)

            if (preferences[PreferencesKeys.ACTIVE_PROFILE_ID] == profileId) {
                val nextActiveId = profiles.firstOrNull()?.id
                if (nextActiveId != null) {
                    preferences[PreferencesKeys.ACTIVE_PROFILE_ID] = nextActiveId
                } else {
                    preferences.remove(PreferencesKeys.ACTIVE_PROFILE_ID)
                }
            }
        }
    }

    suspend fun setActiveProfile(profileId: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVE_PROFILE_ID] = profileId
        }
    }

    suspend fun updateThemeMode(themeMode: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode
        }
    }

    suspend fun updateAppLanguage(languageCode: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = languageCode
        }
    }

    suspend fun updateSyncResultsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SYNC_RESULTS_ENABLED] = enabled
        }
    }

    suspend fun updateSyncRevalEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SYNC_REVAL_ENABLED] = enabled
        }
    }

    suspend fun updateSyncExamDatesEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SYNC_EXAM_DATES_ENABLED] = enabled
        }
    }

    suspend fun updateSyncCircularsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SYNC_CIRCULARS_ENABLED] = enabled
        }
    }

    suspend fun updateAutoUpdateEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_UPDATE_ENABLED] = enabled
        }
    }

    suspend fun addPriorityKeyword(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.PRIORITY_KEYWORDS] ?: emptySet()
            preferences[PreferencesKeys.PRIORITY_KEYWORDS] = current + trimmed
        }
    }

    suspend fun removePriorityKeyword(keyword: String) {
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.PRIORITY_KEYWORDS] ?: emptySet()
            preferences[PreferencesKeys.PRIORITY_KEYWORDS] = current - keyword
        }
    }

    suspend fun restoreFromBackup(backup: pinak.sppunotify.util.AppBackup) {
        dataStore.edit { preferences ->
            val encryptedProfiles = ProfileSerializer.serializeEncryptedList(backup.profiles)
            preferences[PreferencesKeys.USER_PROFILES] = encryptedProfiles
            preferences[PreferencesKeys.WATCHLIST_KEYWORDS] = backup.watchlistKeywords
            preferences[PreferencesKeys.SUBSCRIBED_DEPARTMENTS] = backup.subscribedDepartments
            preferences[PreferencesKeys.PRIORITY_KEYWORDS] = backup.priorityKeywords
            
            if (preferences[PreferencesKeys.ACTIVE_PROFILE_ID] == null && backup.profiles.isNotEmpty()) {
                preferences[PreferencesKeys.ACTIVE_PROFILE_ID] = backup.profiles.first().id
            }
        }
    }
}

data class UserPreferences(
    val notificationsEnabled: Boolean,
    val resultSyncInterval: Int,
    val revalSyncInterval: Int,
    val examDateSyncInterval: Int,
    val circularSyncInterval: Int,
    val watchlistKeywords: Set<String>,
    val subscribedDepartments: Set<String>,
    val profiles: List<UserProfile>,
    val activeProfileId: String?,
    val themeMode: String = "SYSTEM",
    val appLanguage: String = "en",
    val lastAnnouncementId: String = "",
    val wasServerDown: Boolean = false,
    val developerMode: Boolean = false,
    val syncResultsEnabled: Boolean = true,
    val syncRevalEnabled: Boolean = true,
    val syncExamDatesEnabled: Boolean = true,
    val syncCircularsEnabled: Boolean = true,
    val autoUpdateEnabled: Boolean = true,
    val priorityKeywords: Set<String> = emptySet(),
    val onboardingCompleted: Boolean = false
)
