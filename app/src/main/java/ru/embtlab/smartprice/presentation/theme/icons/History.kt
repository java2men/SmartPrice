package ru.embtlab.smartprice.presentation.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val AppIcons.Default.History: ImageVector
    get() {
        if (_History != null) return _History!!
        _History = ImageVector.Builder(
            name = "history",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(13f, 3f)
                curveTo(8.03f, 3f, 4f, 7.03f, 4f, 12f)
                horizontalLineTo(1f)
                lineToRelative(4f, 4f)
                lineToRelative(4f, -4f)
                horizontalLineTo(6f)
                curveToRelative(0f, -3.87f, 3.13f, -7f, 7f, -7f)
                curveToRelative(3.87f, 0f, 7f, 3.13f, 7f, 7f)
                reflectiveCurveToRelative(-3.13f, 7f, -7f, 7f)
                curveToRelative(-1.93f, 0f, -3.68f, -0.79f, -4.94f, -2.06f)
                lineToRelative(-1.42f, 1.42f)
                curveTo(8.27f, 19.99f, 10.51f, 21f, 13f, 21f)
                curveToRelative(4.97f, 0f, 9f, -4.03f, 9f, -9f)
                reflectiveCurveToRelative(-4.03f, -9f, -9f, -9f)
                close()
                moveTo(12f, 8f)
                verticalLineToRelative(5f)
                lineToRelative(4.28f, 2.54f)
                lineToRelative(0.72f, -1.21f)
                lineToRelative(-3.5f, -2.08f)
                verticalLineTo(8f)
                horizontalLineTo(12f)
                close()
            }
        }.build()
        return _History!!
    }

private var _History: ImageVector? = null