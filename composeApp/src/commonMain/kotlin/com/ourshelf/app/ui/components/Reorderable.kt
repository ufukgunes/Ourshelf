package com.ourshelf.app.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex

/**
 * LazyColumn için hafif sürükle-bırak sıralama durumu. Liste yalnızca sıralanabilir öğeleri içermelidir
 * (başlık vb. öğeler LazyColumn dışında tutulur), böylece LazyColumn indeksi = liste indeksi olur.
 *
 * Sürüklenen öğe parmağı takip eder; merkezi başka bir öğenin üzerine geldiğinde [onMove] ile yer değiştirilir.
 * Bırakıldığında [onDragEnd] çağrılır (kalıcı kayıt burada yapılır).
 */
@Stable
class ReorderState internal constructor(
    val listState: LazyListState,
    private val onMove: (from: Int, to: Int) -> Unit,
    private val onDragEnd: () -> Unit,
) {
    var draggingKey by mutableStateOf<Any?>(null)
        private set
    var dragOffset by mutableFloatStateOf(0f)
        private set

    fun start(key: Any) {
        draggingKey = key
        dragOffset = 0f
    }

    fun drag(deltaY: Float) {
        val key = draggingKey ?: return
        dragOffset += deltaY
        val items = listState.layoutInfo.visibleItemsInfo
        val current = items.firstOrNull { it.key == key } ?: return
        val center = current.offset + dragOffset + current.size / 2f
        val target = items.firstOrNull { it.key != key && center.toInt() in it.offset..(it.offset + it.size) }
            ?: return
        // Öğe yeni yerine geçince ofset, parmak konumu sabit kalacak şekilde düzeltilir.
        dragOffset += current.offset - target.offset
        onMove(current.index, target.index)
    }

    fun end() {
        if (draggingKey != null) onDragEnd()
        draggingKey = null
        dragOffset = 0f
    }

    fun isDragging(key: Any) = draggingKey == key
}

@Composable
fun rememberReorderState(
    onMove: (from: Int, to: Int) -> Unit,
    onDragEnd: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
): ReorderState {
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    return remember(listState) {
        ReorderState(listState, { f, t -> currentOnMove(f, t) }, { currentOnDragEnd() })
    }
}

/** Sürüklenen öğeyi parmakla birlikte hareket ettirir ve diğerlerinin üstünde çizer. */
fun Modifier.draggedItem(state: ReorderState, key: Any): Modifier =
    if (state.isDragging(key)) {
        zIndex(1f).graphicsLayer {
            translationY = state.dragOffset
            scaleX = 1.03f
            scaleY = 1.03f
            shadowElevation = 12f
        }
    } else {
        this
    }

/** Sürükleme tutamacı: dokunup sürüklemek sıralamayı başlatır. */
fun Modifier.dragHandle(state: ReorderState, key: Any): Modifier =
    pointerInput(key) {
        detectDragGestures(
            onDragStart = { state.start(key) },
            onDragEnd = { state.end() },
            onDragCancel = { state.end() },
        ) { change, amount ->
            change.consume()
            state.drag(amount.y)
        }
    }
