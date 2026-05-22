package pinak.sppunotify.ui.screens

import android.content.Intent
import android.net.Uri
import pinak.sppunotify.util.safeStartActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pinak.sppunotify.data.local.DownloadedResultEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel = hiltViewModel(),
    onViewPdf: (filePath: String, title: String) -> Unit = { _, _ -> }
) {
    val results by viewModel.downloadedResults.collectAsState()
    val context = LocalContext.current
    var deleteTarget by remember { mutableStateOf<DownloadedResultEntity?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Marksheet Vault", fontWeight = FontWeight.ExtraBold) }
            )
        }
    ) { padding ->
        if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))
                    Text("No marksheets saved yet.", color = MaterialTheme.colorScheme.outline)
                    Text("Downloaded results will appear here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(results) { res ->
                    VaultCard(res, 
                        onOpen = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(res.filePath), res.mimeType)
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }
                            context.safeStartActivity(intent, "No app available to open this file")
                        },
                        onViewInApp = {
                            onViewPdf(res.filePath, res.title)
                        },
                        onDelete = { deleteTarget = res }
                    )
                }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete marksheet?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete \"${target.title}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteResult(target)
                    deleteTarget = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun VaultCard(result: DownloadedResultEntity, onOpen: () -> Unit, onViewInApp: () -> Unit, onDelete: () -> Unit) {
    val dateStr = remember(result.downloadDate) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(result.downloadDate))
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        ListItem(
            headlineContent = {
                Text(result.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = {
                Column {
                    Text("Profile: ${result.profileName}", style = MaterialTheme.typography.labelMedium)
                    Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            },
            leadingContent = {
                Icon(
                    if (result.mimeType.contains("pdf")) Icons.Default.PictureAsPdf else Icons.Default.FileOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingContent = {
                Row {
                    if (result.mimeType.contains("pdf")) {
                        IconButton(onClick = onViewInApp) {
                            Icon(Icons.Default.Visibility, contentDescription = "View in App", modifier = Modifier.size(20.dp))
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}
