package org.rhythmeta.chunithmd.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import org.rhythmeta.chunithmd.shared.CatalogSongFormatter
import org.rhythmeta.chunithmd.shared.CatalogVersionFormatter
import org.rhythmeta.chunithmd.shared.CatalogSong
import org.rhythmeta.chunithmd.shared.VersionPalette
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SongCard(
    song: CatalogSong,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
) {
    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    val hasWorldsEnd = song.sheets.any { it.type.equals("we", ignoreCase = true) }
    val accentColor = if (hasWorldsEnd) {
        difficultyColor("world's end")
    } else {
        song.sheets.maxByOrNull { difficultyOrder(it.difficulty) }?.let { difficultyColor(it.difficulty) }
            ?: difficultyColor("world's end")
    }
    val palette = VersionPalette.forVersion(song.version, isDark)
    val badgeBackground = if (isDark) palette.darkBackground else palette.lightBackground
    val badgeForeground = if (isDark) palette.darkForeground else palette.lightForeground

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .squircleSurface(
                color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = if (isDark) 0.82f else 0.88f),
                cornerRadius = 14.dp,
            )
            .squircleBorder(width = 1.dp, color = accentColor.copy(alpha = 0.12f), cornerRadius = 14.dp)
            .padding(vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxHeight()
                .width(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .let { barModifier ->
                    if (hasWorldsEnd) {
                        barModifier.background(Brush.verticalGradient(WORLDS_END_GRADIENT_COLORS))
                    } else {
                        barModifier.squircleSurface(color = accentColor, cornerRadius = 2.dp)
                    }
                },
        )
        Row(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 10.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = localJacketPath(song.imageName)?.let(::File)
                    ?: jacketBaseUrl.trimEnd('/') + "/" + song.imageName.trimStart('/'),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(MiuixTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                MiuixText(
                    text = CatalogSongFormatter.displayTitle(song),
                    style = MiuixTheme.textStyles.body1.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.fillMaxWidth().height(20.dp).basicMarquee(),
                )
                MiuixText(
                    text = song.artist.ifBlank { "未知艺术家" },
                    style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.fillMaxWidth().height(16.dp).basicMarquee(),
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MiuixText(
                    text = CatalogVersionFormatter.badge(song.version),
                    modifier = Modifier.squircleSurface(color = Color(badgeBackground.toInt()), cornerRadius = 4.dp).padding(horizontal = 7.dp, vertical = 3.dp),
                    color = Color(badgeForeground.toInt()),
                    style = MiuixTheme.textStyles.footnote1.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DIFFICULTIES.forEachIndexed { index, difficulty ->
                        val available = song.sheets.any {
                            it.regions["jp"] == true && if (difficulty.equals("world's end", true)) {
                                it.type.equals("we", true)
                            } else {
                                it.difficulty.equals(difficulty, true)
                            }
                        }
                        Box(
                            Modifier
                                .padding(start = if (index == 0) 0.dp else 3.dp)
                                .size(7.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (available) difficultyColor(difficulty) else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.18f)),
                        )
                    }
                }
            }
        }
    }
}


internal val DIFFICULTIES = listOf("basic", "advanced", "expert", "master", "ultima", "world's end")

private fun difficultyOrder(value: String): Int = DIFFICULTIES.indexOfFirst { it.equals(value, true) }

private fun difficultyColor(value: String): Color = when (value.lowercase()) {
    "basic" -> Color(0xFF65B94A)
    "advanced" -> Color(0xFFE6BD31)
    "expert" -> Color(0xFFE34A47)
    "master" -> Color(0xFF9A50C9)
    "ultima" -> Color(0xFF222222)
    else -> Color(0xFF4AA8C2)
}

private val WORLDS_END_GRADIENT_COLORS = listOf(
    Color(0xFF65B94A),
    Color(0xFFE6BD31),
    Color(0xFFE34A47),
    Color(0xFF9A50C9),
    Color(0xFF5D5D66),
    Color(0xFF4AA8C2),
)
