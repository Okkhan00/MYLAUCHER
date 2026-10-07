package com.mylauncher.app.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex

/**
 * A grid whose items can be dragged into a new order when [reorderEnabled] is true.
 * The new order is kept locally while dragging and reported once, when the finger lifts.
 * Items have stable string keys, so nothing is ever duplicated.
 */
@Composable
fun <T> ReorderableGrid(
    items: List<T>,
    key: (T) -> String,
    columns: Int,
    reorderEnabled: Boolean,
    onReorder: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (item: T, dragging: Boolean) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val sourceKeys = items.map(key)
    var order by remember(sourceKeys) { mutableStateOf(sourceKeys) }
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val byKey = remember(items) { items.associateBy(key) }
    val currentOrder by rememberUpdatedState(order)
    val currentOnReorder by rememberUpdatedState(onReorder)

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
        order = currentOrder.toMutableList().apply {
            removeAt(from)
            add(to, draggedItemKey)
        }
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
                val dragModifier = if (reorderEnabled) {
                    Modifier.pointerInput(itemKey) {
                        detectDragGestures(
                            onDragStart = {
                                draggedKey = itemKey
                                dragOffset = Offset.Zero
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount
                                swapIfNeeded(itemKey)
                            },
                            onDragEnd = {
                                draggedKey = null
                                dragOffset = Offset.Zero
                                currentOnReorder(currentOrder)
                            },
                            onDragCancel = {
                                draggedKey = null
                                dragOffset = Offset.Zero
                                currentOnReorder(currentOrder)
                            },
                        )
                    }
                } else {
                    Modifier
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
                    itemContent(item, dragging)
                }
            }
        }
    }
}
