package com.brokenpip3.gymbro.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object GymbroIcons {
    val Back: ImageVector =
        icon("Back") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
            ) {
                moveTo(19f, 12f)
                horizontalLineTo(5f)
                moveTo(5f, 12f)
                lineTo(11f, 6f)
                moveTo(5f, 12f)
                lineTo(11f, 18f)
            }
        }

    val Schedules: ImageVector =
        icon("Schedules") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
            ) {
                moveTo(7f, 4.5f)
                verticalLineTo(8f)
                moveTo(17f, 4.5f)
                verticalLineTo(8f)
                moveTo(5f, 9f)
                horizontalLineTo(19f)
                moveTo(6.5f, 6.5f)
                horizontalLineTo(17.5f)
                curveTo(18.3f, 6.5f, 19f, 7.2f, 19f, 8f)
                verticalLineTo(18f)
                curveTo(19f, 18.8f, 18.3f, 19.5f, 17.5f, 19.5f)
                horizontalLineTo(6.5f)
                curveTo(5.7f, 19.5f, 5f, 18.8f, 5f, 18f)
                verticalLineTo(8f)
                curveTo(5f, 7.2f, 5.7f, 6.5f, 6.5f, 6.5f)
                close()
            }
        }

    val Exercises: ImageVector =
        icon("Exercises") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero) {
                roundRect(3.5f, 9f, 5.3f, 15f, 0.8f)
                roundRect(6.2f, 7.2f, 8.2f, 16.8f, 0.9f)
                roundRect(9f, 11.1f, 15f, 12.9f, 0.9f)
                roundRect(15.8f, 7.2f, 17.8f, 16.8f, 0.9f)
                roundRect(18.7f, 9f, 20.5f, 15f, 0.8f)
            }
        }

    val Workout: ImageVector =
        icon("Workout") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero) {
                moveTo(8f, 5.8f)
                curveTo(8f, 5.1f, 8.8f, 4.7f, 9.4f, 5.1f)
                lineTo(18.2f, 11.1f)
                curveTo(18.8f, 11.5f, 18.8f, 12.5f, 18.2f, 12.9f)
                lineTo(9.4f, 18.9f)
                curveTo(8.8f, 19.3f, 8f, 18.9f, 8f, 18.2f)
                close()
            }
        }

    val Add: ImageVector =
        icon("Add") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                fill = null,
            ) {
                moveTo(12f, 5f)
                verticalLineTo(19f)
                moveTo(5f, 12f)
                horizontalLineTo(19f)
            }
        }

    val Results: ImageVector =
        icon("Results") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero) {
                roundRect(5f, 12f, 7.5f, 19f, 0.8f)
                roundRect(10.8f, 8f, 13.2f, 19f, 0.8f)
                roundRect(16.5f, 5f, 19f, 19f, 0.8f)
            }
        }

    val Settings: ImageVector =
        icon("Settings") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
            ) {
                moveTo(12f, 8f)
                curveTo(14.2f, 8f, 16f, 9.8f, 16f, 12f)
                curveTo(16f, 14.2f, 14.2f, 16f, 12f, 16f)
                curveTo(9.8f, 16f, 8f, 14.2f, 8f, 12f)
                curveTo(8f, 9.8f, 9.8f, 8f, 12f, 8f)
                close()
                moveTo(12f, 4.5f)
                verticalLineTo(6f)
                moveTo(12f, 18f)
                verticalLineTo(19.5f)
                moveTo(4.5f, 12f)
                horizontalLineTo(6f)
                moveTo(18f, 12f)
                horizontalLineTo(19.5f)
                moveTo(6.7f, 6.7f)
                lineTo(7.8f, 7.8f)
                moveTo(16.2f, 16.2f)
                lineTo(17.3f, 17.3f)
                moveTo(17.3f, 6.7f)
                lineTo(16.2f, 7.8f)
                moveTo(7.8f, 16.2f)
                lineTo(6.7f, 17.3f)
            }
        }

    val ArrowUp: ImageVector =
        icon("ArrowUp") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
            ) {
                moveTo(12f, 19f)
                verticalLineTo(5f)
                moveTo(6f, 11f)
                lineTo(12f, 5f)
                lineTo(18f, 11f)
            }
        }

    val ArrowDown: ImageVector =
        icon("ArrowDown") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
            ) {
                moveTo(12f, 5f)
                verticalLineTo(19f)
                moveTo(6f, 13f)
                lineTo(12f, 19f)
                lineTo(18f, 13f)
            }
        }

    val Edit: ImageVector =
        icon("Edit") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
            ) {
                moveTo(4.5f, 19.5f)
                horizontalLineTo(8f)
                lineTo(19.2f, 8.3f)
                curveTo(20.1f, 7.4f, 20.1f, 6f, 19.2f, 5.1f)
                curveTo(18.3f, 4.2f, 16.9f, 4.2f, 16f, 5.1f)
                lineTo(4.5f, 16.6f)
                close()
                moveTo(14.8f, 6.3f)
                lineTo(17.7f, 9.2f)
            }
        }

    val Delete: ImageVector =
        icon("Delete") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
            ) {
                moveTo(5f, 7.5f)
                horizontalLineTo(19f)
                moveTo(9f, 7.5f)
                verticalLineTo(5f)
                horizontalLineTo(15f)
                verticalLineTo(7.5f)
                moveTo(7f, 7.5f)
                lineTo(7.8f, 19f)
                horizontalLineTo(16.2f)
                lineTo(17f, 7.5f)
                moveTo(10f, 10.5f)
                verticalLineTo(16f)
                moveTo(14f, 10.5f)
                verticalLineTo(16f)
            }
        }
}

private fun icon(
    name: String,
    builder: ImageVector.Builder.() -> Unit,
): ImageVector =
    ImageVector
        .Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply(builder)
        .build()

private fun androidx.compose.ui.graphics.vector.PathBuilder.roundRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    radius: Float,
) {
    moveTo(left + radius, top)
    horizontalLineTo(right - radius)
    quadTo(right, top, right, top + radius)
    verticalLineTo(bottom - radius)
    quadTo(right, bottom, right - radius, bottom)
    horizontalLineTo(left + radius)
    quadTo(left, bottom, left, bottom - radius)
    verticalLineTo(top + radius)
    quadTo(left, top, left + radius, top)
    close()
}
