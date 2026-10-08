package org.rhythmeta.chunithmd.scanner

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import org.rhythmeta.chunithmd.shared.CatalogBundle
import org.rhythmeta.chunithmd.shared.scanner.ScanSongMatch
import org.rhythmeta.chunithmd.ui.components.SquircleExtension
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ScannerSongCard(
    match: ScanSongMatch,
    catalog: CatalogBundle?,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    onClick: () -> Unit,
) {
    val song = catalog?.catalog?.songs?.firstOrNull { it.songId == match.songId } ?: return
    Row(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
            .squircleSurface(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = .94f), 16.dp, SquircleExtension)
            .squircleBorder(1.dp, MiuixTheme.colorScheme.onSurface.copy(alpha = .1f), 16.dp, SquircleExtension)
            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = localJacketPath(song.imageName)?.let(::File)
                ?: (jacketBaseUrl.trimEnd('/') + "/" + song.imageName.trimStart('/')),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(40.dp).squircleSurface(Color.Transparent, 8.dp, SquircleExtension),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                song.title, maxLines = 1, fontWeight = FontWeight.Bold,
                style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp, lineHeight = 14.sp),
                modifier = Modifier.fillMaxWidth().basicMarquee(),
            )
            Text(
                song.artist, maxLines = 1,
                style = MiuixTheme.textStyles.body2.copy(fontSize = 10.sp, lineHeight = 12.sp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.basicMarquee(),
            )
        }
        Icon(
            Icons.Rounded.ChevronRight, null,
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = .4f),
            modifier = Modifier.padding(start = 4.dp, end = 16.dp),
        )
    }
}
