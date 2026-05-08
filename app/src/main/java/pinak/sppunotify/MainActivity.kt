package pinak.sppunotify

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import pinak.sppunotify.ui.MainScreen
import pinak.sppunotify.ui.theme.SPPUResultWatchTheme
import java.lang.reflect.Method

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ -> }

    private val refreshRateHandler = Handler(Looper.getMainLooper())
    private var lastAppliedModeId = -1
    private var lastAppliedRefreshRate = -1f
    private val refreshRateRunnable = object : Runnable {
        override fun run() {
            if (isFinishing || isDestroyed) {
                Log.w(TAG, "Activity is finishing/destroyed, stopping refresh rate loop")
                return
            }
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

    private var noInternetDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isOnline()) {
            val builder = AlertDialog.Builder(this)
                .setTitle("No Internet")
                .setMessage("An internet connection is required to check SPPU results. Please connect to a network and try again.")
                .setPositiveButton("OK") { _, _ ->
                    finish()
                }
                .setCancelable(false)
            noInternetDialog = builder.create()
            noInternetDialog?.show()
            return
        }

        enableEdgeToEdge()

        try {
            disableFrameRatePowerSavingsIfSupported()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set isFrameRatePowerSavingsBalanced: ${e.message}")
        }

        checkAndRequestNotificationPermission()

        setContent {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                SPPUResultWatchTheme {
                    MainScreen()
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

    override fun onDestroy() {
        refreshRateHandler.removeCallbacksAndMessages(null)
        noInternetDialog?.dismiss()
        noInternetDialog = null
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

        } catch (e: Exception) {
            Log.w(TAG, "setHighRefreshRate inner exception: ${e.message}")
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

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to launch notification permission request: ${e.message}")
                }
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val REFRESH_RATE_INTERVAL_MS = 500L
    }
}
