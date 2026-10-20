package ru.embtlab.smartprice.core.ui.components.ad

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun VkBannerAd(
    modifier: Modifier = Modifier,
    slotId: Int = 0,
    onAdLoaded: () -> Unit = {},
    onAdFailed: (String) -> Unit = {}
) {}