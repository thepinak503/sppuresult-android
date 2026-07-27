package pinak.sppunotify.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import pinak.sppunotify.util.safeStartActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import pinak.sppunotify.data.local.PreferenceManager
import pinak.sppunotify.util.ResultImageGenerator
import pinak.sppunotify.util.ResultPdfExport
import pinak.sppunotify.util.ResultImageShare
import pinak.sppunotify.util.FileSaver
import pinak.sppunotify.util.NotificationHelper
import kotlinx.coroutines.flow.first

class CreateDynamicDocument : ActivityResultContract<Pair<String, String>, Uri?>() {
    override fun createIntent(context: Context, input: Pair<String, String>): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(input.second)
            .putExtra(Intent.EXTRA_TITLE, input.first)
    }
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultViewScreen(
    viewModel: ResultViewViewModel,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val notificationHelper = remember { NotificationHelper(context) }
    val scrollState = rememberScrollState()

    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(value = false) }

    var pendingSaveData by remember { mutableStateOf<ResultViewEvent.SaveResult?>(null) }

    val createFileLauncher = rememberLauncherForActivityResult(
        contract = CreateDynamicDocument()
    ) { uri ->
        val data = pendingSaveData
        if (uri != null && data != null) {
            val success = FileSaver.saveToUri(context, data.bytes, uri)
            notificationHelper.showDownloadNotification(success, data.suggestedName)
            
            if (success) {
                viewModel.onResultSaved(uri, data.suggestedName)
            }

            scope.launch {
                if (success) {
                    snackbarHostState.showSnackbar("Result saved successfully")
                    val openIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, data.mimeType)
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.safeStartActivity(openIntent)
                } else {
                    snackbarHostState.showSnackbar("Failed to save result")
                }
            }
        }
        pendingSaveData = null
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ResultViewEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is ResultViewEvent.ShowErrorDialog -> {
                    dialogTitle = event.title
                    dialogMessage = event.message
                    showDialog = true
                }
                is ResultViewEvent.SaveResult -> {
                    pendingSaveData = event
                    createFileLauncher.launch(event.suggestedName to event.mimeType)
                }
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val result = state.result

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("View Result", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        if (result == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = "Published: ${result.publishedDate}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                HorizontalDivider()

                if (state.profiles.isNotEmpty()) {
                    Text(
                        "Quick Fill from Profile:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(state.profiles) { profile ->
                            val isSelected = state.activeProfileName == profile.name
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.switchProfile(profile) },
                                label = { Text(profile.name) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = state.seatNo,
                    onValueChange = { viewModel.updateSeatNo(it) },
                    label = { Text("Seat No") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) }
                )

                OutlinedTextField(
                    value = state.motherName,
                    onValueChange = { viewModel.updateMotherName(it) },
                    label = { Text("Mother Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                )

                // ── Captcha Image Section ──────────────────────────────────
                if (state.captchaBitmap != null || state.isLoadingCaptcha) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isLoadingCaptcha) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    strokeWidth = 3.dp
                                )
                            } else if (state.captchaBitmap != null) {
                                Image(
                                    bitmap = state.captchaBitmap!!.asImageBitmap(),
                                    contentDescription = "Security captcha image",
                                    modifier = Modifier
                                        .fillMaxHeight(0.7f)
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = state.captchaText,
                    onValueChange = { viewModel.updateCaptchaText(it) },
                    label = { Text("Captcha Text") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done
                    ),
                    leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                    supportingText = { Text("Enter the 5-character text from the image above") },
                )

                OutlinedButton(
                    onClick = { viewModel.loadCaptcha() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !state.isLoadingCaptcha,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                ) {
                    if (state.isLoadingCaptcha) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(
                        if (state.captchaBitmap != null) "Reload Captcha" else "Load Captcha",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val buttonScale by animateFloatAsState(
                    targetValue = if (state.isLoading) 0.97f else 1f,
                    label = "button_scale"
                )

                Button(
                    onClick = { viewModel.submitForm() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .scale(buttonScale),
                    enabled = !state.isLoading,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Check Result", style = MaterialTheme.typography.titleMedium)
                    }
                }

                if (state.resultBytes != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Result Fetched!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val uri = ResultPdfExport.exportToPdf(context, result)
                                if (uri != null) {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/pdf")
                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    context.startActivity(Intent.createChooser(intent, "View Result PDF"))
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("PDF")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                ResultImageShare.shareAsImage(
                                    context = context,
                                    title = result.title,
                                    department = result.department,
                                    publishedDate = result.publishedDate,
                                    patternName = result.patternName
                                )
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Image")
                        }
                    }
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
