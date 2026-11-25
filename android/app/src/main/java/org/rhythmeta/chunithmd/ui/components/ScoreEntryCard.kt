package org.rhythmeta.chunithmd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import java.util.Locale
import org.rhythmeta.chunithmd.shared.localization.tr
import org.rhythmeta.chunithmd.ui.catalog.SongCardTransitionContent
import org.rhythmeta.chunithmd.ui.catalog.difficultyColor
import org.rhythmeta.chunithmd.ui.catalog.rememberSongCoverNavigation
import org.rhythmeta.chunithmd.ui.catalog.ultimaStripedBrush
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ScoreEntryCard(
    songId: String,
    chartKey: String,
    title: String,
    imageName: String,
    difficulty: String?,
    rank: String?,
    score: Int?,
    value: String,
    caption: String,
    valueBrush: Brush,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    onClick: () -> Unit,
    badges: @Composable () -> Unit,
) {
    val coverNavigation = rememberSongCoverNavigation(songId, onClick, 10.dp, key = chartKey, cardColor = MiuixTheme.colorScheme.surfaceContainer, cardCornerRadius = 16.dp)
    val accent = difficulty?.let(::difficultyColor) ?: MiuixTheme.colorScheme.onSurfaceVariantSummary
    val isUltima = difficulty.equals("ultima", ignoreCase = true)
    val imageModel = remember(imageName, jacketBaseUrl, localJacketPath) {
        if (imageName.isBlank()) return@remember null
        localJacketPath(imageName)?.let(::File)
            ?: jacketBaseUrl.trimEnd('/').takeIf { it.isNotBlank() }?.let { "$it/${imageName.trimStart('/')}" }
    }
    Card(
        modifier = Modifier.fillMaxWidth().then(coverNavigation.cardModifier).clip(RoundedCornerShape(16.dp)).clickable(onClick = coverNavigation::open),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(start = 8.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
    ) {
        SongCardTransitionContent(coverNavigation) { showCover ->
            Row(
                modifier = Modifier.fillMaxWidth().height(84.dp).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .height(52.dp)
                        .width(4.dp)
                        .then(
                            if (isUltima) {
                                Modifier.clip(RoundedCornerShape(2.dp)).background(ultimaStripedBrush())
                            } else {
                                Modifier.squircleSurface(
                                    color = accent,
                                    cornerRadius = 2.dp,
                                    extension = SquircleExtension,
                                )
                            },
                        ),
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.size(56.dp).then(if (showCover) coverNavigation.modifier else Modifier).squircleSurface(
                        color = if (showCover) MiuixTheme.colorScheme.onSurface.copy(alpha = 0.05f) else Color.Transparent,
                        cornerRadius = 10.dp,
                        extension = SquircleExtension,
                    ),
                ) {
                    if (showCover) {
                        if (imageModel == null) {
                            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = accent, modifier = Modifier.align(Alignment.Center))
                        } else {
                            AsyncImage(imageModel, placeholder = coverNavigation.source.painter, error = coverNavigation.source.painter, onSuccess = { coverNavigation.onPainter(it.painter) }, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().fadingMarquee())
                    if (score != null && rank != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(rank, style = MiuixTheme.textStyles.footnote1, fontWeight = FontWeight.Black,
                                color = rankColor(rank).takeUnless { it == Color.Unspecified } ?: MiuixTheme.colorScheme.onSurface)
                            Text(String.format(Locale.ROOT, "%,d", score), maxLines = 1,
                                style = MiuixTheme.textStyles.footnote1.copy(fontFamily = FontFamily.Monospace),
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        }
                    } else {
                        Text(tr("未记录"), style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { badges() }
                }
                Column(modifier = Modifier.padding(end = 14.dp), horizontalAlignment = Alignment.End) {
                    Text(value, style = MiuixTheme.textStyles.title3.copy(fontWeight = FontWeight.Black, brush = valueBrush),
                        color = Color.Unspecified, maxLines = 1)
                    Text(caption, style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            }
        }
    }
}

@Composable
internal fun ScoreEntryBadge(text: String, color: Color) {
    Text(
        text,
        style = MiuixTheme.textStyles.footnote2.copy(fontSize = 9.sp),
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        color = color,
        modifier = Modifier.squircleSurface(color.copy(alpha = 0.13f), 4.dp, SquircleExtension).padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

internal fun rankColor(rank: String): Color = when (rank.uppercase(Locale.ROOT)) {
    "SSS+", "SSS" -> Color(0xFFFFD900)
    "SS+", "SS" -> Color(0xFFFFBF00)
    "S+", "S" -> Color(0xFFFF9900)
    "AAA" -> Color(0xFFCC99FF)
    "AA" -> Color(0xFF99CCFF)
    "A" -> Color(0xFF80E680)
    else -> Color.Unspecified
}
