package pinak.sppunotify.data.remote

import androidx.compose.runtime.Immutable

@Immutable
data class ResultDto(
    val id: String,
    val title: String,
    val url: String,
    val published: String,
    val patternName: String = "",
    val patternId: String = "",
    val department: String = "",
)
