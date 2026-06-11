package pinak.sppunotify.ui.screens

import android.content.Intent
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import pinak.sppunotify.util.BiometricHelper
import pinak.sppunotify.util.safeStartActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import pinak.sppunotify.R
import pinak.sppunotify.data.local.DownloadedResultEntity
import pinak.sppunotify.ui.components.AppEmptyState
import pinak.sppunotify.ui.components.AppTopBar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel = hiltViewModel(),
    onMenuClick: () -> Unit,
    onViewPdf: (filePath: String, title: String) -> Unit = { _, _ -> }
) {
    val results by viewModel.downloadedResults.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var deleteTarget by remember { mutableStateOf<DownloadedResultEntity?>(null) }
    
    var isAuthorized by remember { mutableStateOf(false) }
    val biometricHelper = remember { BiometricHelper(context) }

    LaunchedEffect(Unit) {
        if ((biometricHelper.canAuthenticate() && activity != null)) {
            biometricHelper.authenticate(
                activity = activity,
                title = "Vault Authentication",
                subtitle = "Authenticate to access saved marksheets",
                onSuccess = { isAuthorized = true },
                onError = { }
            )
        } else {
            // If device has no biometric or PIN set up, allow access (or you could require a fallback)
            isAuthorized = true
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.nav_vault),
                navIcon = Icons.Default.Menu,
                onNavClick = onMenuClick
            )
        }
    ) { padding ->
        if (!isAuthorized) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text("Vault is Locked", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Please authenticate to view your downloaded results.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = {
                        if (activity != null) {
                            biometricHelper.authenticate(
                                activity = activity,
                                title = "Vault Authentication",
                                subtitle = "Authenticate to access saved marksheets",
                                onSuccess = { isAuthorized = true },
                                onError = { }
                            )
                        }
                    }) {
                        Text("Unlock Vault")
                    }
                }
            }
        } else if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                AppEmptyState(
                    icon = Icons.Default.PictureAsPdf,
                    message = "No marksheets saved yet",
                    subMessage = "Downloaded results will appear here"
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                items(
                    items = results,
                    key = { it.uid },
                    contentType = { "vault-result" },
                ) { res ->
                    VaultCard(
                        result = res,
                        modifier = Modifier.animateItem(),
                        onOpen = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(res.filePath.toUri(), res.mimeType)
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
fun VaultCard(
    result: DownloadedResultEntity,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onViewInApp: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateStr = remember(result.downloadDate) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(result.downloadDate))
    }

    Card(
        modifier = modifier.fillMaxWidth().clickable { onOpen() },
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
