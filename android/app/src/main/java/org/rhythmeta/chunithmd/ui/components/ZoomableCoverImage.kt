package org.rhythmeta.chunithmd.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

@Composable
internal fun ZoomableCoverImage(
    model: Any?,
    imageSize: Int,
    animateCoverChanges: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val image = remember(context, model, imageSize) {
        ImageRequest.Builder(context).data(model).size(imageSize).crossfade(false).build()
    }
    val imagePainter = rememberAsyncImagePainter(image)
    var displayedCover by remember { mutableStateOf<Painter?>(null) }
    val loadedCover = (imagePainter.state as? AsyncImagePainter.State.Success)?.painter
    LaunchedEffect(loadedCover) {
        // Keep the old cover while loading; only zoom transitions animate its replacement.
        if (loadedCover != null) displayedCover = loadedCover
    }
    Crossfade(targetState = displayedCover,
        animationSpec = if (animateCoverChanges) tween(180) else snap(),
        modifier = modifier, label = "coverReplacement") { cover ->
        if (cover != null) {
            Image(cover, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize())
        }
    }
}
