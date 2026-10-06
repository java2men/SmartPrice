// presentation/compare/components/AutoScrollCompareList.kt
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

@Composable
fun AutoScrollCompareList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    focusedIndex: Int?,
    onScrollFinished: () -> Unit = {},
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: LazyListScope.() -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // Высота клавиатуры в dp и px
    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val keyboardHeightPx = WindowInsets.ime.getBottom(density)
    val isKeyboardVisible = keyboardHeightPx > 0

    var viewportHeightPx by remember { mutableIntStateOf(0) }
    val bottomMarginPx = with(density) { 16.dp.roundToPx() }

    LaunchedEffect(focusedIndex, keyboardHeightPx) {
        if (focusedIndex == null || !isKeyboardVisible || viewportHeightPx == 0) return@LaunchedEffect

        // 1. Ожидаем завершения выезда клавиатуры (для Funtouch / OriginOS нужно около 180-200мс)
        delay(180L)

        // 2. Ожидаем, пока элемент гарантированно появится в лейауте
        var targetItem = state.layoutInfo.visibleItemsInfo.find { it.index == focusedIndex }
        var retries = 0
        while (targetItem == null && retries < 3) {
            state.scrollToItem(focusedIndex)
            delay(50L)
            targetItem = state.layoutInfo.visibleItemsInfo.find { it.index == focusedIndex }
            retries++
        }

        val cardHeightPx = targetItem?.size ?: with(density) { 220.dp.roundToPx() }
        val visibleAreaHeight = viewportHeightPx - keyboardHeightPx

        // 3. Расчет точного смещения
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
        // КЛЮЧЕВОЙ МОМЕНТ:
        // Добавляем к нижней границе не только высоту клавиатуры, но и дополнительный зазор,
        // чтобы последней карточке всегда было физическое место проскроллиться наверх
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = max(32.dp, imeBottomPadding + 80.dp)
        ),
        verticalArrangement = verticalArrangement,
        content = content
    )
}