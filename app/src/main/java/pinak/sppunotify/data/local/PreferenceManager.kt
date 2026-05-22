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
        val WATCHLIST_KEYWORDS = stringSetPreferencesKey("watchlist_keywords")
        val USER_PROFILES = stringSetPreferencesKey("user_profiles")
        val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val preferencesFlow: Flow<UserPreferences> = dataStore.data.map { preferences ->
        val encryptedStrings = preferences[PreferencesKeys.USER_PROFILES] ?: emptySet()
        val profiles = ProfileSerializer.deserializeEncryptedList(encryptedStrings)

        UserPreferences(
            notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
            resultSyncInterval = preferences[PreferencesKeys.RESULT_SYNC_INTERVAL] ?: 15,
            revalSyncInterval = preferences[PreferencesKeys.REVAL_SYNC_INTERVAL] ?: 60,
            watchlistKeywords = preferences[PreferencesKeys.WATCHLIST_KEYWORDS] ?: emptySet(),
            profiles = profiles,
            activeProfileId = preferences[PreferencesKeys.ACTIVE_PROFILE_ID],
            themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM"
        )
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
}

data class UserPreferences(
    val notificationsEnabled: Boolean,
    val resultSyncInterval: Int,
    val revalSyncInterval: Int,
    val watchlistKeywords: Set<String>,
    val profiles: List<UserProfile>,
    val activeProfileId: String?,
    val themeMode: String = "SYSTEM"
)
