package pinak.sppunotify.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import pinak.sppunotify.data.remote.StatusLevel
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import pinak.sppunotify.data.local.ExamDateEntity
import pinak.sppunotify.ui.components.AppEmptyState
import pinak.sppunotify.ui.components.AppSearchBar
import pinak.sppunotify.ui.components.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDatesScreen(
    viewModel: ExamDatesViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
    onMenuClick: () -> Unit,
) {
    val examDates by viewModel.examDates.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val serverStatus by viewModel.serverStatus.collectAsState()
    var searchActive by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                titleContent = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Exam Dates",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        serverStatus?.let { status ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Circle,
                                    contentDescription = null,
                                    modifier = Modifier.size(8.dp),
                                    tint = when (status.statusLevel) {
                                        StatusLevel.HEALTHY -> Color(0xFF4CAF50)
                                        StatusLevel.SLOW -> Color(0xFFFFC107)
                                        StatusLevel.BUSY -> Color(0xFFFF9800)
                                        StatusLevel.DOWN -> Color(0xFFF44336)
                                    }
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = when (status.statusLevel) {
                                        StatusLevel.HEALTHY -> "Online (${status.responseTimeMs}ms)"
                                        StatusLevel.SLOW -> "Slow (${status.responseTimeMs}ms)"
                                        StatusLevel.BUSY -> "Busy"
                                        StatusLevel.DOWN -> "Down"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navIcon = Icons.Default.Menu,
                onNavClick = onMenuClick,
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                AppSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                    expanded = searchActive,
                    onExpandedChange = { searchActive = it },
                    placeholder = "Search courses…",
                )

                if (!searchActive) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = true,
                        enter = slideInHorizontally { -it } + fadeIn()
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val statuses = listOf("All", "Active", "Over")
                            items(statuses) { status ->
                                FilterChip(
                                    selected = selectedStatus == status,
                                    onClick = { viewModel.onStatusSelected(status) },
                                    label = { Text(status) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }

                when {
                    isRefreshing && examDates.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeWidth = 4.dp
                            )
                        }
                    }
                    examDates.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            AppEmptyState(
                                icon = Icons.Default.Event,
                                message = if (searchQuery.isNotEmpty() || selectedStatus != "All")
                                    "No matches found"
                                else
                                    "No exam dates found",
                                subMessage = if (searchQuery.isNotEmpty() || selectedStatus != "All")
                                    "Try clearing filters"
                                else
                                    "Pull down to refresh"
                            )
                        }
                    }
                    else -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(
                                    start = 16.dp, end = 16.dp,
                                    top = 8.dp, bottom = 120.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(examDates, key = { it.courseName }) { item ->
                                    ExamDateCard(
                                        item = item,
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }

                            LazyScrollbar(
                                listState = listState,
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExamDateCard(
    item: ExamDateEntity,
    modifier: Modifier = Modifier,
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.courseName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(item.status)
                Text(
                    text = "Starts: ${item.startDate}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            DateInfoRow("End Date", item.endDateWithoutLateFee)
            DateInfoRow("With Late Fee", item.endDateWithLateFee)
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val isActive = status.contains("Active", ignoreCase = true)
    val containerColor =
        if (isActive) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.errorContainer
    val contentColor =
        if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onErrorContainer

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DateInfoRow(label: String, date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = date,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}
