package pinak.sppunotify.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun LazyScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    var isDragging by remember { mutableStateOf(false) }
    
    // Channel for throttling scroll updates
    val scrollChannel = remember { Channel<Int>(Channel.CONFLATED) }
    
    LaunchedEffect(Unit) {
        scrollChannel.receiveAsFlow().collectLatest { targetIdx ->
            listState.scrollToItem(targetIdx)
        }
    }

    // Hide if nothing to scroll — read layoutInfo lazily inside derivedStateOf
    val scrollInfo by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val totalItems = info.totalItemsCount
            val visibleItems = info.visibleItemsInfo
            if (totalItems <= 0 || visibleItems.isEmpty() || visibleItems.size >= totalItems) {
                null
            } else {
                val viewportHeight = info.viewportEndOffset - info.viewportStartOffset
                // Use a more stable calculation for progress
                val firstItem = visibleItems.firstOrNull()
                val lastItem = visibleItems.lastOrNull()
                
                if (firstItem == null || lastItem == null) return@derivedStateOf null
                
                val avgItemSize = info.visibleItemsInfo.map { it.size }.average().toFloat().coerceAtLeast(1f)
                val scrollOffset = (listState.firstVisibleItemIndex * avgItemSize) + listState.firstVisibleItemScrollOffset
                val totalScrollRange = totalItems * avgItemSize
                val scrollableRange = (totalScrollRange - viewportHeight).coerceAtLeast(1f)
                
                val progress = (scrollOffset / scrollableRange).coerceIn(0f, 1f)
                ScrollInfo(progress, totalItems, visibleItems.size)
            }
        }
    }

    val info = scrollInfo ?: return
    val stateProgress = info.progress
    val totalItems = info.totalItems
    val visibleItemCount = info.visibleItemCount

    // Visual feedback for dragging
    val alpha by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0.45f,
        animationSpec = tween(if (isDragging) 100 else 400),
        label = "alpha"
    )

    val thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)

    Canvas(
        modifier = modifier
            .width(20.dp) // Narrower touch area
            .fillMaxHeight()
            .pointerInput(listState) {
                detectVerticalDragGestures(
                    onDragStart = { 
                        isDragging = true
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val currentLayoutInfo = listState.layoutInfo
                        val currentTotal = currentLayoutInfo.totalItemsCount
                        if (currentTotal > 0) {
                            val trackHeight = size.height.toFloat()
                            val visibleCount = currentLayoutInfo.visibleItemsInfo.size
                            val thumbH = (trackHeight * (visibleCount.toFloat() / currentTotal.toFloat())).coerceAtLeast(60f)
                            val scrollable = trackHeight - thumbH
                            
                            if (scrollable > 0) {
                                val deltaP = dragAmount / scrollable
                                val newP = (stateProgress + deltaP).coerceIn(0f, 1f)
                                val targetIdx = (newP * (currentTotal - 1)).toInt()
                                scrollChannel.trySend(targetIdx)
                            }
                        }
                    }
                )
            }
    ) {
        val trackHeight = size.height
        val thumbHeight = (trackHeight * (visibleItemCount.toFloat() / totalItems.toFloat())).coerceAtLeast(80f)
        val thumbOffset = stateProgress * (trackHeight - thumbHeight)

        // Draw track - subtler
        drawRoundRect(
            color = trackColor,
            size = Size(4.dp.toPx(), trackHeight),
            topLeft = Offset(14.dp.toPx(), 0f),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
        
        // Draw thumb - cleaner
        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(x = 12.dp.toPx(), y = thumbOffset),
            size = Size(8.dp.toPx(), thumbHeight),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
    }
}

private data class ScrollInfo(
    val progress: Float,
    val totalItems: Int,
    val visibleItemCount: Int,
)
