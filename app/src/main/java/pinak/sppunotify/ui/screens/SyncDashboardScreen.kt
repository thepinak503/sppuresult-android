package pinak.sppunotify.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncDashboardScreen(
    viewModel: SyncDashboardViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val isAnyRunning = state.resultRunning || state.revalRunning || state.examDatesRunning || state.circularsRunning

    val rotation by animateFloatAsState(
        targetValue = if (isAnyRunning) 360f else 0f,
        animationSpec = if (isAnyRunning) {
            infiniteRepeatable(tween(1000, easing = LinearEasing))
        } else { tween(0) },
        label = "refresh_rotation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync Dashboard", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.syncAll() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Sync All",
                            modifier = Modifier.graphicsLayer { rotationZ = rotation }
                        )
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isAnyRunning,
            onRefresh = { viewModel.syncAll() },
            modifier = Modifier.padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Results Sync Card
                SyncWorkerCard(
                    title = "Results Sync",
                    icon = Icons.Default.Description,
                    isEnabled = state.resultEnabled,
                    isRunning = state.resultRunning,
                    lastRun = state.resultLastRun,
                    onSyncNow = { viewModel.syncNow("results") },
                    onToggle = { enabled -> viewModel.toggleSync("results", enabled) },
                    accent = MaterialTheme.colorScheme.primary
                )

                // Reval Sync Card
                SyncWorkerCard(
                    title = "Revaluation Sync",
                    icon = Icons.Default.Refresh,
                    isEnabled = state.revalEnabled,
                    isRunning = state.revalRunning,
                    lastRun = state.revalLastRun,
                    onSyncNow = { viewModel.syncNow("reval") },
                    onToggle = { enabled -> viewModel.toggleSync("reval", enabled) },
                    accent = MaterialTheme.colorScheme.tertiary
                )

                // Exam Dates Sync Card
                SyncWorkerCard(
                    title = "Exam Dates Sync",
                    icon = Icons.Default.Event,
                    isEnabled = state.examDatesEnabled,
                    isRunning = state.examDatesRunning,
                    lastRun = state.examDatesLastRun,
                    onSyncNow = { viewModel.syncNow("examDates") },
                    onToggle = { enabled -> viewModel.toggleSync("examDates", enabled) },
                    accent = MaterialTheme.colorScheme.secondary
                )

                // Circulars Sync Card
                SyncWorkerCard(
                    title = "Circulars Sync",
                    icon = Icons.Default.Info,
                    isEnabled = state.circularsEnabled,
                    isRunning = state.circularsRunning,
                    lastRun = state.circularsLastRun,
                    onSyncNow = { viewModel.syncNow("circulars") },
                    onToggle = { enabled -> viewModel.toggleSync("circulars", enabled) },
                    accent = MaterialTheme.colorScheme.error
                )

                // Sync History Section
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Sync History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        if (state.recentSyncs.isEmpty()) {
                            Text(
                                "No sync history yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            state.recentSyncs.forEach { sync ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(sync.type, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                        Text(sync.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (sync.success) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Text(
                                            text = if (sync.success) "OK" else "Failed",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (sync.success) MaterialTheme.colorScheme.onPrimaryContainer
                                                    else MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncWorkerCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isEnabled: Boolean,
    isRunning: Boolean,
    lastRun: String,
    onSyncNow: () -> Unit,
    onToggle: (Boolean) -> Unit,
    accent: androidx.compose.ui.graphics.Color,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = accent)
                    Column {
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            if (lastRun.isNotEmpty()) "Last: $lastRun" else "Not run yet",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Switch(checked = isEnabled, onCheckedChange = onToggle)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRunning) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.5.dp,
                            color = accent
                        )
                        Text(
                            "Syncing...",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }
                } else {
                    Text("")
                }
                Button(
                    onClick = onSyncNow,
                    enabled = isEnabled && !isRunning,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sync Now", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
