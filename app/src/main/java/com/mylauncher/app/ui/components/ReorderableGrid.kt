package com.mylauncher.app.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mylauncher.app.launcher.home.HomeGridOps
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A grid whose items can be dragged into a new order.
 *
 *  - [reorderEnabled]: dragging starts at once (the explicit "reorder" mode).
 *  - [longPressDrag]: dragging starts after a long press. If the finger lifts without moving, the
 *    long press is reported through [onItemLongPress] instead (the app menu), so a long press
 *    still opens the menu and a long press + move reorders.
 *
 * The new order is kept locally while dragging and reported once, when the finger lifts, and only if it
 * really changed. If the gesture is interrupted the order goes back to what it was and nothing is saved.
 * Items have stable string keys, so nothing is ever duplicated or lost.
 *
 * `itemContent`'s second argument is true while that item is being dragged and for a moment after it
 * was dropped, so the caller can ignore the tap that Android may deliver when the finger lifts.
 */
@Composable
fun <T> ReorderableGrid(
    items: List<T>,
    key: (T) -> String,
    columns: Int,
    reorderEnabled: Boolean,
    onReorder: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    longPressDrag: Boolean = false,
    onItemLongPress: ((T) -> Unit)? = null,
    itemContent: @Composable (item: T, dragging: Boolean) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val sourceKeys = items.map(key)
    var order by remember(sourceKeys) { mutableStateOf(sourceKeys) }
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var suppressKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var movedPx by remember { mutableFloatStateOf(0f) }
    val byKey = remember(items) { items.associateBy(key) }
    val currentOrder by rememberUpdatedState(order)
    val currentSource by rememberUpdatedState(sourceKeys)
    val currentByKey by rememberUpdatedState(byKey)
    val currentOnReorder by rememberUpdatedState(onReorder)
    val currentOnLongPress by rememberUpdatedState(onItemLongPress)
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val tapSlopPx = with(LocalDensity.current) { 10.dp.toPx() }

    // If the dragged item disappears (for example the app was uninstalled), forget the drag.
    LaunchedEffect(sourceKeys) {
        val dragged = draggedKey
        if (dragged != null && dragged !in sourceKeys) {
            draggedKey = null
            dragOffset = Offset.Zero
        }
    }

    fun startDrag(itemKey: String) {
        draggedKey = itemKey
        suppressKey = itemKey
        dragOffset = Offset.Zero
        movedPx = 0f
    }

    fun finishDrag(itemKey: String, commit: Boolean, fromLongPress: Boolean) {
        val finalOrder = currentOrder
        val moved = movedPx
        draggedKey = null
        dragOffset = Offset.Zero
        if (commit) {
            if (HomeGridOps.changed(currentSource, finalOrder)) {
                currentOnReorder(finalOrder)
            } else if (fromLongPress && moved < tapSlopPx) {
                currentByKey[itemKey]?.let { currentOnLongPress?.invoke(it) }
            }
        } else {
            order = currentSource
        }
        // Keep ignoring clicks on this item for a moment: the lifted finger must not launch the app.
        scope.launch {
            delay(350)
            if (suppressKey == itemKey) suppressKey = null
        }
    }

    fun swapIfNeeded(draggedItemKey: String) {
        val info = gridState.layoutInfo.visibleItemsInfo
        if (info.isEmpty()) return
        // Ignore events that arrive before the grid has laid out the previous swap.
        val expected = currentOrder.drop(info.first().index).take(info.size)
        if (info.map { it.key } != expected) return
        val dragged = info.firstOrNull { it.key == draggedItemKey } ?: return
        val centerX = dragged.offset.x + dragged.size.width / 2f + dragOffset.x
        val centerY = dragged.offset.y + dragged.size.height / 2f + dragOffset.y
        val target = info.firstOrNull {
            it.key != draggedItemKey &&
                centerX >= it.offset.x && centerX < it.offset.x + it.size.width &&
                centerY >= it.offset.y && centerY < it.offset.y + it.size.height
        } ?: return
        val from = currentOrder.indexOf(draggedItemKey)
        val to = currentOrder.indexOf(target.key as String)
        if (from < 0 || to < 0 || from == to) return
        // Keep the dragged item under the finger after it jumps to its new cell.
        dragOffset += Offset(
            (dragged.offset.x - target.offset.x).toFloat(),
            (dragged.offset.y - target.offset.y).toFloat(),
        )
        order = HomeGridOps.move(currentOrder, draggedItemKey, to)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        modifier = modifier,
    ) {
        items(order, key = { it }) { itemKey ->
            val item = byKey[itemKey]
            if (item != null) {
                val dragging = itemKey == draggedKey
                val dragModifier = when {
                    reorderEnabled -> Modifier.pointerInput(itemKey) {
                        detectDragGestures(
                            onDragStart = { startDrag(itemKey) },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount
                                movedPx += amount.getDistance()
                                swapIfNeeded(itemKey)
                            },
                            onDragEnd = { finishDrag(itemKey, commit = true, fromLongPress = false) },
                            onDragCancel = { finishDrag(itemKey, commit = false, fromLongPress = false) },
                        )
                    }
                    longPressDrag -> Modifier.pointerInput(itemKey) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                startDrag(itemKey)
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount
                                movedPx += amount.getDistance()
                                swapIfNeeded(itemKey)
                            },
                            onDragEnd = { finishDrag(itemKey, commit = true, fromLongPress = true) },
                            onDragCancel = { finishDrag(itemKey, commit = false, fromLongPress = true) },
                        )
                    }
                    else -> Modifier
                }
                Box(
                    Modifier
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer {
                            if (dragging) {
                                translationX = dragOffset.x
                                translationY = dragOffset.y
                                scaleX = 1.08f
                                scaleY = 1.08f
                                alpha = 0.92f
                            }
                        }
                        .then(dragModifier),
                ) {
                    itemContent(item, dragging || itemKey == suppressKey)
                }
            }
        }
    }
}
