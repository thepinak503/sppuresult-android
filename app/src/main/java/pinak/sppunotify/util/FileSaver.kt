package pinak.sppunotify.util

import android.content.Context
import android.net.Uri

object FileSaver {
    fun saveToUri(context: Context, bytes: ByteArray, uri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(bytes)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
