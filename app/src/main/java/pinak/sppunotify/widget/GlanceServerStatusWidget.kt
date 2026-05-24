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
        // 1. Fetch fresh data from DB directly
        val fetched = fetchWidgetData(context)

        // 2. Save to widget state for persistence (future renders)
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

        // 3. Render with FRESH data passed directly (not via currentState which may be stale)
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

    /** Fetch data from DB and return everything needed for rendering. */
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
                // Return defaults — will show "No results yet" / "Updated Never"
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

    /** Container for all widget data for a single render. */
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
            status == "HEALTHY" -> "Online"
            isDown -> "Down"
            status == "SLOW" -> "Slow"
            else -> "Busy"
        }

        val statusBarColor = when {
            isDown -> Color(0xFFF44336)
            status == "SLOW" -> Color(0xFFFFC107)
            status == "BUSY" -> Color(0xFFFF9800)
            else -> Color(0xFF4CAF50)
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
            // ── Header row: status dot + label + time + count + refresh ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                Text(
                    text = "\u25CF",
                    style = TextStyle(
                        color = ColorProvider(statusColor),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = statusLabel,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.width(6.dp))

                if (time > 0) {
                    Text(
                        text = "${time}ms",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFAAAAAA)),
                            fontSize = 10.sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                if (total > 0) {
                    Text(
                        text = "$total results",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF888888)),
                            fontSize = 9.sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(4.dp))

                // Refresh button
                Text(
                    text = "\u21BB",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFBBBBBB)),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier
                        .clickable(actionRunCallback<RefreshWidgetCallback>())
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // ── Status bar ──
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(ColorProvider(statusBarColor)),
                content = {}
            )

            Spacer(modifier = GlanceModifier.height(6.dp))

            // ── Results list ──
            if (items.isEmpty()) {
                Text(
                    text = "No results yet",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF888888)),
                        fontSize = 12.sp
                    ),
                    modifier = GlanceModifier
                        .padding(vertical = 8.dp)
                        .defaultWeight()
                )
            } else {
                items.forEach { item ->
                    ResultItemRow(item = item)
                }
            }

            Spacer(modifier = GlanceModifier.height(2.dp))

            // ── Footer: last updated time ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                Text(
                    text = "Updated $updated",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF666666)),
                        fontSize = 8.sp
                    )
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
                .padding(vertical = 4.dp)
        ) {
            // Title
            Text(
                text = item.title,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = GlanceModifier.fillMaxWidth()
            )

            // Date
            if (item.date.isNotEmpty()) {
                Text(
                    text = item.date,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF999999)),
                        fontSize = 9.sp
                    ),
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
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[GlanceWidgetKeys.statusLevel] = "DOWN"
                    this[GlanceWidgetKeys.responseTime] = 0L
                    this[GlanceWidgetKeys.lastUpdated] = "error"
                    this[GlanceWidgetKeys.resultItems] = emptySet()
                }
            }
            GlanceServerStatusWidget().update(context, glanceId)
        }
    }
}

class ServerStatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlanceServerStatusWidget()
}
