package ru.embtlab.smartprice.presentation.scanner.components

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp

@Composable
fun BoxScope.ScannerOverlayBox(
    isFrozen: Boolean,
    frozenBitmap: Bitmap?,
    onFramePositioned: (Rect) -> Unit
) {
    if (isFrozen && frozenBitmap != null) {
        Image(
            bitmap = frozenBitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.40f)))
    }

    Box(
        modifier = Modifier
            .size(width = 310.dp, height = 150.dp)
            .align(Alignment.Center)
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                onFramePositioned(
                    Rect(
                        pos.x.toInt(),
                        pos.y.toInt(),
                        (pos.x + coords.size.width).toInt(),
                        (pos.y + coords.size.height).toInt()
                    )
                )
            }
            .border(
                width = 2.dp,
                color = if (isFrozen) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp)
            )
    )
}