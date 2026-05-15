package pinak.sppunotify.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import pinak.sppunotify.data.remote.RevaluationScraper

enum class RevalSort(val label: String) {
    DEFAULT("Default"),
    NAME_ASC("A-Z"),
    NAME_DESC("Z-A"),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RevaluationScreen(
    listState: LazyListState = rememberLazyListState(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scraper = remember { RevaluationScraper() }

    var courses by remember { mutableStateOf<List<RevalCourse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(RevalSort.DEFAULT) }
    var searchActive by remember { mutableStateOf(false) }
    val revalUrl = "https://unipune.ac.in/university_files/Reval_Online_Results_online.htm"

    val filteredCourses = remember(courses, searchQuery, sortOrder) {
        var list = courses
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            list = list.filter { it.course.lowercase().contains(q) || it.subject.lowercase().contains(q) }
        }
        when (sortOrder) {
            RevalSort.DEFAULT -> list
            RevalSort.NAME_ASC -> list.sortedBy { it.course.lowercase() }
            RevalSort.NAME_DESC -> list.sortedByDescending { it.course.lowercase() }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val result = scraper.scrapeCourses()
            courses = result
            isLoading = false
        } catch (e: Exception) {
            errorMsg = e.message ?: "Failed to load"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Revaluation Results", fontWeight = FontWeight.ExtraBold)
                },
                actions = {
                    val rotation by animateFloatAsState(
                        targetValue = if (isLoading) 360f else 0f,
                        animationSpec = if (isLoading) {
                            infiniteRepeatable(tween(1000, easing = LinearEasing))
                        } else {
                            tween(0)
                        },
                        label = "refresh_rotation"
                    )
                    IconButton(onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            errorMsg = ""
                            try {
                                courses = scraper.scrapeCourses()
                            } catch (e: Exception) {
                                errorMsg = e.message ?: "Failed to load"
                            }
                            isLoading = false
                        }
                    }) {
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSearch = { searchActive = false },
                            expanded = searchActive,
                            onExpandedChange = { searchActive = it },
                            placeholder = { Text("Search revaluation...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                    },
                    expanded = searchActive,
                    onExpandedChange = { searchActive = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = if (searchActive) RoundedCornerShape(0.dp) else RoundedCornerShape(32.dp),
                    colors = SearchBarDefaults.colors(
                        containerColor = if (searchActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
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
                                            context.startActivity(intent)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!searchActive) {
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
                                    Text("Failed to load", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Text(errorMsg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(16.dp))
                                    Button(onClick = {
                                        coroutineScope.launch {
                                            isLoading = true; errorMsg = ""
                                            try { courses = scraper.scrapeCourses() } catch (e: Exception) { errorMsg = e.message ?: "Failed" }
                                            isLoading = false
                                        }
                                    }) { Text("Retry") }
                                }
                            }
                        }
                        filteredCourses.isEmpty() -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(if (searchQuery.isNotEmpty()) "No matches" else "No revaluation courses",
                                        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Try clearing filters", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
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
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(revalUrl))
                                            context.startActivity(intent)
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
    onClick: () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "scale"
    )

    Card(
        modifier = Modifier
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
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
