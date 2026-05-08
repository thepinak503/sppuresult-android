package pinak.sppunotify.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import pinak.sppunotify.util.FileSaver
import pinak.sppunotify.util.NotificationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultViewScreen(
    viewModel: ResultViewViewModel,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val notificationHelper = remember { NotificationHelper(context) }

    var seatNo by remember { mutableStateOf("") }
    var motherName by remember { mutableStateOf("") }
    var captchaText by remember { mutableStateOf("") }

    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(value = false) }

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
                    val uri = FileSaver.saveToDownloads(
                        context = context,
                        bytes = event.bytes,
                        fileName = event.suggestedName,
                        mimeType = event.mimeType,
                    )
                    
                    val success = uri != null
                    notificationHelper.showDownloadNotification(success, event.suggestedName)
                    
                    if (success) {
                        snackbarHostState.showSnackbar("Result saved to Downloads")
                        try {
                            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, event.mimeType)
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(openIntent)
                        } catch (_: Exception) {
                            // ignore
                        }
                    } else {
                        snackbarHostState.showSnackbar("Failed to save result")
                    }
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
                title = { Text("View Result", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (result == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

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

            OutlinedTextField(
                value = seatNo,
                onValueChange = { seatNo = it },
                label = { Text("Seat No") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )

            OutlinedTextField(
                value = motherName,
                onValueChange = { motherName = it },
                label = { Text("Mother Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.captchaBitmap != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            bitmap = state.captchaBitmap!!.asImageBitmap(),
                            contentDescription = "Captcha",
                            modifier = Modifier
                                .heightIn(max = 80.dp)
                                .fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(onClick = { viewModel.loadCaptcha() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Refresh Captcha")
                        }
                    }
                }
            } else if (state.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            OutlinedTextField(
                value = captchaText,
                onValueChange = { captchaText = it.take(5) },
                label = { Text("Captcha Text") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                supportingText = { Text("Enter the 5-character text from the image above") },
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.submitForm(
                        seatNo = seatNo,
                        motherName = motherName,
                        captchaText = captchaText,
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !state.isLoading,
                shape = RoundedCornerShape(12.dp),
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
