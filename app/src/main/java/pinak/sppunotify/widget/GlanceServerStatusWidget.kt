package pinak.sppunotify.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import pinak.sppunotify.MainActivity
import pinak.sppunotify.R
import pinak.sppunotify.di.WidgetEntryPoint
import java.text.SimpleDateFormat
import java.util.*

object GlanceWidgetKeys {
    val statusLevel = stringPreferencesKey("status_level")
    val responseTime = longPreferencesKey("response_time")
    val lastUpdated = stringPreferencesKey("last_updated")
    val totalResults = longPreferencesKey("total_results")
    val resultItems = stringSetPreferencesKey("result_items")
}

private const val ITEM_DELIMITER = "\u001F"
private const val MAX_RESULTS = 12

data class WidgetResultItem(
    val id: String,
    val title: String,
    val date: String,
) {
    fun encode(): String = buildString {
        append(id); append(ITEM_DELIMITER)
        append(title); append(ITEM_DELIMITER)
        append(date)
    }

    companion object {
        fun decode(raw: String): WidgetResultItem? {
            val parts = raw.split(ITEM_DELIMITER)
            if (parts.size < 3) return null
            return WidgetResultItem(
                id = parts[0],
                title = parts.getOrElse(1) { "" },
                date = parts.getOrElse(2) { "" },
            )
        }
    }
}

class GlanceServerStatusWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val fetched = fetchWidgetData(context)

        try {
            updateAppWidgetState(context, id) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[GlanceWidgetKeys.statusLevel] = fetched.statusLevel
                    this[GlanceWidgetKeys.responseTime] = fetched.responseTime
                    this[GlanceWidgetKeys.lastUpdated] = fetched.lastUpdated
                    this[GlanceWidgetKeys.totalResults] = fetched.totalResults
                    this[GlanceWidgetKeys.resultItems] = fetched.encodedItems
                }
            }
        } catch (e: Exception) {
            Log.e("GlanceWidget", "Failed to persist widget state", e)
        }

        provideContent {
            WidgetContent(
                status = fetched.statusLevel,
                time = fetched.responseTime,
                items = fetched.items,
                total = fetched.totalResults,
                updated = fetched.lastUpdated,
            )
        }
    }

    private suspend fun fetchWidgetData(context: Context): WidgetData {
        return withContext(Dispatchers.IO) {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
                val scraper = entryPoint.scraper()
                val repository = entryPoint.repository()
                val db = entryPoint.database()

                val status = scraper.checkServerHealth()

                var results = db.dao.getAllResults().first()
                if (results.isEmpty()) {
                    try {
                        repository.fetchResults()
                        results = db.dao.getAllResults().first()
                    } catch (e: Exception) {
                        Log.e("GlanceWidget", "Network fetch failed", e)
                    }
                }

                val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val items = results.take(MAX_RESULTS).map { result ->
                    WidgetResultItem(
                        id = result.id,
                        title = result.title,
                        date = result.publishedDate,
                    )
                }
                val encoded = items.map { it.encode() }.toSet()
                val total = results.size.toLong()

                WidgetData(
                    statusLevel = status.statusLevel.name,
                    responseTime = status.responseTimeMs,
                    lastUpdated = now,
                    totalResults = total,
                    encodedItems = encoded,
                    items = items,
                )
            } catch (e: Exception) {
                Log.e("GlanceWidget", "fetchWidgetData failed", e)
                WidgetData(
                    statusLevel = "DOWN",
                    responseTime = 0L,
                    lastUpdated = "Never",
                    totalResults = 0L,
                    encodedItems = emptySet(),
                    items = emptyList(),
                )
            }
        }
    }

    private data class WidgetData(
        val statusLevel: String,
        val responseTime: Long,
        val lastUpdated: String,
        val totalResults: Long,
        val encodedItems: Set<String>,
        val items: List<WidgetResultItem>,
    )

    @Composable
    private fun WidgetContent(
        status: String,
        time: Long,
        items: List<WidgetResultItem>,
        total: Long,
        updated: String,
    ) {
        val isDown = status == "DOWN"
        val statusColor = when (status) {
            "HEALTHY" -> Color(0xFF4CAF50)
            "SLOW" -> Color(0xFFFFC107)
            "BUSY" -> Color(0xFFFF9800)
            else -> Color(0xFFF44336)
        }
        val statusLabel = when {
            status == "HEALTHY" -> "Portal Online"
            isDown -> "Portal Down"
            status == "SLOW" -> "Portal Slow"
            else -> "Portal Busy"
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp)
                .background(ImageProvider(R.drawable.widget_background))
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                // MATERIAL 3 STYLE BIG PLUS BUTTON
                Box(
                    modifier = GlanceModifier
                        .size(48.dp)
                        .background(ColorProvider(Color(0xFFEADDFF))) // M3 light primary container
                        .cornerRadius(16.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF21005D)), // M3 on primary container
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )
                }
                
                Spacer(modifier = GlanceModifier.width(12.dp))

                Column(modifier = GlanceModifier.defaultWeight()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = GlanceModifier
                                .size(10.dp)
                                .background(ColorProvider(statusColor))
                                .cornerRadius(5.dp),
                            content = {}
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            text = statusLabel,
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Text(
                        text = if (time > 0) "${time}ms • $total results" else "$total results",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFCBC4CF)),
                            fontSize = 12.sp
                        )
                    )
                }

                // BIG TOUCH AREA REFRESH
                Box(
                    modifier = GlanceModifier
                        .size(48.dp)
                        .clickable(actionRunCallback<RefreshWidgetCallback>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u21BB",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Results list
            Box(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                if (items.isEmpty()) {
                    Text(
                        text = "No results yet",
                        style = TextStyle(color = ColorProvider(Color(0xFF888888)), fontSize = 13.sp),
                        modifier = GlanceModifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column {
                        items.take(4).forEach { item ->
                            ResultItemRow(item = item)
                        }
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                Text(
                    text = "Sync: $updated",
                    style = TextStyle(color = ColorProvider(Color(0xFF938F99)), fontSize = 10.sp)
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "SPPU Result Watch",
                    style = TextStyle(color = ColorProvider(Color(0xFF938F99).copy(alpha = 0.5f)), fontSize = 9.sp)
                )
            }
        }
    }

    @Composable
    private fun ResultItemRow(item: WidgetResultItem) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity<MainActivity>())
                .padding(vertical = 3.dp)
        ) {
            Text(
                text = item.title,
                maxLines = 1,
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp, fontWeight = FontWeight.Medium),
                modifier = GlanceModifier.fillMaxWidth()
            )
            if (item.date.isNotEmpty()) {
                Text(
                    text = item.date,
                    style = TextStyle(color = ColorProvider(Color(0xFF938F99)), fontSize = 10.sp),
                    modifier = GlanceModifier.padding(top = 1.dp)
                )
            }
        }
    }
}

class RefreshWidgetCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        try {
            val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
            val scraper = entryPoint.scraper()
            val repository = entryPoint.repository()
            val db = entryPoint.database()

            val status = scraper.checkServerHealth()

            var results = db.dao.getAllResults().first()
            if (results.isEmpty()) {
                repository.fetchResults()
                results = db.dao.getAllResults().first()
            }

            val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val items = results.take(MAX_RESULTS).map { result ->
                WidgetResultItem(
                    id = result.id,
                    title = result.title,
                    date = result.publishedDate,
                )
            }
            val encoded = items.map { it.encode() }.toSet()

            updateAppWidgetState(context, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[GlanceWidgetKeys.statusLevel] = status.statusLevel.name
                    this[GlanceWidgetKeys.responseTime] = status.responseTimeMs
                    this[GlanceWidgetKeys.lastUpdated] = now
                    this[GlanceWidgetKeys.totalResults] = results.size.toLong()
                    this[GlanceWidgetKeys.resultItems] = encoded
                }
            }
            GlanceServerStatusWidget().update(context, glanceId)
        } catch (e: Exception) {
            Log.e("RefreshWidget", "Widget manual refresh failed", e)
        }
    }
}

class ServerStatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlanceServerStatusWidget()
}
