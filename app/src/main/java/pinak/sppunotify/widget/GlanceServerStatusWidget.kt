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
import androidx.glance.action.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.*
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

// ── Glance state keys ──────────────────────────────────────────────────────────
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
    val department: String = "",
) {
    fun encode(): String = buildString {
        append(id); append(ITEM_DELIMITER)
        append(title); append(ITEM_DELIMITER)
        append(date); append(ITEM_DELIMITER)
        append(department)
    }

    companion object {
        fun decode(raw: String): WidgetResultItem? {
            val parts = raw.split(ITEM_DELIMITER)
            if (parts.size < 3) return null
            return WidgetResultItem(
                id = parts[0],
                title = parts.getOrElse(1) { "" },
                date = parts.getOrElse(2) { "" },
                department = parts.getOrElse(3) { "" },
            )
        }
    }
}

// ── Department accent colours ──────────────────────────────────────────────────
private object WidgetDeptColors {
    val FE      = Color(0xFF4A55A2)
    val SE      = Color(0xFF0D7377)
    val TE      = Color(0xFF2E7D32)
    val BE      = Color(0xFF1565C0)
    val MBA     = Color(0xFF6A1B9A)
    val MCA     = Color(0xFF00838F)
    val MSc     = Color(0xFF37474F)
    val BCom    = Color(0xFFF57F17)
    val BSc     = Color(0xFF558B2F)
    val BA      = Color(0xFF4E342E)
    val BPharm  = Color(0xFFAD1457)
    val Law     = Color(0xFF37474F)
    val Diploma = Color(0xFF5D4037)
    val Default = Color(0xFF546E7A)

    fun accentFor(department: String): Color = when {
        department.startsWith("FE")                                                         -> FE
        department.startsWith("SE")                                                         -> SE
        department.startsWith("TE")                                                         -> TE
        department.startsWith("BE")                                                         -> BE
        department.startsWith("MBA")                                                        -> MBA
        department.startsWith("MCA")                                                        -> MCA
        department.startsWith("M.Sc") || department.startsWith("M.A") || department.startsWith("M.Com") -> MSc
        department.startsWith("B.Com")                                                      -> BCom
        department.startsWith("B.Sc")                                                       -> BSc
        department.startsWith("B.A")                                                        -> BA
        department.startsWith("B.Pharm")                                                    -> BPharm
        department.startsWith("Law")                                                        -> Law
        department.startsWith("Diploma")                                                    -> Diploma
        else                                                                                -> Default
    }
}

// ── Theme colours ──────────────────────────────────────────────────────────────
private object WidgetColors {
    val primary        = Color(0xFF5DADE2)
    val onSurface      = Color(0xFFE1E3E5)
    val onSurfaceDim   = Color(0xFFB0B3B8)
    val onSurfaceMuted = Color(0xFF8B9198)
    val outline        = Color(0xFF41484D)
    val surfaceLight   = Color(0xFF222A2D)
    val green          = Color(0xFF4CAF50)
    val yellow         = Color(0xFFFFC107)
    val orange         = Color(0xFFFF9800)
    val red            = Color(0xFFF44336)
}

