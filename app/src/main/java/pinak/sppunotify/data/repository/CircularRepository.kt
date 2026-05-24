package pinak.sppunotify.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import pinak.sppunotify.data.local.CircularDao
import pinak.sppunotify.data.local.CircularEntity
import pinak.sppunotify.data.remote.CircularRssItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CircularRepository @Inject constructor(
    private val dao: CircularDao
) {

    private val feeds = listOf(
        "http://collegecirculars.unipune.ac.in/sites/examdocs/_layouts/listfeed.aspx?List=%7B2BC7A6B5%2D0C60%2D4171%2DB341%2DBEADD8306CFC%7D", // Exam Circulars
        "http://collegecirculars.unipune.ac.in/_layouts/listfeed.aspx?List=%7B7DAE8C71%2D35F4%2D4842%2DB6F9%2DB8FF1D3AC73C%7D", // Important Circulars
        "http://collegecirculars.unipune.ac.in/sites/documents/_layouts/listfeed.aspx?List=%7BF7317358%2D9C40%2D4E71%2DA48A%2DB2DE5AB73E69%7D" // Academic Calendar
    )

    fun getCachedCirculars(): Flow<List<CircularRssItem>> = dao.getAllCirculars().map { entities ->
        entities.map { entity ->
            CircularRssItem(
                title = entity.title,
                link = entity.link,
                description = entity.description,
                pubDate = entity.pubDate,
                feedSource = entity.feedSource
            )
        }
    }

    fun searchCirculars(query: String): Flow<List<CircularRssItem>> = dao.searchCirculars(query).map { entities ->
        entities.map { entity ->
            CircularRssItem(
                title = entity.title,
                link = entity.link,
                description = entity.description,
                pubDate = entity.pubDate,
                feedSource = entity.feedSource
            )
        }
    }

    suspend fun fetchAllCirculars(): List<CircularRssItem> = withContext(Dispatchers.IO) {
        val items = coroutineScope {
            feeds.mapIndexed { index, url ->
                val sourceName = when (index) {
                    0 -> "Exam"
                    1 -> "Important"
                    2 -> "Academic"
                    else -> ""
                }
                async {
                    try {
                        val doc = Jsoup.connect(url)
                            .parser(Parser.xmlParser())
                            .timeout(10000)
                            .get()

                        doc.select("item").map { item ->
                            CircularRssItem(
                                title = item.select("title").text(),
                                link = item.select("link").text(),
                                description = item.select("description").text(),
                                pubDate = item.select("pubDate").text(),
                                feedSource = sourceName
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("CircularRepository", "Failed to fetch feed $url: ${e.message}")
                        emptyList()
                    }
                }
            }.flatMap { it.await() }
        }.distinctBy { it.link }
        val entities = items.map { item ->
            CircularEntity(
                link = item.link,
                title = item.title,
                description = item.description,
                pubDate = item.pubDate,
                feedSource = item.feedSource
            )
        }
        dao.insertCirculars(entities)
        items
    }
}
