package pinak.sppunotify.ui.screens

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import pinak.sppunotify.R
import pinak.sppunotify.data.local.ResultEntity
import pinak.sppunotify.data.remote.ServerStatus
import pinak.sppunotify.ui.theme.DeptColors
import pinak.sppunotify.ui.components.AppSearchBar
import pinak.sppunotify.ui.components.ServerStatusBar
import pinak.sppunotify.ui.components.AppTopBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onResultClick: (ResultEntity) -> Unit,
    onMenuClick: () -> Unit,
    listState: LazyListState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val context = LocalContext.current
    val results by viewModel.results.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val serverStatus by viewModel.serverStatus.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedDept by viewModel.selectedDepartment.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val lastUpdated by viewModel.lastUpdated.collectAsState()
    val departments = viewModel.departments

    val snackbarHostState = remember { SnackbarHostState() }
    var fabExpanded by remember { mutableStateOf(false) }

    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(value = false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var searchActive by remember { mutableStateOf(false) }
    val recentSearches = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is HomeViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is HomeViewModel.UiEvent.ShowErrorDialog -> {
                    dialogTitle = event.title
                    dialogMessage = event.message
                    showDialog = true
                }
            }
        }
    }

    val fabRotation by animateFloatAsState(
        targetValue = if (fabExpanded) 45f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "fabRotation"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AppTopBar(
                titleContent = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.nav_results),
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        ServerStatusBar(serverStatus = serverStatus)
                    }
                },
                navIcon = Icons.Default.Menu,
                onNavClick = onMenuClick,
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort",
                                tint = if (sortOrder != SortOrder.NEWEST_FIRST) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                    val rotation by animateFloatAsState(
                        targetValue = if (isRefreshing) 360f else 0f,
                        animationSpec = if (isRefreshing) {
                            infiniteRepeatable(tween(1000, easing = LinearEasing))
                        } else { tween(0) },
                        label = "refresh_rotation"
                    )
                    IconButton(onClick = {
                        viewModel.refresh()
                        viewModel.checkServerStatus()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.graphicsLayer { rotationZ = rotation }
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (!searchActive) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedVisibility(
                        visible = fabExpanded,
                        enter = fadeIn(spring()) + slideInVertically { it / 2 },
                        exit = fadeOut(spring()) + slideOutVertically { it / 2 }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MiniFabItem(
                                icon = Icons.Default.CloudSync,
                                label = stringResource(R.string.check_status),
                                onClick = {
                                    viewModel.checkServerStatus()
                                    fabExpanded = false
                                }
                            )
                            MiniFabItem(
                                icon = Icons.Default.Refresh,
                                label = stringResource(R.string.refresh),
                                onClick = {
                                    viewModel.refresh()
                                    fabExpanded = false
                                }
                            )
                            MiniFabItem(
                                icon = Icons.AutoMirrored.Filled.Sort,
                                label = "Sort",
                                onClick = {
                                    fabExpanded = false
                                    showSortMenu = true
                                }
                            )
                        }
                    }

                    FloatingActionButton(
                        onClick = { fabExpanded = !fabExpanded },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 12.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = if (fabExpanded) "Close quick actions" else "More actions",
                            modifier = Modifier.graphicsLayer { rotationZ = fabRotation }
                        )
                    }
                }
            }
        }
    ) { padding ->
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
                        text = stringResource(R.string.sort_by),
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
                    SortOrder.entries.forEach { order ->
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AppSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                expanded = searchActive,
                onExpandedChange = {
                    searchActive = it
                    if (!it && searchQuery.isNotBlank() && !recentSearches.contains(searchQuery)) {
                        recentSearches.add(0, searchQuery)
                    }
                },
                placeholder = stringResource(R.string.search_placeholder),
                recentSearches = recentSearches,
                onDismissRecentSearch = { recentSearches.remove(it) },
                onSearch = { query ->
                    if (query.isNotBlank() && !recentSearches.contains(query)) {
                        recentSearches.add(0, query)
                    }
                },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically { it / 2 },
                        label = "Recs"
                    ) {
                        Column {
                            Text(
                                stringResource(R.string.rec_depts),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("FE", "SE", "TE", "BE", "MBA", "MCA", "B.Sc", "B.Com").forEach { dept ->
                                    SuggestionChip(
                                        onClick = {
                                            viewModel.onDepartmentSelected(dept)
                                            searchActive = false
                                        },
                                        label = { Text(dept) },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (results.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            stringResource(R.string.instant_results),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(results.take(15)) { index, res ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically(
                                        initialOffsetY = { it },
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    ) + fadeIn(
                                        animationSpec = tween(
                                            durationMillis = 300,
                                            delayMillis = index * 50
                                        )
                                    ),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                res.title,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        supportingContent = { Text(res.publishedDate) },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                searchActive = false
                                                onResultClick(res)
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!searchActive) {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInHorizontally { -it } + fadeIn()
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
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

                if (results.isNotEmpty()) {
                    AnimatedVisibility(
                        visible = true,
                        enter = expandVertically(animationSpec = tween(400)) + fadeIn(tween(300))
                    ) {
                        QuickStatsRow(
                            totalCount = totalCount,
                            serverStatus = serverStatus,
                            sortOrder = sortOrder,
                            lastUpdated = lastUpdated
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (results.isEmpty()) {
                            if (isRefreshing) {
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
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    EmptyState(
                                        message = when {
                                            searchQuery.isNotEmpty() -> stringResource(R.string.no_matches, searchQuery)
                                            selectedDept != "All" -> stringResource(R.string.no_dept_results, selectedDept)
                                            else -> stringResource(R.string.no_results_found)
                                        }
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 16.dp,
                                    bottom = 160.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(
                                    items = results,
                                    key = { _, result -> result.id }
                                ) { _, result ->
                                    ResultCard(
                                        result = result,
                                        searchQuery = searchQuery,
                                        modifier = Modifier.animateItem(),
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        onToggleBookmark = { viewModel.toggleBookmark(result.id) },
                                        onShareResult = {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, "Check out this SPPU Result: ${result.title}\nLink: ${result.url}")
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, null))
                                        },
                                        onCopyTitle = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Result Title", result.title))
                                        },
                                        onMarkAsViewed = { viewModel.markAsViewed(result.id) },
                                        onClick = {
                                            viewModel.markAsViewed(result.id)
                                            onResultClick(result)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (results.isNotEmpty()) {
                        LazyScrollbar(
                            listState = listState,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(dialogTitle, fontWeight = FontWeight.Bold) },
                text = { Text(dialogMessage) },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(24.dp),
            )
        }
    }
}

@Composable
private fun MiniFabItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f))
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 6.dp)
        )
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(36.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(0.dp)
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun QuickStatsRow(
    totalCount: Int,
    serverStatus: ServerStatus?,
    sortOrder: SortOrder,
    lastUpdated: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "Results count",
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.results_count, totalCount),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    text = sortOrder.label,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (lastUpdated.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.updated_at, lastUpdated),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ResultCard(
    result: ResultEntity,
    searchQuery: String,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onToggleBookmark: () -> Unit = {},
    onShareResult: () -> Unit = {},
    onCopyTitle: () -> Unit = {},
    onMarkAsViewed: () -> Unit = {},
    onClick: () -> Unit,
) {
    val highlightedTitle = remember(result.title, searchQuery) {
        highlightText(result.title, searchQuery)
    }
    val deptColor = remember(result.department) { DeptColors.accentFor(result.department) }

    var isPressed by remember { mutableStateOf(value = false) }
    var showContextMenu by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scale"
    )

    with(sharedTransitionScope) {
        Box {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .sharedBounds(
                        rememberSharedContentState(key = "card-${result.id}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                    )
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            isPressed = true
                            onClick()
                            isPressed = false
                        },
                        onLongClick = { showContextMenu = true }
                    )
                    .semantics {
                        contentDescription = "Result: ${result.title}, ${result.department}, ${result.publishedDate}"
                        role = Role.Button
                    },
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
                ),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        deptColor,
                                        deptColor.copy(alpha = 0.3f)
                                    )
                                ),
                                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                            )
                            .align(Alignment.CenterVertically)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // isViewed blue dot
                                if (!result.isViewed) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = CircleShape
                                            )
                                            .semantics { contentDescription = "Unread" }
                                    )
                                }
                                if (result.department.isNotEmpty() && result.department != "Other UG") {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = deptColor.copy(alpha = 0.15f),
                                    ) {
                                        Text(
                                            text = result.department,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = deptColor,
                                            maxLines = 1
                                        )
                                    }
                                } else {
                                    Spacer(Modifier.size(1.dp))
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Bookmark star toggle
                                IconButton(
                                    onClick = onToggleBookmark,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (result.isBookmarked) Icons.Filled.Star else Icons.Filled.StarBorder,
                                        contentDescription = if (result.isBookmarked) "Remove bookmark" else "Bookmark",
                                        modifier = Modifier.size(18.dp),
                                        tint = if (result.isBookmarked) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline
                                    )
                                }
                                Text(
                                    text = result.publishedDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.sharedElement(
                                        rememberSharedContentState(key = "date-${result.id}"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = highlightedTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 22.sp,
                            modifier = Modifier.sharedElement(
                                rememberSharedContentState(key = "title-${result.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        )
                        if (result.patternName.isNotBlank() && result.patternName.length <= 50 && result.patternName.contains(' ')) {
                            Spacer(Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Bookmark,
                                    contentDescription = "Pattern: ${result.patternName}",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = result.patternName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Long-press context menu overlay
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
                offset = DpOffset(x = 48.dp, y = 0.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.copy_title), fontWeight = FontWeight.Medium) },
                    onClick = { showContextMenu = false; onCopyTitle() },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = "Copy Title", modifier = Modifier.size(20.dp)) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.share), fontWeight = FontWeight.Medium) },
                    onClick = { showContextMenu = false; onShareResult() },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp)) }
                )
                DropdownMenuItem(
                    text = { Text(if (result.isBookmarked) stringResource(R.string.remove_bookmark) else stringResource(R.string.bookmark), fontWeight = FontWeight.Medium) },
                    onClick = { showContextMenu = false; onToggleBookmark() },
                    leadingIcon = {
                        Icon(
                            if (result.isBookmarked) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = if (result.isBookmarked) "Remove Bookmark" else "Bookmark",
                            modifier = Modifier.size(20.dp),
                            tint = if (result.isBookmarked) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.mark_as_read), fontWeight = FontWeight.Medium) },
                    onClick = { showContextMenu = false; onMarkAsViewed() },
                    leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = "Mark as Read", modifier = Modifier.size(20.dp)) }
                )
            }
        }
    }
}

