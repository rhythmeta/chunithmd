package org.rhythmeta.chunithmd.ui.components

import androidx.compose.ui.graphics.Color

/**
 * Flat tier colour for a rating on the CHUNITHM 0–17 scale.
 *
 * Green < 4, orange < 7, red < 10, purple < 12, bronze < 13.25, silver < 14.5, gold < 15.25,
 * platinum < 16, rainbow at 16+.
 */
internal fun ratingTierColor(rating: Double): Color = when {
    rating >= 16.0 -> Color(0xFFFF6100)
    rating >= 15.25 -> Color(0xFFE5E4E2)
    rating >= 14.50 -> Color(0xFFFFD700)
    rating >= 13.25 -> Color(0xFFC0C0C0)
    rating >= 12.00 -> Color(0xFFCD7F32)
    rating >= 10.00 -> Color(0xFFD084FF)
    rating >= 7.00 -> Color(0xFFFF5E5E)
    rating >= 4.00 -> Color(0xFFFF9500)
    else -> Color(0xFF46D246)
}

/** Gradient stops used wherever a rating is drawn with its tier material. */
internal fun playerRatingColors(rating: Double): List<Color> = when {
    rating >= 16.0 -> listOf(
        Color(0xFFFF5E5E),
        Color(0xFFFFF75E),
        Color(0xFF5EFF5E),
        Color(0xFF5EBAFF),
        Color(0xFFBA5EFF),
    )
    rating >= 15.25 -> listOf(Color.LightGray, Color.White, Color.LightGray)
    rating >= 14.5 -> listOf(Color(0xFFFFD700), Color(0xFFFFA500))
    else -> listOf(ratingTierColor(rating), ratingTierColor(rating))
}
