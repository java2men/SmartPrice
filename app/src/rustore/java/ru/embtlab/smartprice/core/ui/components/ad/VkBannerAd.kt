package ru.embtlab.smartprice.core.ui.components.ad

import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.my.target.ads.MyTargetView
import com.my.target.ads.MyTargetView.AdSize
import com.my.target.common.MyTargetManager
import com.my.target.common.models.IAdLoadingError
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import ru.embtlab.smartprice.core.ad.AdConfig
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "VkBannerAd"

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
                    Log.d(TAG, "Баннер успешно загружен!")
                    onAdLoaded()
                }

                override fun onNoAd(error: IAdLoadingError, view: MyTargetView) {
                    Log.e(TAG, "Ошибка загрузки баннера: ${error.message} (код: ${error.code})")
                    onAdFailed(error.message)
                }

                override fun onShow(view: MyTargetView) {
                    Log.d(TAG, "Баннер отображён на экране")
                }

                override fun onClick(view: MyTargetView) {}
            }
            // Вызов load() убран из блока создания View
        }
    }

    LaunchedEffect(targetView) {
        waitForMyTargetSdkWithTimeout(
            targetView = targetView,
            timeoutMillis = 5000L,
            checkIntervalMillis = 150L,
            onSdkReady = {
                targetView.load()
            },
            onTimeout = {
                Log.e(TAG, "Превышено время ожидания (5000 мс) инициализации MyTarget SDK")
                onAdFailed("SDK initialization timeout")
            }
        )
    }

    DisposableEffect(targetView) {
        onDispose {
            targetView.destroy()
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { targetView }
    )
}

/**
 * Проверка статуса готовности SDK напрямую
 */
private suspend fun waitForMyTargetSdkWithTimeout(
    targetView: MyTargetView,
    timeoutMillis: Long = 5000L,
    checkIntervalMillis: Long = 150L,
    onSdkReady: () -> Unit,
    onTimeout: () -> Unit
) {
    val isReady = withTimeoutOrNull(timeoutMillis.milliseconds) {
        while (!MyTargetManager.isSdkInitialized()) {
            delay(checkIntervalMillis.milliseconds)
        }
        true
    }

    if (isReady == true) {
        Log.d(TAG, "MyTarget SDK инициализирован, выполняем targetView.load()")
        onSdkReady()
    } else {
        onTimeout()
    }
}