private fun highlightText(text: String, query: String): androidx.compose.ui.text.AnnotatedString {
    if (query.isBlank()) return buildAnnotatedString { append(text) }

    val lower = text.lowercase()
    val tokens = query.lowercase().trim().split(Regex("\\s+"))
    val hlColor = Color(0xFFFFD54F).copy(alpha = 0.4f)
    val matched = BooleanArray(text.length)

    for (token in tokens) {
        if (token.isEmpty()) continue
        var idx = lower.indexOf(token)
        while (idx >= 0) {
            for (i in idx until (idx + token.length)) matched[i] = true
            idx = lower.indexOf(token, idx + 1)
        }
    }

    return buildAnnotatedString {
        for (i in text.indices) {
            if (matched[i]) {
                withStyle(SpanStyle(background = hlColor, fontWeight = FontWeight.ExtraBold)) {
                    append(text[i])
                }
            } else {
                append(text[i])
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val rotate by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotate"
    )

    Column(
        modifier = Modifier.padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(96.dp)
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = "No results",
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer {
                        scaleX = pulse; scaleY = pulse
                        rotationZ = rotate
                    },
                tint = MaterialTheme.colorScheme.surfaceVariant
            )
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer { scaleX = pulse; scaleY = pulse },
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            )
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Pull down to refresh · Clear filters to see all results",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}
