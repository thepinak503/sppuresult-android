package pinak.sppunotify.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.ToggleableStateKey
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pinak.sppunotify.MainActivity
import pinak.sppunotify.R
import pinak.sppunotify.data.remote.StatusLevel
import pinak.sppunotify.di.WidgetEntryPoint
import java.text.SimpleDateFormat
import java.util.*

object GlanceWidgetKeys {
    val statusLevel = stringPreferencesKey("status_level")
    val responseTime = longPreferencesKey("response_time")
    val latestResult = stringPreferencesKey("latest_result")
    val lastUpdated = stringPreferencesKey("last_updated")
}

class GlanceServerStatusWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val status = prefs[GlanceWidgetKeys.statusLevel] ?: "HEALTHY"
            val time = prefs[GlanceWidgetKeys.responseTime] ?: 0L
            val result = prefs[GlanceWidgetKeys.latestResult] ?: "Checking..."
            val updated = prefs[GlanceWidgetKeys.lastUpdated] ?: "Never"

            WidgetContent(status, time, result, updated)
        }
    }

    @Composable
    private fun WidgetContent(status: String, time: Long, result: String, updated: String) {
        val statusColor = when (status) {
            "HEALTHY" -> Color(0xFF4CAF50)
            "SLOW" -> Color(0xFFFFC107)
            "BUSY" -> Color(0xFFFF9800)
            else -> Color(0xFFF44336)
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp)
                .background(ImageProvider(R.drawable.widget_background))
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                // Circle indicator using a simple box with background
                // Note: Glance doesn't support generic Canvas drawing easily
                // We use a small image or just text.
                Text(
                    text = "●",
                    style = TextStyle(
                        color = ColorProvider(statusColor),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = if (status == "HEALTHY") "Online (${time}ms)" else status.lowercase().replaceFirstChar { it.uppercase() },
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Text(
                text = result,
                maxLines = 2,
                style = TextStyle(
                    color = ColorProvider(Color(0xFFBBBBBB)),
                    fontSize = 12.sp
                ),
                modifier = GlanceModifier
                    .padding(top = 6.dp)
                    .clickable(actionStartActivity<MainActivity>())
            )

            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "↻",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF888888)),
                            fontSize = 14.sp
                        ),
                        modifier = GlanceModifier.clickable(actionRunCallback<RefreshWidgetCallback>())
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = "Updated: $updated",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF888888)),
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

class RefreshWidgetCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val scraper = entryPoint.scraper()
        val repository = entryPoint.repository()
        val db = entryPoint.database()

        try {
            val status = scraper.checkServerHealth()
            
            // Try getting from DB first, if empty, trigger a fetch
            var results = db.dao.getAllResults().first()
            if (results.isEmpty()) {
                repository.fetchResults()
                results = db.dao.getAllResults().first()
            }
            
            val latest = results.firstOrNull()?.title ?: "No results"
            val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            updateAppWidgetState(context, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[GlanceWidgetKeys.statusLevel] = status.statusLevel.name
                    this[GlanceWidgetKeys.responseTime] = status.responseTimeMs
                    this[GlanceWidgetKeys.latestResult] = latest
                    this[GlanceWidgetKeys.lastUpdated] = now
                }
            }
            GlanceServerStatusWidget().update(context, glanceId)
        } catch (e: Exception) {
            android.util.Log.e("RefreshWidget", "Widget update failed", e)
        }
    }
}

class ServerStatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlanceServerStatusWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
            GlanceServerStatusWidget().updateAll(context)
        }
    }
}
