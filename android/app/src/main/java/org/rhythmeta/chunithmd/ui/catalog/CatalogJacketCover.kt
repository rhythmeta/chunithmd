package org.rhythmeta.chunithmd.ui.catalog

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import org.rhythmeta.chunithmd.ui.components.squircleShape

/**
 * Geometry and shadow of the song detail jacket, shared by the resting hero and the cover that
 * flies between the catalog and the detail page.
 *
 * The jacket uses a drawn drop shadow ([Shadow]) instead of a platform elevation shadow so the
 * shadow can travel with the shared cover: [CatalogContainerTransform] paints the same [Shape] with
 * the same [Shadow] at the interpolated cover bounds, which makes the hand-off to the real jacket
 * pixel-exact instead of popping in on the transition's last frame.
 *
 * The parameters follow the iOS app (`ios/chunithmd/Views/Song/SongDetailHero.swift`), toned down
 * to the Android surface and palette.
 */
internal object CatalogJacketCover {
    val Size = 220.dp
    val CornerRadius = 26.dp
    val Shape: Shape = squircleShape(CornerRadius)

    private val ShadowRadius = 24.dp
    private val ShadowOffsetY = 6.dp

    val Shadow: Shadow = Shadow(
        radius = ShadowRadius,
        color = Color.Black,
        alpha = 0.15f,
        offset = DpOffset(0.dp, ShadowOffsetY),
    )

}
