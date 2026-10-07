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
    onScrollFinished: () -> Unit = {},
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: LazyListScope.() -> Unit
) {
    val density = LocalDensity.current

    // Получаем инсеты клавиатуры в composable-контексте
    val imeInsets = WindowInsets.ime
    val imeBottomPadding = imeInsets.asPaddingValues().calculateBottomPadding()

    var viewportHeightPx by remember { mutableIntStateOf(0) }
    val bottomMarginPx = with(density) { 16.dp.roundToPx() }

    // Реактивное отслеживание анимации клавиатуры без delay(...)
    LaunchedEffect(focusedIndex) {
        if (focusedIndex == null) return@LaunchedEffect

        // Слушаем изменение высоты инсета IME покадрово во время системной анимации
        snapshotFlow { imeInsets.getBottom(density) }
            .filter { it > 0 } // Пропускаем состояние, пока клавиатура закрыта
            .distinctUntilChanged() // Реагируем на каждый шаг изменения высоты
            .collectLatest { currentKeyboardHeightPx ->
                if (viewportHeightPx == 0) return@collectLatest

                // Находим текущие габариты сфокусированной карточки
                var targetItem = state.layoutInfo.visibleItemsInfo.find { it.index == focusedIndex }

                // Если элемент находится вне видимой зоны, подтягиваем его в лейаут без паузы
                if (targetItem == null) {
                    state.scrollToItem(focusedIndex)
                    targetItem = state.layoutInfo.visibleItemsInfo.find { it.index == focusedIndex }
                }

                val cardHeightPx = targetItem?.size ?: with(density) { 220.dp.roundToPx() }
                val visibleAreaHeight = viewportHeightPx - currentKeyboardHeightPx

                // Двусторонний расчет оффсета:
                // Прижимает нижний срез карточки максимально близко к верхнему срезу клавиатуры
                val targetScrollOffset = if (cardHeightPx in 1..<visibleAreaHeight) {
                    -(visibleAreaHeight - cardHeightPx - bottomMarginPx)
                } else {
                    0
                }

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
        // Динамический запас для скролла в самый конец списка без обрезания футера карточки
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = max(32.dp, imeBottomPadding + 80.dp)
        ),
        verticalArrangement = verticalArrangement,
        content = content
    )
}