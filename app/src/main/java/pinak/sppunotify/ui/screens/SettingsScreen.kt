package pinak.sppunotify.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import pinak.sppunotify.util.safeStartActivity
import pinak.sppunotify.data.local.UserPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import pinak.sppunotify.ui.theme.ThemeMode
import pinak.sppunotify.ui.components.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    scrollState: androidx.compose.foundation.ScrollState = rememberScrollState(),
    viewModel: SettingsViewModel = hiltViewModel(),
    onMenuClick: () -> Unit,
) {
    val context = LocalContext.current
    val userPreferences by viewModel.userPreferences.collectAsState()

    var themeMode by remember(userPreferences.themeMode) {
        mutableStateOf(
            try { ThemeMode.valueOf(userPreferences.themeMode) }
            catch (_: Exception) { ThemeMode.SYSTEM }
        )
    }

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val isBatteryOptIgnored = powerManager.isIgnoringBatteryOptimizations(context.packageName)

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Settings",
                navIcon = Icons.Default.Menu,
                onNavClick = onMenuClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Background Sync Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Background Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Periodically check for new results", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = userPreferences.notificationsEnabled,
                            onCheckedChange = { viewModel.updateNotificationsEnabled(it) }
                        )
                    }

                    if (userPreferences.notificationsEnabled) {
                        Spacer(Modifier.height(16.dp))
                        
                        // Result Sync Interval
                        SyncIntervalInput(
                            label = "Result Sync Interval (minutes)",
                            value = userPreferences.resultSyncInterval,
                            onValueChange = { viewModel.updateResultSyncInterval(it) }
                        )

                        Spacer(Modifier.height(12.dp))
                        
                        // Revaluation Sync Interval
                        SyncIntervalInput(
                            label = "Revaluation Sync Interval (minutes)",
                            value = userPreferences.revalSyncInterval,
                            onValueChange = { viewModel.updateRevalSyncInterval(it) }
                        )

                        Spacer(Modifier.height(12.dp))

                        // Exam Dates Sync Interval
                        SyncIntervalInput(
                            label = "Exam Dates Sync Interval (minutes)",
                            value = userPreferences.examDateSyncInterval,
                            onValueChange = { viewModel.updateExamDateSyncInterval(it) }
                        )
                    }
                }
            }

            // Saved Profiles Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text("Saved Profiles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        
                        var showAddDialog by remember { mutableStateOf(false) }
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Add Profile")
                        }

                        if (showAddDialog) {
                            var name by remember { mutableStateOf("") }
                            var seatNo by remember { mutableStateOf("") }
                            var motherName by remember { mutableStateOf("") }

                            AlertDialog(
                                onDismissRequest = { showAddDialog = false },
                                title = { Text("Add New Profile") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name (e.g. Me, Rahul)") }, singleLine = true)
                                        OutlinedTextField(value = seatNo, onValueChange = { seatNo = it }, label = { Text("Seat No") }, singleLine = true)
                                        OutlinedTextField(value = motherName, onValueChange = { motherName = it }, label = { Text("Mother's Name") }, singleLine = true)
                                    }
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            viewModel.addProfile(name, seatNo, motherName)
                                            showAddDialog = false
                                        },
                                        enabled = name.isNotBlank() && seatNo.isNotBlank() && motherName.isNotBlank()
                                    ) { Text("Add") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
                                }
                            )
                        }
                    }
                    
                    if (userPreferences.profiles.isEmpty()) {
                        Text("No profiles saved. Add one to auto-fill result forms.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        userPreferences.profiles.forEach { profile ->
                            val isActive = userPreferences.activeProfileId == profile.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setActiveProfile(profile.id) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = isActive, onClick = { viewModel.setActiveProfile(profile.id) })
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(profile.name, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
                                    Text("${profile.seatNo} • ${profile.motherName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { viewModel.deleteProfile(profile.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            // Smart Watchlist Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(10.dp))
                        Text("Smart Watchlist", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Only get notifications for results matching these keywords. Leave empty to get all notifications.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))

                    var newKeyword by remember { mutableStateOf("") }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newKeyword,
                            onValueChange = { newKeyword = it },
                            placeholder = { Text("e.g. Mechanical, TE 2019", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { viewModel.addKeyword(newKeyword); newKeyword = "" }, enabled = newKeyword.isNotBlank()) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }

                    if (userPreferences.watchlistKeywords.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            userPreferences.watchlistKeywords.forEach { keyword ->
                                InputChip(
                                    selected = true,
                                    onClick = { viewModel.removeKeyword(keyword) },
                                    label = { Text(keyword) },
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Theme Selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("App Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                themeMode = mode
                                viewModel.updateThemeMode(mode.name)
                                (context as? Activity)?.recreate()
                            }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = themeMode == mode, onClick = {
                                themeMode = mode
                                viewModel.updateThemeMode(mode.name)
                                (context as? Activity)?.recreate()
                            })
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = when (mode) {
                                    ThemeMode.SYSTEM -> Icons.Default.Settings
                                    ThemeMode.LIGHT -> Icons.Default.LightMode
                                    ThemeMode.DARK -> Icons.Default.DarkMode
                                    ThemeMode.BLACK -> Icons.Default.PhoneAndroid // Using phone icon for OLED black
                                },
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(text = when (mode) {
                                ThemeMode.SYSTEM -> "System default"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                                ThemeMode.BLACK -> "Pitch Black (OLED)"
                            }, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            // Battery Optimization
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBatteryOptIgnored)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BatteryFull, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("Battery Optimization", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isBatteryOptIgnored) "Battery optimization is disabled. Background sync will work reliably."
                        else "Battery optimization may prevent background sync. Disable it for reliable operation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, android.net.Uri.parse("package:${context.packageName}"))
                                context.safeStartActivity(intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isBatteryOptIgnored) "Already Disabled" else "Disable Optimization")
                    }
                }
            }

            // Notifications Permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasNotifPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                if (!hasNotifPerm) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Text("Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply { putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName) }
                                context.safeStartActivity(intent)
                            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                Text("Open Notification Settings")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SyncIntervalInput(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }
    
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = textValue,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                    textValue = newValue
                    val intVal = newValue.toIntOrNull()
                    if (intVal != null && intVal >= 1) { // Support intervals down to 1 minute
                        onValueChange(intVal)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            suffix = { Text("min") },
            supportingText = {
                val current = textValue.toIntOrNull() ?: 0
                if (current < 1) {
                    Text("Minimum 1 minute required", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}
