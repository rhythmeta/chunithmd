package org.rhythmeta.chunithmd.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val FishIcon: ImageVector by lazy {
    ImageVector.Builder(name = "Fish", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
            moveTo(7f, 12f)
            curveTo(10f, 5f, 17f, 4f, 22f, 12f)
            curveTo(17f, 20f, 10f, 19f, 7f, 12f)
            lineTo(3f, 16.5f)
            quadTo(2f, 17.5f, 2f, 16f)
            lineTo(2f, 8f)
            quadTo(2f, 6.5f, 3f, 7.5f)
            close()
            moveTo(18.5f, 11f)
            curveTo(18.5f, 11.55f, 18.05f, 12f, 17.5f, 12f)
            curveTo(16.95f, 12f, 16.5f, 11.55f, 16.5f, 11f)
            curveTo(16.5f, 10.45f, 16.95f, 10f, 17.5f, 10f)
            curveTo(18.05f, 10f, 18.5f, 10.45f, 18.5f, 11f)
            close()
        }
    }.build()
}