// ── Main widget ────────────────────────────────────────────────────────────────
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
                    } catch (_: Exception) { }
                }

                val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val items = results.take(MAX_RESULTS).map { result ->
                    WidgetResultItem(
                        id = result.id,
                        title = result.title,
                        date = result.publishedDate,
                        department = result.department,
                    )
                }
                val encoded = items.map { it.encode() }.toSet()
                WidgetData(
                    statusLevel = status.statusLevel.name,
                    responseTime = status.responseTimeMs,
                    lastUpdated = now,
                    totalResults = results.size.toLong(),
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

    // ── ROOT COMPOSABLE ──────────────────────────────────────────────────────
    @Composable
    private fun WidgetContent(
        status: String,
        time: Long,
        items: List<WidgetResultItem>,
        total: Long,
        updated: String,
    ) {
        val statusColor = statusColor(status)
        val statusLabel = statusLabel(status)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp)
                .background(ImageProvider(R.drawable.widget_background))
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            // ── Header ───────────────────────────────────────────────────────
            HeaderSection(statusColor, statusLabel, time, total)

            Spacer(modifier = GlanceModifier.height(8.dp))

            // ── Results ──────────────────────────────────────────────────────
            if (items.isEmpty()) {
                EmptySection()
            } else {
                val visibleItems = items.take(4)
                Column {
                    visibleItems.forEachIndexed { index, item ->
                        val accent = WidgetDeptColors.accentFor(item.department)
                        ResultItem(item, accent)
                        if (index < visibleItems.lastIndex) {
                            Spacer(modifier = GlanceModifier.height(4.dp))
                        }
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // ── Footer ───────────────────────────────────────────────────────
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = "Sync: $updated",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.onSurfaceMuted.copy(alpha = 0.7f)),
                        fontSize = 9.sp,
                    ),
                    maxLines = 1,
                )
                Box(
                    modifier = GlanceModifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text(
                        text = "SPPU Watch",
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.onSurfaceMuted.copy(alpha = 0.35f)),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }

    // ── HEADER ───────────────────────────────────────────────────────────────
    @Composable
    private fun HeaderSection(
        statusColor: Color,
        statusLabel: String,
        time: Long,
        total: Long,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth(),
        ) {
            // ── App brand badge ──────────────────────────────────────────────
            Box(
                modifier = GlanceModifier
                    .size(44.dp)
                    .background(ColorProvider(WidgetColors.primary.copy(alpha = 0.15f)))
                    .cornerRadius(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "S",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.primary),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            Spacer(modifier = GlanceModifier.width(10.dp))

            // ── Status column ────────────────────────────────────────────────
            Column(modifier = GlanceModifier.defaultWeight()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = GlanceModifier
                            .size(12.dp)
                            .background(ColorProvider(statusColor.copy(alpha = 0.25f)))
                            .cornerRadius(6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .size(6.dp)
                                .background(ColorProvider(statusColor))
                                .cornerRadius(3.dp),
                        ) {}
                    }
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = statusLabel,
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.onSurface),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (time > 0) {
                        Text(
                            text = "${time}ms",
                            style = TextStyle(
                                color = ColorProvider(WidgetColors.onSurfaceDim),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                        Text(
                            text = " \u2022 ",
                            style = TextStyle(
                                color = ColorProvider(WidgetColors.onSurfaceMuted),
                                fontSize = 11.sp,
                            ),
                        )
                    }
                    Text(
                        text = if (total == 1L) "1 result" else "$total results",
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.onSurfaceDim),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }

            // ── Refresh button ───────────────────────────────────────────────
            Box(
                modifier = GlanceModifier
                    .size(40.dp)
                    .background(ColorProvider(WidgetColors.surfaceLight.copy(alpha = 0.6f)))
                    .cornerRadius(11.dp)
                    .clickable(actionRunCallback<RefreshWidgetCallback>()),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\u21BB",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.onSurface.copy(alpha = 0.8f)),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }

    // ── RESULT ITEM ──────────────────────────────────────────────────────────
    @Composable
    private fun ResultItem(item: WidgetResultItem, accent: Color) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Box(
                modifier = GlanceModifier
                    .width(3.dp)
                    .height(32.dp)
                    .background(ColorProvider(accent))
                    .cornerRadius(1.5.dp),
            ) {}
            Spacer(modifier = GlanceModifier.width(10.dp))

            Column {
                Text(
                    text = item.title,
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.onSurface),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
                if (item.date.isNotEmpty()) {
                    Text(
                        text = item.date,
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.onSurfaceMuted),
                            fontSize = 10.sp,
                        ),
                        maxLines = 1,
                    )
                }
            }

            if (item.department.isNotEmpty()) {
                Spacer(modifier = GlanceModifier.width(6.dp))
                Box(
                    modifier = GlanceModifier
                        .height(18.dp)
                        .background(ColorProvider(accent.copy(alpha = 0.15f)))
                        .cornerRadius(4.dp)
                        .padding(start = 6.dp, end = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item.department.take(6),
                        style = TextStyle(
                            color = ColorProvider(accent),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }

    // ── EMPTY ────────────────────────────────────────────────────────────────
    @Composable
    private fun EmptySection() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "\uD83D\uDCCB",
                style = TextStyle(fontSize = 20.sp),
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "No results yet",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.onSurfaceMuted),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = "Tap to open app",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.outline),
                    fontSize = 10.sp,
                ),
            )
        }
    }

    // ── UTILITIES ────────────────────────────────────────────────────────────
    private fun statusColor(status: String): Color = when (status) {
        "HEALTHY" -> WidgetColors.green
        "SLOW"    -> WidgetColors.yellow
        "BUSY"    -> WidgetColors.orange
        else      -> WidgetColors.red
    }

    private fun statusLabel(status: String): String = when {
        status == "HEALTHY" -> "Portal Online"
        status == "DOWN"    -> "Portal Down"
        status == "SLOW"    -> "Portal Slow"
        else                -> "Portal Busy"
    }
}

// ── REFRESH CALLBACK ──────────────────────────────────────────────────────────
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
                    department = result.department,
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

// ── WIDGET RECEIVER ───────────────────────────────────────────────────────────
class ServerStatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlanceServerStatusWidget()
}
