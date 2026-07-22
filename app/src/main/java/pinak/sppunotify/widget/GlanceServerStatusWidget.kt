package pinak.sppunotify.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// ── Logging ────────────────────────────────────────────────────────────────────
private object WidgetLog {
    private const val TAG = "GlanceWidget"

    fun e(msg: String, e: Throwable? = null) {
        if (e != null) Log.e(TAG, msg, e) else Log.e(TAG, msg)
    }

    fun w(msg: String, e: Throwable? = null) {
        if (e != null) Log.w(TAG, msg, e) else Log.w(TAG, msg)
    }

    fun i(msg: String) = Log.i(TAG, msg)
    fun d(msg: String) = Log.d(TAG, msg)
}

// ── Glance state keys ──────────────────────────────────────────────────────────
object GlanceWidgetKeys {
    val statusLevel = stringPreferencesKey("status_level")
    val responseTime = longPreferencesKey("response_time")
    val lastUpdated = stringPreferencesKey("last_updated")
    val totalResults = longPreferencesKey("total_results")
    val resultItems = stringSetPreferencesKey("result_items")
}

private const val ITEM_DELIMITER = "\u001F"
private const val MAX_RESULTS = 50

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

// ── Size interpolation helpers ─────────────────────────────────────────────────
private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)

private data class WidgetSizing(
    val compact: Boolean,
    val narrow: Boolean,
    val width: Dp,
    val height: Dp,
) {
    val availableWidth: Float get() = width.value
    val availableHeight: Float get() = height.value

    fun lerpDp(small: Dp, large: Dp): Dp {
        val t = ((availableWidth - 200f) / 400f).coerceIn(0f, 1f)
        return lerp(small.value, large.value, t).dp
    }

    fun lerpFont(small: Float, large: Float): Float {
        val t = ((availableWidth - 200f) / 400f).coerceIn(0f, 1f)
        return lerp(small, large, t)
    }

    fun lerpSp(small: Float, large: Float): androidx.compose.ui.unit.TextUnit = lerpFont(small, large).sp
}

// ── Size buckets (5 levels covering the full resize range) ────────────────────
private val SIZES = setOf(
    DpSize(200.dp, 80.dp),   // COMPACT  (minResize)
    DpSize(250.dp, 130.dp),  // NORMAL
    DpSize(300.dp, 180.dp),  // EXPANDED
    DpSize(400.dp, 260.dp),  // LARGE
    DpSize(600.dp, 420.dp),  // XLARGE   (maxResize)
)

