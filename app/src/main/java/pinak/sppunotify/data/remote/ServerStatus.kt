package pinak.sppunotify.data.remote

data class ServerStatus(
    val isOnline: Boolean,
    val responseTimeMs: Long,
    val lastChecked: Long = System.currentTimeMillis()
) {
    val statusLevel: StatusLevel
        get() = when {
            !isOnline -> StatusLevel.DOWN
            responseTimeMs > 4000 -> StatusLevel.SLOW
            else -> StatusLevel.HEALTHY
        }
}

enum class StatusLevel {
    HEALTHY, SLOW, DOWN
}
