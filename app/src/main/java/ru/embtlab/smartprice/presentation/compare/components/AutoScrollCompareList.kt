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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun AutoScrollCompareList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    focusedIndex: Int?,
    scrollTrigger: Long = 0L,
    onScrollFinished: () -> Unit = {},
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: LazyListScope.() -> Unit
) {
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val imeBottomPadding = imeInsets.asPaddingValues().calculateBottomPadding()

    var viewportHeightPx by remember { mutableIntStateOf(0) }
    val bottomMarginPx = with(density) { 16.dp.roundToPx() }

    // Следим за появлением клавиатуры и фокусом, без задержек delay()
    LaunchedEffect(focusedIndex, scrollTrigger) {
        if (focusedIndex == null) return@LaunchedEffect

        snapshotFlow { imeInsets.getBottom(density) }
            .filter { it > 0 }
            .distinctUntilChanged()
            .collectLatest { currentKeyboardHeightPx ->
                if (viewportHeightPx == 0) return@collectLatest

                // Находим текущий элемент
                var targetItem = state.layoutInfo.visibleItemsInfo.find { it.index == focusedIndex }
                if (targetItem == null) {
                    state.scrollToItem(focusedIndex)
                    targetItem = state.layoutInfo.visibleItemsInfo.find { it.index == focusedIndex }
                }

                if (targetItem == null) return@collectLatest

                val cardHeightPx = targetItem.size
                val visibleAreaHeight = viewportHeightPx - currentKeyboardHeightPx

                // Если карточка помещается целиком над клавиатурой, опускаем/поднимаем её к нижнему краю видимой зоны
                val targetOffset = if (cardHeightPx in 1..<visibleAreaHeight) {
                    -(visibleAreaHeight - cardHeightPx - bottomMarginPx)
                } else {
                    0
                }

                state.animateScrollToItem(
                    index = focusedIndex,
                    scrollOffset = targetOffset.coerceAtLeast(0)
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
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = max(32.dp, imeBottomPadding + 80.dp)
        ),
        verticalArrangement = verticalArrangement,
        content = content
    )
}