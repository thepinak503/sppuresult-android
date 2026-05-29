package pinak.sppunotify.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import pinak.sppunotify.data.repository.RemoteConfigRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val preferenceManager: pinak.sppunotify.data.local.PreferenceManager
) {
    fun downloadUpdate(url: String, versionName: String) {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("SPPU Result Watch Update v$versionName")
            .setDescription("Downloading new version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "update.apk")
            .setMimeType("application/vnd.android.package-archive")

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
    }

    fun installUpdate(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    suspend fun checkAndNotifyUpdate(notificationHelper: NotificationHelper) {
        val config = remoteConfigRepository.fetchAppConfig() ?: return
        
        val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        
        val currentVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pInfo.versionCode.toLong()
        }

        if (config.latestVersionCode > currentVersion) {
            val prefs = preferenceManager.preferencesFlow.first()
            if (prefs.autoUpdateEnabled) {
                downloadUpdate(config.updateUrl, config.latestVersionName)
            } else {
                notificationHelper.showUpdateNotification(
                    versionName = config.latestVersionName,
                    updateUrl = config.updateUrl
                )
            }
        }
    }
}
