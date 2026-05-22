package pinak.sppunotify.util

import android.content.Context
import android.content.Intent
import android.widget.Toast

fun Context.safeStartActivity(intent: Intent, errorMessage: String = "No app available to handle this action") {
    try {
        startActivity(intent)
    } catch (e: android.content.ActivityNotFoundException) {
        android.widget.Toast.makeText(this, errorMessage, android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(this, "An unexpected error occurred", android.widget.Toast.LENGTH_SHORT).show()
    }
}
