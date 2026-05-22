package pinak.sppunotify.ui.screens

import android.content.Intent
import android.net.Uri
import pinak.sppunotify.util.safeStartActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pinak.sppunotify.data.remote.CircularRssItem
import pinak.sppunotify.ui.components.AppEmptyState
import pinak.sppunotify.ui.components.AppSearchBar
import pinak.sppunotify.ui.components.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircularsScreen(
    viewModel: CircularsViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
    onMenuClick: () -> Unit,
) {
    val context = LocalContext.current
    val circulars by viewModel.circulars.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val errorMsg by viewModel.errorMsg.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var searchActive by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Circulars",
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
                    placeholder = "Search circulars…",
                )

                when {
                    isRefreshing && circulars.isEmpty() -> {
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
                    circulars.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            AppEmptyState(
                                icon = Icons.Default.Description,
                                message = if (errorMsg.isNotEmpty()) errorMsg
                                          else if (searchQuery.isNotEmpty()) "No circulars match \"$searchQuery\""
                                          else "No circulars found",
                                subMessage = if (errorMsg.isNotEmpty()) "Pull down to retry"
                                             else "Pull down to refresh"
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp,
                                top = 8.dp, bottom = 120.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(circulars) { item ->
                                CircularCard(item) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.link))
                                    context.safeStartActivity(intent, "No browser available to open circular")
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
fun CircularCard(item: CircularRssItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Column {
                    Spacer(Modifier.height(4.dp))
                    Text(item.pubDate, style = MaterialTheme.typography.labelMedium)
                }
            },
            leadingContent = {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingContent = {
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open in browser",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}
