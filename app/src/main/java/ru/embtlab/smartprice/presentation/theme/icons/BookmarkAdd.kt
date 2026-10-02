package ru.embtlab.smartprice.presentation.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
public val AppIcons.Default.BookmarkAdd: ImageVector
    get() {
        if (_BookmarkAdd != null) {
            return _BookmarkAdd!!
        }
        _BookmarkAdd =
            ImageVector.Builder(
                name = "bookmark_add",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    path(
                        fill = SolidColor(Color.Black),
                        fillAlpha = 1f,
                        stroke = null,
                        strokeAlpha = 1f,
                        strokeLineWidth = 1f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Bevel,
                        strokeLineMiter = 1f,
                        pathFillType = PathFillType.Companion.NonZero,
                    ) {
                        moveTo(5f, 21f)
                        verticalLineTo(5f)
                        quadTo(5f, 4.17f, 5.59f, 3.59f)
                        reflectiveQuadTo(7f, 3f)
                        horizontalLineToRelative(6f)
                        quadToRelative(0f, 0.57f, 0f, 1f)
                        quadToRelative(0f, 0.42f, 0f, 1f)
                        horizontalLineTo(7f)
                        verticalLineTo(17.95f)
                        lineTo(12f, 15.8f)
                        lineToRelative(5f, 2.15f)
                        verticalLineTo(11f)
                        quadToRelative(0.57f, 0f, 1f, 0f)
                        reflectiveQuadToRelative(1f, 0f)
                        verticalLineTo(21f)
                        lineTo(12f, 18f)
                        lineTo(5f, 21f)
                        close()
                        moveTo(7f, 5f)
                        horizontalLineToRelative(6f)
                        horizontalLineTo(12f)
                        horizontalLineTo(7f)
                        close()
                        moveTo(17f, 9f)
                        verticalLineTo(7f)
                        horizontalLineTo(15f)
                        verticalLineTo(5f)
                        horizontalLineToRelative(2f)
                        verticalLineTo(3f)
                        horizontalLineToRelative(2f)
                        verticalLineTo(5f)
                        horizontalLineToRelative(2f)
                        verticalLineTo(7f)
                        horizontalLineTo(19f)
                        verticalLineTo(9f)
                        horizontalLineTo(17f)
                        close()
                    }
                }
                .build()
        return _BookmarkAdd!!
    }

private var _BookmarkAdd: ImageVector? = null