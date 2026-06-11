package pinak.sppunotify.data.remote

import androidx.compose.runtime.Immutable

@Immutable
data class RevalCourse(
    val course: String,
    val subject: String,
    val eventTarget: String,
)

data class RevalResult(
    val html: String,
)
