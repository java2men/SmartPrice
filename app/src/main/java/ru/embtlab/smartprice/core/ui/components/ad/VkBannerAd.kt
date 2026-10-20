package ru.embtlab.smartprice.core.ui.components.ad

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.my.target.ads.MyTargetView
import com.my.target.ads.MyTargetView.AdSize
import com.my.target.common.models.IAdLoadingError
import ru.embtlab.smartprice.core.ad.AdConfig

@Composable
fun VkBannerAd(
    modifier: Modifier = Modifier,
    slotId: Int = AdConfig.VK_BANNER_SLOT_ID,
    adSize: AdSize = AdSize.ADSIZE_320x50,
    onAdLoaded: () -> Unit = {},
    onAdFailed: (String) -> Unit = {}
) {
    if (slotId <= 0) return

    val context = LocalContext.current
    val targetView = remember(slotId) {
        MyTargetView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setSlotId(slotId)
            setAdSize(adSize)
            listener = object : MyTargetView.MyTargetViewListener {
                override fun onLoad(view: MyTargetView) {
                    onAdLoaded()
                }

                override fun onNoAd(error: IAdLoadingError, view: MyTargetView) {
                    onAdFailed(error.message)
                }

                override fun onShow(view: MyTargetView) {}
                override fun onClick(view: MyTargetView) {}
            }
            load()
        }
    }

    DisposableEffect(targetView) {
        onDispose {
            targetView.destroy()
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        factory = { targetView }
    )
}