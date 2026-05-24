package pinak.sppunotify.data.remote

data class CircularRssItem(
    val title: String,
    val link: String,
    val description: String,
    val pubDate: String,
    val feedSource: String = ""
)
