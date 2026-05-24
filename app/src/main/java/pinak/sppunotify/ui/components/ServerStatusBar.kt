package pinak.sppunotify.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import pinak.sppunotify.data.remote.ServerStatus
import pinak.sppunotify.data.remote.StatusLevel

@Composable
fun ServerStatusBar(
    serverStatus: ServerStatus?,
    modifier: Modifier = Modifier,
) {
    serverStatus?.let { status ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
        ) {
            Icon(
                imageVector = Icons.Default.Circle,
                contentDescription = null,
                modifier = Modifier.size(8.dp),
                tint = when (status.statusLevel) {
                    StatusLevel.HEALTHY -> Color(0xFF4CAF50)
                    StatusLevel.SLOW -> Color(0xFFFFC107)
                    StatusLevel.BUSY -> Color(0xFFFF9800)
                    StatusLevel.DOWN -> Color(0xFFF44336)
                }
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = when (status.statusLevel) {
                    StatusLevel.HEALTHY -> "Online (${status.responseTimeMs}ms)"
                    StatusLevel.SLOW -> "Slow (${status.responseTimeMs}ms)"
                    StatusLevel.BUSY -> "Busy (Server Overloaded)"
                    StatusLevel.DOWN -> "Down"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
