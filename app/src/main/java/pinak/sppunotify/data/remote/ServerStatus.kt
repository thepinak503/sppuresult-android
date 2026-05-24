package pinak.sppunotify.data.remote

data class ServerStatus(
    val isOnline: Boolean,
    val statusCode: Int = 200,
    val responseTimeMs: Long,
    val lastChecked: Long = System.currentTimeMillis()
) {
    val statusLevel: StatusLevel
        get() = when {
            !isOnline -> StatusLevel.DOWN
            statusCode in 500..599 -> StatusLevel.BUSY
            responseTimeMs > 8000 -> StatusLevel.SLOW
            else -> StatusLevel.HEALTHY
        }
}

enum class StatusLevel {
    HEALTHY, SLOW, BUSY, DOWN
}