// ── Main widget ────────────────────────────────────────────────────────────────
class GlanceServerStatusWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Responsive(SIZES)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        WidgetLog.d("provideGlance start")
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
            WidgetLog.i("Widget state persisted: status=${fetched.statusLevel} items=${fetched.items.size}")
        } catch (e: Exception) {
            WidgetLog.e("Failed to persist widget state", e)
        }

        provideContent {
            val size = LocalSize.current
            WidgetLog.d("Rendering widget size=${size.width.value}x${size.height.value}")
            WidgetContent(
                sizing = sizingFrom(size),
                status = fetched.statusLevel,
                time = fetched.responseTime,
                items = fetched.items,
                total = fetched.totalResults,
                updated = fetched.lastUpdated,
            )
        }
    }

    // ── Build sizing context from actual size ────────────────────────────────
    private fun sizingFrom(size: DpSize): WidgetSizing {
        val w = size.width.value
        val h = size.height.value
        return WidgetSizing(
            compact = h < 100f,
            narrow = w < 230f,
            width = size.width,
            height = size.height,
        )
    }

    // ── ROOT COMPOSABLE ──────────────────────────────────────────────────────
    @Composable
    private fun WidgetContent(
        sizing: WidgetSizing,
        status: String,
        time: Long,
        items: List<WidgetResultItem>,
        total: Long,
        updated: String,
    ) {
        val pad = sizing.lerpDp(8.dp, 14.dp)
        val statusColor = statusColor(status)
        val statusLabel = statusLabel(status)
        val showActions = !sizing.narrow && !sizing.compact
        val showDeptBadge = !sizing.compact && !sizing.narrow && sizing.availableWidth >= 300f
        val showFooter = !sizing.compact
        val showBranding = sizing.availableWidth >= 350f && !sizing.compact

        // ── Show all items up to a cap — widget clips overflow ──────────────
        val h = sizing.availableHeight
        val maxItems = if (sizing.compact) 0 else items.size.coerceAtMost(30)
        val visibleCount = maxItems
        WidgetLog.d("render h=$h compact=${sizing.compact} narrow=${sizing.narrow} items=${items.size} visible=$visibleCount")
        val titleFont = sizing.lerpSp(13f, 20f)
        val bodyFont = sizing.lerpSp(10f, 14f)
        val captionFont = sizing.lerpSp(8f, 11f)
        val metaFont = sizing.lerpSp(9f, 12f)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(pad)
                .background(ImageProvider(R.drawable.widget_background))
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier.fillMaxWidth(),
            ) {
                val badgeSize = sizing.lerpDp(24.dp, 44.dp)
                val badgeFont = if (sizing.compact) 12.sp else sizing.lerpSp(13f, 22f)

                Box(
                    modifier = GlanceModifier
                        .size(badgeSize)
                        .background(ColorProvider(WidgetColors.primary.copy(alpha = 0.15f)))
                        .cornerRadius(sizing.lerpDp(8.dp, 14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "S",
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.primary),
                            fontSize = badgeFont,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }

                Spacer(modifier = GlanceModifier.width(if (sizing.compact) 6.dp else sizing.lerpDp(8.dp, 12.dp)))

                Column(modifier = GlanceModifier.defaultWeight()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val dotOuter = if (sizing.compact) 8.dp else sizing.lerpDp(9.dp, 14.dp)
                        val dotInner = if (sizing.compact) 4.dp else sizing.lerpDp(5.dp, 8.dp)
                        Box(
                            modifier = GlanceModifier
                                .size(dotOuter)
                                .background(ColorProvider(statusColor.copy(alpha = 0.25f)))
                                .cornerRadius(dotOuter / 2),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .size(dotInner)
                                    .background(ColorProvider(statusColor))
                                    .cornerRadius(dotInner / 2),
                            ) {}
                        }
                        Spacer(modifier = GlanceModifier.width(if (sizing.compact) 4.dp else sizing.lerpDp(5.dp, 8.dp)))
                        Text(
                            text = if (sizing.compact) statusLabel.take(12) else statusLabel,
                            style = TextStyle(
                                color = ColorProvider(WidgetColors.onSurface),
                                fontSize = titleFont,
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                        )
                    }

                    if (!sizing.compact && time > 0) {
                        Spacer(modifier = GlanceModifier.height(1.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${time}ms",
                                style = TextStyle(
                                    color = ColorProvider(WidgetColors.onSurfaceDim),
                                    fontSize = metaFont,
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                            Text(
                                text = " \u2022 ",
                                style = TextStyle(
                                    color = ColorProvider(WidgetColors.onSurfaceMuted),
                                    fontSize = metaFont,
                                ),
                            )
                            Text(
                                text = if (total == 1L) "1 result" else "$total results",
                                style = TextStyle(
                                    color = ColorProvider(WidgetColors.onSurfaceDim),
                                    fontSize = metaFont,
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                        }
                    }
                }

                if (showActions) {
                    val btnSize = sizing.lerpDp(30.dp, 42.dp)
                    val btnFont = sizing.lerpSp(14f, 20f)
                    Row {
                        Box(
                            modifier = GlanceModifier
                                .size(btnSize)
                                .background(ColorProvider(WidgetColors.surfaceLight.copy(alpha = 0.6f)))
                                .cornerRadius(sizing.lerpDp(9.dp, 12.dp))
                                .clickable(actionRunCallback<RefreshWidgetCallback>()),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "\u21BB",
                                style = TextStyle(
                                    color = ColorProvider(WidgetColors.onSurface.copy(alpha = 0.8f)),
                                    fontSize = btnFont,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                        if (!sizing.narrow) {
                            Spacer(modifier = GlanceModifier.width(5.dp))
                            Box(
                                modifier = GlanceModifier
                                    .size(btnSize)
                                    .background(ColorProvider(WidgetColors.orange.copy(alpha = 0.2f)))
                                    .cornerRadius(sizing.lerpDp(9.dp, 12.dp))
                                    .clickable(actionRunCallback<HardRefreshWidgetCallback>()),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "\u27F3",
                                    style = TextStyle(
                                        color = ColorProvider(WidgetColors.orange),
                                        fontSize = btnFont,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            // ── Results (weight fills remaining space) ─────────────────────
            if (visibleCount > 0) {
                Column(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                    items.take(visibleCount).forEachIndexed { index, item ->
                        WidgetItemRow(
                            item = item,
                            accent = WidgetDeptColors.accentFor(item.department),
                            bodyFont = bodyFont,
                            captionFont = captionFont,
                            showDeptBadge = showDeptBadge,
                            sizing = sizing,
                        )
                        if (index < visibleCount - 1) {
                            Spacer(modifier = GlanceModifier.height(sizing.lerpDp(2.dp, 5.dp)))
                        }
                    }
                }
            } else if (items.isEmpty() && !sizing.compact) {
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "\uD83D\uDCCB",
                            style = TextStyle(fontSize = (bodyFont.value + 6f).sp),
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = "No results yet",
                            style = TextStyle(
                                color = ColorProvider(WidgetColors.onSurfaceMuted),
                                fontSize = bodyFont,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }
                }
            }

            // ── Footer ──────────────────────────────────────────────────────
            if (showFooter) {
                Spacer(modifier = GlanceModifier.height(4.dp))
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    Text(
                        text = "Sync: $updated",
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.onSurfaceMuted.copy(alpha = 0.7f)),
                            fontSize = captionFont,
                        ),
                        maxLines = 1,
                    )
                    if (showBranding) {
                        Box(
                            modifier = GlanceModifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Text(
                                text = "SPPU Watch",
                                style = TextStyle(
                                    color = ColorProvider(WidgetColors.onSurfaceMuted.copy(alpha = 0.35f)),
                                    fontSize = (captionFont.value * 0.9f).sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }

    // ── RESULT ITEM ROW ─────────────────────────────────────────────────────
    @Composable
    private fun WidgetItemRow(
        item: WidgetResultItem,
        accent: Color,
        bodyFont: androidx.compose.ui.unit.TextUnit,
        captionFont: androidx.compose.ui.unit.TextUnit,
        showDeptBadge: Boolean,
        sizing: WidgetSizing,
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .width(3.dp)
                    .height(sizing.lerpDp(22.dp, 32.dp))
                    .background(ColorProvider(accent))
                    .cornerRadius(1.5.dp),
            ) {}
            Spacer(modifier = GlanceModifier.width(sizing.lerpDp(6.dp, 10.dp)))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = item.title,
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.onSurface),
                        fontSize = bodyFont,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
                if (item.date.isNotEmpty() && !sizing.compact) {
                    Text(
                        text = item.date,
                        style = TextStyle(
                            color = ColorProvider(WidgetColors.onSurfaceMuted),
                            fontSize = captionFont,
                        ),
                        maxLines = 1,
                    )
                }
            }
            if (showDeptBadge && item.department.isNotEmpty()) {
                Spacer(modifier = GlanceModifier.width(5.dp))
                Box(
                    modifier = GlanceModifier
                        .height(sizing.lerpDp(16.dp, 20.dp))
                        .background(ColorProvider(accent.copy(alpha = 0.15f)))
                        .cornerRadius(4.dp)
                        .padding(horizontal = sizing.lerpDp(4.dp, 6.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item.department.take(5),
                        style = TextStyle(
                            color = ColorProvider(accent),
                            fontSize = captionFont,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                    )
                }
            }
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

    // ── Data fetching ────────────────────────────────────────────────────────
    private suspend fun fetchWidgetData(context: Context): WidgetData {
        WidgetLog.d("fetchWidgetData start")
        return withContext(Dispatchers.IO) {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
                val scraper = entryPoint.scraper()
                val repository = entryPoint.repository()
                val db = entryPoint.database()

                WidgetLog.d("Fetching server health")
                val status = try {
                    scraper.checkServerHealth()
                } catch (e: Exception) {
                    WidgetLog.w("Server health check failed", e)
                    null
                }

                WidgetLog.d("Loading results from DB")
                var results = try {
                    db.dao.getAllResults().first()
                } catch (e: Exception) {
                    WidgetLog.e("DB load failed", e)
                    emptyList()
                }

                if (results.isEmpty()) {
                    try {
                        WidgetLog.d("No cached results, fetching from network")
                        repository.fetchResults()
                        results = db.dao.getAllResults().first()
                        WidgetLog.i("Fetched ${results.size} results from network")
                    } catch (e: Exception) {
                        WidgetLog.w("Network fetch failed", e)
                    }
                } else {
                    WidgetLog.i("Loaded ${results.size} results from DB cache")
                }

                val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val items = results.take(MAX_RESULTS).map { result ->
                    WidgetResultItem(
                        id = result.id,
                        title = result.title,
                        date = result.publishedDate,
                        department = result.department,
                    )
                }.also { WidgetLog.d("Encoded ${it.size} items for widget state") }

                val encoded = items.map { it.encode() }.toSet()
                WidgetData(
                    statusLevel = status?.statusLevel?.name ?: "UNKNOWN",
                    responseTime = status?.responseTimeMs ?: 0L,
                    lastUpdated = now,
                    totalResults = results.size.toLong(),
                    encodedItems = encoded,
                    items = items,
                )
            } catch (e: Exception) {
                WidgetLog.e("fetchWidgetData failed", e)
                WidgetData(
                    statusLevel = "DOWN",
                    responseTime = 0L,
                    lastUpdated = "Error",
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
}

// ── REFRESH CALLBACK ──────────────────────────────────────────────────────────
class RefreshWidgetCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetLog.i("Manual refresh triggered")
        try {
            val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
            val scraper = entryPoint.scraper()
            val repository = entryPoint.repository()
            val db = entryPoint.database()

            WidgetLog.d("Refresh: checking server health")
            val status = scraper.checkServerHealth()

            var results = db.dao.getAllResults().first()
            if (results.isEmpty()) {
                WidgetLog.d("Refresh: no cached results, fetching")
                repository.fetchResults()
                results = db.dao.getAllResults().first()
            }
            WidgetLog.i("Refresh: status=${status.statusLevel.name} results=${results.size}")

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
            WidgetLog.i("Refresh complete")
        } catch (e: Exception) {
            WidgetLog.e("Widget manual refresh failed", e)
        }
    }
}

// ── HARD REFRESH CALLBACK ─────────────────────────────────────────────────────
class HardRefreshWidgetCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetLog.i("Hard refresh triggered")
        try {
            val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
            val repository = entryPoint.repository()
            val db = entryPoint.database()

            WidgetLog.d("HardRefresh: executing hard refresh")
            repository.hardRefresh()

            val results = db.dao.getAllResults().first()
            WidgetLog.i("HardRefresh: ${results.size} results after refresh")

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
                    this[GlanceWidgetKeys.statusLevel] = "HEALTHY"
                    this[GlanceWidgetKeys.responseTime] = 0L
                    this[GlanceWidgetKeys.lastUpdated] = now
                    this[GlanceWidgetKeys.totalResults] = results.size.toLong()
                    this[GlanceWidgetKeys.resultItems] = encoded
                }
            }
            GlanceServerStatusWidget().update(context, glanceId)
            WidgetLog.i("Hard refresh complete")
        } catch (e: Exception) {
            WidgetLog.e("Widget hard refresh failed", e)
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[GlanceWidgetKeys.statusLevel] = "DOWN"
                    this[GlanceWidgetKeys.lastUpdated] = "Failed: ${e.message?.take(20) ?: "Error"}"
                }
            }
            GlanceServerStatusWidget().update(context, glanceId)
        }
    }
}

// ── WIDGET RECEIVER ───────────────────────────────────────────────────────────
class ServerStatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlanceServerStatusWidget()
}
