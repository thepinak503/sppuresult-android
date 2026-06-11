package pinak.sppunotify

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavHostController
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.lifecycleScope
import pinak.sppunotify.ui.screens.OnboardingScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pinak.sppunotify.data.local.PreferenceManager
import pinak.sppunotify.ui.MainScreen
import pinak.sppunotify.util.LocaleHelper
import pinak.sppunotify.ui.theme.SPPUResultWatchTheme
import pinak.sppunotify.ui.theme.ThemeMode
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    /** Reference set by MainScreen composable for handling deep links */
    private var navController: NavHostController? = null

    /** Called from MainScreen to register the nav controller for deep link handling */
    fun setNavController(controller: NavHostController) {
        navController = controller
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ ->
        // We can handle the denial state in the UI
    }

    private val refreshRateHandler = Handler(Looper.getMainLooper())
    private var lastAppliedModeId = -1
    private var lastAppliedRefreshRate = -1f
    private val refreshRateRunnable = object : Runnable {
        override fun run() {
            if (isFinishing || isDestroyed) return
            try {
                setHighRefreshRateIfNeeded()
            } catch (e: Exception) {
                Log.w(TAG, "Ignoring setHighRefreshRate exception: ${e.message}")
            }
            if (!isFinishing && !isDestroyed) {
                refreshRateHandler.postDelayed(this, REFRESH_RATE_INTERVAL_MS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Configuration state to handle initialization
        var isReady by mutableStateOf(false)
        splashScreen.setKeepOnScreenCondition { !isReady }
        
        lifecycleScope.launch {
            try {
                val prefs = preferenceManager.preferencesFlow.first()
                LocaleHelper.setLocale(this@MainActivity, prefs.appLanguage)
                
                val mode = try { ThemeMode.valueOf(prefs.themeMode) } catch (_: Exception) { ThemeMode.SYSTEM }
                when(mode) {
                    ThemeMode.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    ThemeMode.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    ThemeMode.SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    ThemeMode.BLACK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                
                isReady = true
            } catch (e: Exception) {
                Log.e(TAG, "Startup config failed", e)
                isReady = true // Continue anyway
            }
        }
        
        splashScreen.setOnExitAnimationListener { splashProvider ->
            val fadeOut = android.view.animation.AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
            splashProvider.view.startAnimation(fadeOut)
            Handler(Looper.getMainLooper()).postDelayed({
                splashProvider.remove()
            }, 300)
        }

        enableEdgeToEdge()

        try {
            disableFrameRatePowerSavingsIfSupported()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set isFrameRatePowerSavingsBalanced: ${e.message}")
        }

        setContent {
            val prefs by preferenceManager.preferencesFlow.collectAsState(initial = null)
            val themeMode = remember(prefs) {
                try {
                    ThemeMode.valueOf(prefs?.themeMode ?: "SYSTEM")
                } catch (_: Exception) {
                    ThemeMode.SYSTEM
                }
            }
            
            SPPUResultWatchTheme(themeMode = themeMode) {
                AppSetupFlow(prefs)
            }
        }
    }

    @Composable
    private fun AppSetupFlow(prefs: pinak.sppunotify.data.local.UserPreferences?) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        
        var isOnline by remember { mutableStateOf(isOnline()) }
        var disclaimerAccepted by remember {
            mutableStateOf(context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).getBoolean("disclaimer_accepted", false))
        }
        var showPermissionDialog by remember { mutableStateOf(false) }

        if (prefs == null) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
            return
        }

        if (!prefs.onboardingCompleted) {
            OnboardingScreen(onFinished = {
                scope.launch { preferenceManager.updateOnboardingCompleted(true) }
            })
            return
        }

        LaunchedEffect(disclaimerAccepted) {
            if (disclaimerAccepted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    showPermissionDialog = true
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (disclaimerAccepted && isOnline) {
                MainScreen()
            }

            if (!isOnline) {
                SetupPopup(
                    title = "No Internet",
                    message = "An internet connection is required to check SPPU results. Please connect to a network and try again.",
                    icon = Icons.Default.WifiOff,
                    confirmLabel = "Retry",
                    onConfirm = { isOnline = isOnline() },
                    cancelLabel = "Exit",
                    onCancel = { finish() }
                )
            }

            if (isOnline && !disclaimerAccepted) {
                SetupPopup(
                    title = "Disclaimer",
                    message = "This application is NOT affiliated, associated, authorized, endorsed by, or in any way officially connected with Savitribai Phule Pune University (SPPU), or any of its subsidiaries, departments, or affiliates.\n\nThis is an independent, community-developed open source application.",
                    icon = Icons.Default.Warning,
                    iconColor = MaterialTheme.colorScheme.error,
                    confirmLabel = "Agree",
                    onConfirm = {
                        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit { putBoolean("disclaimer_accepted", true) }
                        disclaimerAccepted = true
                    },
                    cancelLabel = "Exit",
                    onCancel = { finish() }
                )
            }

            if (isOnline && disclaimerAccepted && showPermissionDialog) {
                SetupPopup(
                    title = "Notifications Required",
                    message = "SPPU Result Notify needs notification permission to alert you when new results are published. This is highly recommended for background sync.",
                    icon = Icons.Default.Notifications,
                    confirmLabel = "Grant Permission",
                    onConfirm = {
                        showPermissionDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    cancelLabel = "Later",
                    onCancel = { showPermissionDialog = false }
                )
            }
        }
    }

    @Composable
    private fun SetupPopup(
        title: String,
        message: String,
        icon: ImageVector,
        iconColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
        confirmLabel: String,
        onConfirm: () -> Unit,
        cancelLabel: String,
        onCancel: () -> Unit
    ) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(32.dp))
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCancel,
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(cancelLabel, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onConfirm,
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = iconColor)
                            ) {
                                Text(confirmLabel, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    private fun disableFrameRatePowerSavingsIfSupported() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            var applied = false

            try {
                window::class.java.getMethod(
                    "setFrameRatePowerSavingsBalanced",
                    Boolean::class.javaPrimitiveType
                ).invoke(window, false)
                Log.i(TAG, "Window.setFrameRatePowerSavingsBalanced(false) applied via reflection")
                applied = true
            } catch (e: Exception) {
                Log.w(TAG, "Window reflection failed: ${e.message}")
            }

            if (!applied) {
                try {
                    val lp = window.attributes
                    val setMethod = lp::class.java.getMethod(
                        "setFrameRatePowerSavingsBalanced",
                        Boolean::class.javaPrimitiveType
                    )
                    setMethod.invoke(lp, false)
                    window.attributes = lp
                    Log.i(TAG, "LayoutParams.setFrameRatePowerSavingsBalanced(false) applied via reflection")
                } catch (e: Exception) {
                    Log.w(TAG, "LayoutParams reflection also failed: ${e.message}")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isFinishing || isDestroyed) return

        try {
            setHighRefreshRateIfNeeded()
        } catch (e: Exception) {
            Log.w(TAG, "onResume setHighRefreshRate failed: ${e.message}")
        }

        refreshRateHandler.removeCallbacks(refreshRateRunnable)
        refreshRateHandler.postDelayed(refreshRateRunnable, REFRESH_RATE_INTERVAL_MS)
    }

    override fun onPause() {
        refreshRateHandler.removeCallbacks(refreshRateRunnable)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle deep links from notifications and shortcuts
        intent.data?.let { uri ->
            val request = NavDeepLinkRequest.Builder.fromUri(uri).build()
            navController?.handleDeepLink(request)
        }
        // Ensure the new intent is used by LaunchedEffect in MainScreen
        setIntent(intent)
    }

    override fun onDestroy() {
        refreshRateHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun setHighRefreshRateIfNeeded() {
        if (isFinishing || isDestroyed) return

        val lp = try {
            window.attributes
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get window.attributes: ${e.message}")
            return
        }

        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        }

        if (display == null) {
            Log.w(TAG, "Display is null, cannot set high refresh rate")
            return
        }

        try {
            val mode = display.mode
            val supportedModes = display.supportedModes

            if (supportedModes.isNullOrEmpty()) {
                Log.w(TAG, "No supported display modes available")
                return
            }

            val bestMode = supportedModes
                .asSequence()
                .filter {
                    try {
                        it.physicalWidth == mode.physicalWidth && it.physicalHeight == mode.physicalHeight
                    } catch (e: Exception) {
                        false
                    }
                }
                .maxByOrNull {
                    try {
                        it.refreshRate
                    } catch (e: Exception) {
                        0f
                    }
                }

            val targetMode = bestMode ?: mode
            val targetRefreshRate = targetMode.refreshRate
            val targetModeId = targetMode.modeId

            // Only re-apply if the rate actually changed
            if (lastAppliedModeId == targetModeId && lastAppliedRefreshRate == targetRefreshRate) {
                return
            }

            lp.preferredDisplayModeId = targetModeId
            lp.preferredRefreshRate = targetRefreshRate

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                try {
                    val setMethod = lp::class.java.getMethod(
                        "setFrameRatePowerSavingsBalanced",
                        Boolean::class.javaPrimitiveType
                    )
                    setMethod.invoke(lp, false)
                } catch (e: Exception) {
                }
            }

            window.attributes = lp
            lastAppliedModeId = targetModeId
            lastAppliedRefreshRate = targetRefreshRate
            Log.d(TAG, "Set refresh rate: ${targetRefreshRate.toInt()}Hz, modeId=$targetModeId")

        } catch (_: Exception) {
            // Log.w(TAG, "setHighRefreshRate inner exception: ${e.message}")
        }
    }

    private fun isOnline(): Boolean {
        return try {
            val cm = getSystemService(ConnectivityManager::class.java)
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            Log.w(TAG, "isOnline check failed: ${e.message}")
            false
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val REFRESH_RATE_INTERVAL_MS = 500L
    }
}
