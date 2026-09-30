package org.rhythmeta.chunithmd.ui.recommendation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.rhythmeta.chunithmd.shared.BestTablePreferences
import org.rhythmeta.chunithmd.shared.CatalogBundle
import org.rhythmeta.chunithmd.shared.CatalogSongFormatter
import org.rhythmeta.chunithmd.shared.ChunithmScoreRules
import org.rhythmeta.chunithmd.shared.ProfileServer
import org.rhythmeta.chunithmd.shared.RecommendationCalculator
import org.rhythmeta.chunithmd.shared.RecommendationResponse
import org.rhythmeta.chunithmd.shared.RecommendationResult
import org.rhythmeta.chunithmd.shared.ScoreRecord
import org.rhythmeta.chunithmd.ui.catalog.difficultyColor
import org.rhythmeta.chunithmd.ui.catalog.ultimaStripedBrush
import org.rhythmeta.chunithmd.ui.components.SquircleExtension
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Lightbulb

@Composable
fun RecommendationScreen(
    bundle: CatalogBundle?,
    records: List<ScoreRecord>,
    activeServer: ProfileServer,
    preferences: BestTablePreferences,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    contentTopPadding: androidx.compose.ui.unit.Dp,
    topBarScrollConnection: NestedScrollConnection,
    switcherScrollConnection: NestedScrollConnection,
    selectedPage: Int,
    onOpenSong: (String) -> Unit,
) {
    val response by produceState<RecommendationResponse?>(
        null,
        bundle,
        records,
        activeServer,
        preferences,
    ) {
        value = null
        value = bundle?.let { catalog ->
            withContext(Dispatchers.Default) {
                RecommendationCalculator.calculate(
                    bundle = catalog,
                    records = records,
                    activeServer = activeServer,
                    bestSlotCount = preferences.bestCount,
                    newSlotCount = preferences.newCount,
                )
            }
        }
    }
    val listState = rememberLazyListState()

    if (bundle == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("需要先下载歌曲目录", color = MiuixTheme.colorScheme.onBackgroundVariant)
        }
        return
    }

    val loadedResponse = response
    if (loadedResponse == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val results = if (selectedPage == 0) loadedResponse.new else loadedResponse.old

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(topBarScrollConnection)
            .nestedScroll(switcherScrollConnection),
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, top = contentTopPadding + 10.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (results.isEmpty()) {
            item(key = "empty") {
                Column(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("暂时没有可吃分的谱面", style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("先录入成绩，或提高当前谱面的分数", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            }
        } else {
            items(results, key = RecommendationResult::chartId) { result ->
                RecommendationRow(
                    result = result,
                    jacketBaseUrl = jacketBaseUrl,
                    localJacketPath = localJacketPath,
                    onClick = { onOpenSong(result.song.songId) },
                )
            }
        }
    }
}

@Composable
private fun RecommendationRow(
    result: RecommendationResult,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    onClick: () -> Unit,
) {
    val accent = difficultyColor(result.sheet.difficulty)
    val isUltima = result.sheet.difficulty.equals("ultima", ignoreCase = true)
    val imageModel = remember(result.song.imageName, jacketBaseUrl) {
        localJacketPath(result.song.imageName)?.let(::File)
            ?: jacketBaseUrl.trimEnd('/').takeIf { it.isNotBlank() }?.let { "$it/${result.song.imageName.trimStart('/')}" }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(start = 8.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(84.dp).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.height(52.dp).width(4.dp).then(
                    if (isUltima) Modifier.clip(RoundedCornerShape(2.dp)).background(ultimaStripedBrush())
                    else Modifier.squircleSurface(accent, 2.dp, SquircleExtension)
                ),
            )
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(56.dp).squircleSurface(
                    color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    cornerRadius = 10.dp,
                    extension = SquircleExtension,
                ),
            ) {
                if (imageModel == null) {
                    Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = accent, modifier = Modifier.align(Alignment.Center))
                } else {
                    AsyncImage(imageModel, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                val currentRank = result.currentScore?.let(ChunithmScoreRules::rank)
                Text(
                    CatalogSongFormatter.displayTitle(result.song),
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.fillMaxWidth().basicMarquee(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (currentRank == null) {
                        Text("未游玩", style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    } else {
                        Text(
                            currentRank,
                            style = MiuixTheme.textStyles.footnote1,
                            fontWeight = FontWeight.Black,
                            color = rankColor(currentRank),
                        )
                        Text(
                            formatScore(result.currentScore!!),
                            style = MiuixTheme.textStyles.footnote2.copy(fontFamily = FontFamily.Monospace),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
                Text(
                    formatLevel(result.constant),
                    style = MiuixTheme.textStyles.footnote2.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = accent,
                    modifier = Modifier
                        .squircleSurface(accent.copy(alpha = 0.13f), 4.dp, SquircleExtension)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
            Column(modifier = Modifier.padding(end = 14.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "+${formatRating(result.potentialGain)}",
                    style = MiuixTheme.textStyles.title3,
                    fontWeight = FontWeight.Black,
                    color = RecommendationAccent,
                )
                Text(
                    "目标 ${result.targetRank}",
                    style = MiuixTheme.textStyles.footnote2.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

@Composable
fun RecommendationPageSwitcher(
    selectedPage: Int,
    visible: Boolean,
    onSelectedPageChange: (Int) -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
    ) {
        TabRowWithContour(
            tabs = listOf("新曲推荐", "旧曲推荐"),
            selectedTabIndex = selectedPage,
            onTabSelected = onSelectedPageChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            minWidth = 112.dp,
            maxWidth = 180.dp,
        )
    }
}

@Composable
fun RecommendationHomeCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.height(140.dp),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(16.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
        onClick = onClick,
    ) {
        Icon(Icons.AutoMirrored.Rounded.TrendingUp, contentDescription = null, modifier = Modifier.size(30.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantActions)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("吃分推荐", style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold)
            Text("定数拟合分析", style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 2)
        }
    }
}

private fun formatScore(value: Int): String = String.format(Locale.ROOT, "%,d", value)
private fun formatLevel(value: Double): String = String.format(Locale.ROOT, "%.1f", value)
private fun formatRating(value: Double): String = String.format(Locale.ROOT, "%.2f", value)

@Composable
private fun rankColor(rank: String): Color = when (rank.uppercase(Locale.ROOT)) {
    "SSS+", "SSS" -> Color(0xFFFFD900)
    "SS+", "SS" -> Color(0xFFFFBF00)
    "S+", "S" -> Color(0xFFFF9900)
    "AAA" -> Color(0xFFCC99FF)
    "AA" -> Color(0xFF99CCFF)
    "A" -> Color(0xFF80E680)
    else -> MiuixTheme.colorScheme.onSurface
}

private val RecommendationAccent = Color(0xFFFF9500)
