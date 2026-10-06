package ru.embtlab.smartprice.presentation.compare.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun AutoScrollCompareList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    focusedIndex: Int?,
    onScrollFinished: () -> Unit = {},
    baseContentPadding: PaddingValues = PaddingValues(top = 16.dp, bottom = 24.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: LazyListScope.() -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // 1. Получаем высоту клавиатуры в dp для contentPadding
    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    // 2. Получаем высоту клавиатуры в px для точного расчета скролла
    val keyboardHeightPx = WindowInsets.ime.getBottom(density)
    val isKeyboardVisible = keyboardHeightPx > 0
    var viewportHeightPx by remember { mutableIntStateOf(0) }
    val bottomMarginPx = with(density) { 12.dp.roundToPx() }

    // Программный расчет смещения и доскролл карточки к верхнему срезу клавиатуры
    LaunchedEffect(focusedIndex, keyboardHeightPx) {
        if (focusedIndex == null || !isKeyboardVisible || viewportHeightPx == 0) return@LaunchedEffect

        delay(300L.milliseconds)

        val targetItem = state.layoutInfo.visibleItemsInfo.find { it.index == focusedIndex } ?: return@LaunchedEffect
        val visibleAreaHeight = viewportHeightPx - keyboardHeightPx

        // Если карточка УЖЕ полностью видна над клавиатурой — не дергаем список
        val isAlreadyVisible = (targetItem.offset + targetItem.size) <= visibleAreaHeight && targetItem.offset >= 0
        if (isAlreadyVisible) {
            onScrollFinished()
            return@LaunchedEffect
        }

        val cardHeightPx = targetItem.size
        val targetScrollOffset = if (cardHeightPx in 1..<visibleAreaHeight) {
            -(visibleAreaHeight - cardHeightPx - bottomMarginPx)
        } else {
            0
        }

        coroutineScope.launch {
            state.animateScrollToItem(
                index = focusedIndex,
                scrollOffset = targetScrollOffset.coerceAtLeast(0)
            )
            onScrollFinished()
        }
    }

    LazyColumn(
        state = state,
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                viewportHeightPx = coordinates.size.height
            },
        // Расширяем нижний отступ контента на высоту клавиатуры вместо imePadding()
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = max(24.dp, imeBottomPadding + 24.dp)
        ),
        verticalArrangement = verticalArrangement,
        content = content
    )
}