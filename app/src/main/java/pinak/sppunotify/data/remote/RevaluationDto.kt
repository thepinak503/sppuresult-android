package pinak.sppunotify.data.remote

data class RevalCourse(
    val course: String,
    val subject: String,
    val eventTarget: String,
)

data class RevalResult(
    val html: String,
)
