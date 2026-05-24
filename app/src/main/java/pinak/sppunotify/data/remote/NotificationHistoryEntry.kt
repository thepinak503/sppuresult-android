package pinak.sppunotify.data.remote

data class NotificationHistoryEntry(
    val id: Long,
    val title: String,
    val message: String,
    val type: String,
    val timestamp: Long
)
