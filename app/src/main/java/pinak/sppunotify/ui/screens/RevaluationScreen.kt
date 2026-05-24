package pinak.sppunotify.ui.screens

import android.content.Intent
import androidx.core.net.toUri
import pinak.sppunotify.util.safeStartActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import pinak.sppunotify.R
import pinak.sppunotify.data.remote.RevalCourse
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import pinak.sppunotify.ui.components.AppEmptyState
import pinak.sppunotify.ui.components.AppSearchBar
import pinak.sppunotify.ui.components.AppTopBar
import pinak.sppunotify.ui.components.ServerStatusBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RevaluationScreen(
    listState: LazyListState = rememberLazyListState(),
    viewModel: RevaluationViewModel = hiltViewModel(),
    onMenuClick: () -> Unit,
) {
    val context = LocalContext.current
    val courses by viewModel.courses.collectAsState()
    val filteredCourses by viewModel.filteredCourses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMsg.collectAsState()
    val serverStatus by viewModel.serverStatus.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedDept by viewModel.selectedDept.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val departments = remember(courses) { viewModel.departments }

    var searchActive by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    val revalUrl = "https://unipune.ac.in/university_files/Reval_Online_Results_online.htm"
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is RevaluationViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AppTopBar(
                titleContent = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.nav_reval),
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        ServerStatusBar(serverStatus = serverStatus)
                    }
                },
                navIcon = Icons.Default.Menu,
                onNavClick = onMenuClick,
                actions = {
                    // Sort button
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort",
                                tint = if (sortOrder != RevalSort.DEFAULT) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                    // Refresh button
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
            // Search bar
            AppSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                expanded = searchActive,
                onExpandedChange = { searchActive = it },
                placeholder = stringResource(R.string.search_placeholder),
            ) {
                // Search dropdown content
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
                        departments.filter { it != "All" }.take(9).forEach { tag ->
                            SuggestionChip(
                                onClick = {
                                    viewModel.onDepartmentSelected(tag)
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
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    modifier = Modifier.clickable {
                                        searchActive = false
                                        val intent = Intent(Intent.ACTION_VIEW, revalUrl.toUri())
                                        context.safeStartActivity(intent)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (!searchActive) {
                // Department filter chips (dynamically derived)
                AnimatedVisibility(
                    visible = true,
                    enter = slideInHorizontally { -it } + fadeIn()
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(departments) { dept ->
                            FilterChip(
                                selected = selectedDept == dept,
                                onClick = { viewModel.onDepartmentSelected(dept) },
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

                // Count + sort row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.courses_count, totalCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                    TextButton(onClick = { showSortMenu = true }) {
                        Text(sortOrder.label, style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Sort bottom sheet
                if (showSortMenu) {
                    ModalBottomSheet(
                        onDismissRequest = { showSortMenu = false },
                        sheetState = rememberModalBottomSheetState(),
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        dragHandle = { BottomSheetDefaults.DragHandle() },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 40.dp)
                        ) {
                            Text(
                                text = "Sort Revaluation Courses",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 0.5.dp
                            )
                            Spacer(Modifier.height(8.dp))
                            RevalSort.entries.forEach { order ->
                                val selected = sortOrder == order
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            viewModel.setSortOrder(order)
                                            showSortMenu = false
                                        }
                                        .padding(horizontal = 20.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = {
                                            viewModel.setSortOrder(order)
                                            showSortMenu = false
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = order.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Main content area
                Box(modifier = Modifier.fillMaxSize()) {
                    PullToRefreshBox(
                        isRefreshing = isLoading,
                        onRefresh = { viewModel.loadCourses() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when {
                            isLoading && courses.isEmpty() -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(48.dp),
                                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        strokeWidth = 4.dp
                                    )
                                }
                            }
                            errorMsg.isNotEmpty() && filteredCourses.isEmpty() -> {
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
                                        message = if (searchQuery.isNotEmpty()) "No matches for \"$searchQuery\""
                                                  else if (selectedDept != "All") "No courses in $selectedDept"
                                                  else "No revaluation courses",
                                        subMessage = if (searchQuery.isNotEmpty() || selectedDept != "All")
                                                        "Try a different filter"
                                                     else "Pull down to refresh"
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
                                    itemsIndexed(
                                        items = filteredCourses,
                                        key = { _, course -> "${course.course}|${course.subject}|${course.eventTarget}" }
                                    ) { _, course ->
                                        RevalCourseCard(
                                            course = course,
                                            modifier = Modifier.animateItem(),
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, revalUrl.toUri())
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

    val deptCode = remember(course) { RevaluationViewModel.extractDeptCode(course.course) }

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
            // Left content
            Column(modifier = Modifier.weight(1f)) {
                // Dept badge
                if (deptCode != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = deptCode,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
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
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
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
