package pinak.sppunotify.ui.screens

import android.content.Intent
import android.net.Uri
import pinak.sppunotify.util.safeStartActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.graphics.Color
import pinak.sppunotify.data.remote.StatusLevel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pinak.sppunotify.data.remote.RevalCourse
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import pinak.sppunotify.ui.components.AppEmptyState
import pinak.sppunotify.ui.components.AppSearchBar
import pinak.sppunotify.ui.components.AppTopBar

enum class RevalSort(val label: String) {
    DEFAULT("Default"),
    NAME_ASC("A-Z"),
    NAME_DESC("Z-A"),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RevaluationScreen(
    listState: LazyListState = rememberLazyListState(),
    viewModel: RevaluationViewModel = hiltViewModel(),
    onMenuClick: () -> Unit,
) {
    val context = LocalContext.current
    val courses by viewModel.courses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMsg.collectAsState()
    val serverStatus by viewModel.serverStatus.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedDept by remember { mutableStateOf("All") }
    var sortOrder by remember { mutableStateOf(RevalSort.DEFAULT) }
    var searchActive by remember { mutableStateOf(false) }
    val revalUrl = "https://unipune.ac.in/university_files/Reval_Online_Results_online.htm"

    val filteredCourses = remember(courses, searchQuery, selectedDept, sortOrder) {
        var list = courses
        if (selectedDept != "All") {
            val d = selectedDept.lowercase()
            list = list.filter { it.course.lowercase().contains(d) }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            list = list.filter { it.course.lowercase().contains(q) || it.subject.lowercase().contains(q) }
        }
        when (sortOrder) {
            RevalSort.DEFAULT   -> list
            RevalSort.NAME_ASC  -> list.sortedBy { it.course.lowercase() }
            RevalSort.NAME_DESC -> list.sortedByDescending { it.course.lowercase() }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                titleContent = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Revaluation",
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
                    val rotation by animateFloatAsState(
                        targetValue = if (isLoading) 360f else 0f,
                        animationSpec = if (isLoading) {
                            infiniteRepeatable(tween(1000, easing = LinearEasing))
                        } else { tween(0) },
                        label = "refresh_rotation"
                    )
                    IconButton(onClick = { viewModel.loadCourses() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.graphicsLayer { rotationZ = rotation }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            AppSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                expanded = searchActive,
                onExpandedChange = { searchActive = it },
                placeholder = "Search revaluation…",
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        "Quick Tags",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("FE", "SE", "TE", "BE", "MBA", "MCA", "B.Sc", "B.Com").forEach { tag ->
                            SuggestionChip(
                                onClick = {
                                    searchQuery = tag
                                    searchActive = false
                                },
                                label = { Text(tag) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    if (filteredCourses.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Matches",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(filteredCourses.take(15)) { course ->
                                ListItem(
                                    headlineContent = { Text(course.course, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    supportingContent = { Text(course.subject, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.clickable {
                                        searchActive = false
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(revalUrl))
                                        context.safeStartActivity(intent)
                                    }
                                )
                            }
                        }
                    }
                }
            }

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
                        val depts = listOf("All", "FE", "SE", "TE", "BE", "MBA", "MCA", "B.Sc", "B.Com")
                        items(depts) { dept ->
                            FilterChip(
                                selected = selectedDept == dept,
                                onClick = { selectedDept = dept },
                                label = { Text(dept) },
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${filteredCourses.size} courses",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { showSortMenu = true }) {
                            Text(sortOrder.label, style = MaterialTheme.typography.labelSmall)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            RevalSort.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.label, fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = { sortOrder = order; showSortMenu = false }
                                )
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        isLoading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    strokeWidth = 4.dp
                                )
                            }
                        }
                        errorMsg.isNotEmpty() -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AppEmptyState(
                                        icon = Icons.AutoMirrored.Filled.List,
                                        message = "Failed to load",
                                        subMessage = errorMsg
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Button(onClick = { viewModel.loadCourses() }) { Text("Retry") }
                                }
                            }
                        }
                        filteredCourses.isEmpty() -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                AppEmptyState(
                                    icon = Icons.AutoMirrored.Filled.List,
                                    message = if (searchQuery.isNotEmpty()) "No matches for \"$searchQuery\"" else "No revaluation courses",
                                    subMessage = if (searchQuery.isNotEmpty()) "Try a different keyword" else "Pull down to refresh"
                                )
                            }
                        }
                        else -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 16.dp,
                                    bottom = 120.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(
                                    items = filteredCourses,
                                    key = { "${it.course}-${it.subject}-${it.eventTarget}" }
                                ) { course ->
                                    RevalCourseCard(
                                        course = course,
                                        modifier = Modifier.animateItem(),
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(revalUrl))
                                            context.safeStartActivity(intent)
                                        }
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
fun RevalCourseCard(
    course: RevalCourse,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable {
                isPressed = true
                onClick()
                isPressed = false
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.course,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (course.subject.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = course.subject,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            FilledTonalButton(
                onClick = onClick,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Open", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
