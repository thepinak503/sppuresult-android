package pinak.sppunotify.data.remote

data class ResultDto(
    val id: String,
    val title: String,
    val url: String,
    val published: String,
    val patternName: String = "",
    val patternId: String = "",
)
