package pinak.sppunotify.data.remote

import androidx.compose.runtime.Immutable

@Immutable
data class NotificationHistoryEntry(
    val id: Long,
    val title: String,
    val message: String,
    val type: String,
    val targetUri: String? = null,
    val timestamp: Long
)
