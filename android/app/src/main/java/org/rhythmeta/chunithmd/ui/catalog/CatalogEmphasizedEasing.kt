package org.rhythmeta.chunithmd.ui.catalog

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Material 3 emphasized path, expressed as two normalized cubic segments.
 * https://github.com/material-components/material-components-android/blob/master/docs/theming/Motion.md
 * M 0,0 C 0.05,0 0.133333,0.06 0.166666,0.4 C 0.208333,0.82 0.25,1 1,1
 */
internal val CatalogEmphasizedEasing: Easing = run {
    val joinX = 0.166666f
    val joinY = 0.4f
    val first = CubicBezierEasing(0.05f / joinX, 0f, 0.133333f / joinX, 0.06f / joinY)
    val second = CubicBezierEasing(
        (0.208333f - joinX) / (1f - joinX), (0.82f - joinY) / (1f - joinY),
        (0.25f - joinX) / (1f - joinX), 1f,
    )
    Easing { fraction ->
        when {
            fraction <= 0f -> 0f
            fraction >= 1f -> 1f
            fraction < joinX -> joinY * first.transform(fraction / joinX)
            else -> joinY + (1f - joinY) * second.transform((fraction - joinX) / (1f - joinX))
        }
    }
}
