package pinak.sppunotify.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
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
    var dragProgress by remember { mutableStateOf(0f) }

    // Visual feedback for dragging
    val alpha by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0.4f,
        animationSpec = tween(200),
        label = "alpha"
    )

    val layoutInfo = listState.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val visibleItems = layoutInfo.visibleItemsInfo

    // Hide if nothing to scroll
    if (totalItems <= 0 || visibleItems.isEmpty() || visibleItems.size >= totalItems) return

    val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
    
    // Calculate current scroll progress from list state (when not dragging)
    val stateProgress by remember {
        derivedStateOf {
            val index = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            val firstItemSize = visibleItems.firstOrNull()?.size ?: 1
            val totalScrollableItems = (totalItems.toFloat() - (viewportHeight.toFloat() / firstItemSize)).coerceAtLeast(1f)
            val currentScrollPos = index.toFloat() + (offset.toFloat() / firstItemSize)
            (currentScrollPos / totalScrollableItems).coerceIn(0f, 1f)
        }
    }

    // Display progress is either the live state or the finger's drag position
    val currentProgress = if (isDragging) dragProgress else stateProgress

    val thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)

    Canvas(
        modifier = modifier
            .width(32.dp) // Large touch area
            .fillMaxHeight()
            .pointerInput(totalItems) {
                detectVerticalDragGestures(
                    onDragStart = { 
                        isDragging = true
                        dragProgress = stateProgress
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val trackHeight = size.height.toFloat()
                        val thumbHeight = (trackHeight * (visibleItems.size.toFloat() / totalItems.toFloat())).coerceAtLeast(60f)
                        val scrollableArea = trackHeight - thumbHeight
                        
                        if (scrollableArea > 0) {
                            // Move the virtual thumb
                            val deltaProgress = dragAmount / scrollableArea
                            dragProgress = (dragProgress + deltaProgress).coerceIn(0f, 1f)
                            
                            // Scroll the list to match the virtual thumb
                            val targetIndex = (dragProgress * (totalItems - 1)).toInt().coerceIn(0, totalItems - 1)
                            coroutineScope.launch {
                                listState.scrollToItem(targetIndex)
                            }
                        }
                    }
                )
            }
    ) {
        val trackHeight = size.height
        val thumbHeight = (trackHeight * (visibleItems.size.toFloat() / totalItems.toFloat())).coerceAtLeast(60f)
        val thumbOffset = currentProgress * (trackHeight - thumbHeight)

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

@Composable
fun VerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    // Intentionally left blank as per user request to hide in settings/about
}
