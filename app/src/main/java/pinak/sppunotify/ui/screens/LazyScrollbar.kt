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
import kotlinx.coroutines.launch

@Composable
fun LazyScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }

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
                val avgItemSize = visibleItems.map { it.size }.average().toFloat().coerceAtLeast(1f)
                val totalScrollRange = (totalItems * avgItemSize).coerceAtLeast(1f)
                val scrollOffset = (listState.firstVisibleItemIndex * avgItemSize) + listState.firstVisibleItemScrollOffset
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
        targetValue = if (isDragging) 1f else 0.4f,
        animationSpec = tween(200),
        label = "alpha"
    )

    val thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)

    Canvas(
        modifier = modifier
            .width(32.dp) // Large touch area
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
                        val currentVisible = currentLayoutInfo.visibleItemsInfo
                        if (currentTotal > 0 && currentVisible.isNotEmpty()) {
                            val trackHeight = size.height.toFloat()
                            val thumbH = (trackHeight * (currentVisible.size.toFloat() / currentTotal.toFloat())).coerceAtLeast(60f)
                            val scrollable = trackHeight - thumbH
                            
                            if (scrollable > 0) {
                                val deltaP = dragAmount / scrollable
                                val newP = (stateProgress + deltaP).coerceIn(0f, 1f)
                                val targetIdx = (newP * (currentTotal - 1)).toInt().coerceIn(0, currentTotal - 1)
                                coroutineScope.launch {
                                    listState.scrollToItem(targetIdx)
                                }
                            }
                        }
                    }
                )
            }
    ) {
        val trackHeight = size.height
        val thumbHeight = (trackHeight * (visibleItemCount.toFloat() / totalItems.toFloat())).coerceAtLeast(60f)
        val thumbOffset = stateProgress * (trackHeight - thumbHeight)

        // Draw track
        drawRoundRect(
            color = trackColor,
            size = Size(10.dp.toPx(), trackHeight),
            topLeft = Offset(11.dp.toPx(), 0f),
            cornerRadius = CornerRadius(5.dp.toPx())
        )
        
        // Draw thumb
        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(x = 9.dp.toPx(), y = thumbOffset),
            size = Size(14.dp.toPx(), thumbHeight),
            cornerRadius = CornerRadius(7.dp.toPx())
        )
    }
}

private data class ScrollInfo(
    val progress: Float,
    val totalItems: Int,
    val visibleItemCount: Int,
)
