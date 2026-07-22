package pinak.sppunotify.util

import pinak.sppunotify.data.local.PreferenceManager
import pinak.sppunotify.data.repository.RemoteConfigRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class AnnouncementManager @Inject constructor(
    private val preferenceManager: PreferenceManager
) {
    suspend fun shouldShowAnnouncement(announcement: RemoteConfigRepository.Announcement): Boolean {
        val prefs = preferenceManager.preferencesFlow.first()
        // Show if ID is different from the last DISMISSED/SEEN ID
        return announcement.id.isNotBlank() && announcement.id != prefs.lastAnnouncementId
    }

    suspend fun markAnnouncementAsSeen(id: String) {
        preferenceManager.updateLastAnnouncementId(id)
    }
}
