package org.rhythmeta.chunithmd.ui.components

import androidx.compose.ui.graphics.Color

/** Shared palette for player Rating text, badges and exported images. */
internal fun playerRatingColors(rating: Double): List<Color> = when {
    rating >= 16.0 -> listOf(Color(0xFFFF5E5E), Color(0xFFFFF75E), Color(0xFF5EFF5E), Color(0xFF5EBAFF), Color(0xFFBA5EFF))
    rating >= 15.25 -> listOf(Color.LightGray, Color.White, Color.LightGray)
    rating >= 14.5 -> listOf(Color(0xFFFFD700), Color(0xFFFFA500))
    else -> listOf(Color(0xFFFF9500), Color(0xFFFF9500))
}
