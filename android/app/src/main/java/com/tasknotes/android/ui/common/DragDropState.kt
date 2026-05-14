package com.tasknotes.android.ui.common

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DragDropState(
    val lazyListState: LazyListState,
    private val scope: CoroutineScope,
    private val onSwap: (Int, Int) -> Unit,
    private val onDragFinished: () -> Unit = {}
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set
    var draggingItemOffset by mutableStateOf(0f)
        private set
    private var scrollJob: Job? = null

    private val currentItem
        get() = lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == draggingItemIndex }

    fun onDragStart(offset: Offset) {
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
            ?.also { draggingItemIndex = it.index }
    }

    fun onDragEnd() {
        if (draggingItemIndex != null) onDragFinished()
        draggingItemIndex  = null
        draggingItemOffset = 0f
        scrollJob?.cancel()
    }

    fun onDrag(offset: Offset) {
        draggingItemOffset += offset.y
        val dragging = currentItem ?: return
        val top    = dragging.offset + draggingItemOffset
        val bottom = top + dragging.size
        val mid    = (top + bottom) / 2f

        val target = lazyListState.layoutInfo.visibleItemsInfo.find { item ->
            mid.toInt() in item.offset until (item.offset + item.size) &&
                item.index != dragging.index
        } ?: return

        onSwap(dragging.index, target.index)
        draggingItemOffset = top - target.offset
        draggingItemIndex  = target.index

        if (target.index == lazyListState.firstVisibleItemIndex) {
            scrollJob = scope.launch {
                lazyListState.scrollToItem(
                    dragging.index,
                    lazyListState.firstVisibleItemScrollOffset
                )
            }
        }
    }
}

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    onSwap: (Int, Int) -> Unit,
    onDragFinished: () -> Unit = {}
): DragDropState {
    val scope = rememberCoroutineScope()
    return remember(lazyListState) { DragDropState(lazyListState, scope, onSwap, onDragFinished) }
}

fun Modifier.dragContainer(state: DragDropState): Modifier =
    pointerInput(state) {
        detectDragGesturesAfterLongPress(
            onDragStart  = { state.onDragStart(it) },
            onDrag       = { change, offset -> change.consume(); state.onDrag(offset) },
            onDragEnd    = { state.onDragEnd() },
            onDragCancel = { state.onDragEnd() }
        )
    }
