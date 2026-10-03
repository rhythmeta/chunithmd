package org.rhythmeta.chunithmd.ui.random

import org.rhythmeta.chunithmd.shared.localization.tr

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import org.rhythmeta.chunithmd.shared.CatalogBundle
import org.rhythmeta.chunithmd.shared.CatalogFilters
import org.rhythmeta.chunithmd.shared.CatalogSong
import org.rhythmeta.chunithmd.shared.RandomSongQuery
import org.rhythmeta.chunithmd.shared.ScoreRecord
import org.rhythmeta.chunithmd.ui.catalog.CatalogFilterDialog
import org.rhythmeta.chunithmd.ui.catalog.SongCard
import org.rhythmeta.chunithmd.ui.components.SquircleExtension
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@Stable
internal class RandomSongSessionState {
    var songCount by mutableIntStateOf(3)
    var filterSettings by mutableStateOf(CatalogFilters())
    var resultIds by mutableStateOf<List<String>>(emptyList())
}

@Composable
internal fun RandomSongScreen(
    bundle: CatalogBundle?,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    scores: List<ScoreRecord>,
    favoriteSongIds: Set<String>,
    playableRegion: String,
    sessionState: RandomSongSessionState,
    filterRequested: Boolean,
    onFilterRequestHandled: () -> Unit,
    onFilterActiveChanged: (Boolean) -> Unit,
    contentTopPadding: Dp,
    topBarScrollConnection: NestedScrollConnection,
    onOpenSong: (String) -> Unit,
) {
    val catalog = bundle?.catalog?.songs.orEmpty()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val scoresBySheetKey = remember(scores) {
        scores.groupBy(ScoreRecord::sheetKey)
            .mapValues { (_, records) -> records.maxByOrNull(ScoreRecord::score)!! }
    }
    val filteredSongs = remember(bundle, sessionState.filterSettings, playableRegion, favoriteSongIds) {
        bundle?.let {
            RandomSongQuery.filter(
                bundle = it,
                filters = sessionState.filterSettings,
                playableRegion = playableRegion,
                favoriteSongIds = favoriteSongIds,
            )
        }.orEmpty()
    }
    val results = remember(catalog, sessionState.resultIds) {
        val songsById = catalog.associateBy(CatalogSong::songId)
        sessionState.resultIds.mapNotNull(songsById::get)
    }
    val slotOffsets = remember { List(4) { Animatable(0f) } }
    val resultsListState = rememberLazyListState()
    var displayedColumns by remember {
        val songsById = catalog.associateBy(CatalogSong::songId)
        val restored = sessionState.resultIds.mapNotNull(songsById::get)
        mutableStateOf(List(4) { index -> restored.getOrNull(index)?.let { listOf(it) }.orEmpty() })
    }
    var isSpinning by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }
    var spinJob by remember { mutableStateOf<Job?>(null) }
    var animationJobs by remember { mutableStateOf(emptyList<Job>()) }

    val filterActive = sessionState.filterSettings.categories.isNotEmpty() ||
        sessionState.filterSettings.versions.isNotEmpty() ||
        sessionState.filterSettings.difficulties.isNotEmpty() ||
        sessionState.filterSettings.types.isNotEmpty() ||
        sessionState.filterSettings.minLevel != 1.0 ||
        sessionState.filterSettings.maxLevel != 16.0 ||
        sessionState.filterSettings.playableOnly ||
        sessionState.filterSettings.hideDeleted ||
        sessionState.filterSettings.favoritesOnly
    LaunchedEffect(filterRequested) {
        if (filterRequested) {
            showFilter = true
            onFilterRequestHandled()
        }
    }
    LaunchedEffect(filterActive) { onFilterActiveChanged(filterActive) }

    fun finishSpin(pending: List<CatalogSong>) {
        sessionState.resultIds = pending.map(CatalogSong::songId)
        isSpinning = false
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun skipSpin() {
        val pending = displayedColumns.take(sessionState.songCount).mapNotNull { it.lastOrNull() }
        spinJob?.cancel()
        animationJobs.forEach(Job::cancel)
        animationJobs = listOf(scope.launch {
            val itemHeight = with(density) { slotHeight(sessionState.songCount).toPx() }
            slotOffsets.take(sessionState.songCount).forEachIndexed { index, offset ->
                offset.snapTo(-(displayedColumns[index].lastIndex * itemHeight))
            }
            finishSpin(pending)
        })
    }

    fun spin() {
        val pending = RandomSongQuery.draw(filteredSongs, sessionState.songCount)
        if (pending.isEmpty()) return
        spinJob?.cancel()
        animationJobs.forEach(Job::cancel)
        sessionState.resultIds = emptyList()
        isSpinning = true
        val columns = List(4) { index ->
            if (index >= sessionState.songCount) emptyList()
            else List(SlotFillerCount) { catalog.random() } + pending[index]
        }
        displayedColumns = columns
        val itemHeight = with(density) { slotHeight(sessionState.songCount).toPx() }
        animationJobs = slotOffsets.take(sessionState.songCount).mapIndexed { index, offset ->
            scope.launch {
                offset.snapTo(0f)
                offset.animateTo(
                    targetValue = -(columns[index].lastIndex * itemHeight),
                    animationSpec = tween(
                        durationMillis = BaseSpinDuration + index * ColumnDelay,
                        easing = SlotEasing,
                    ),
                )
            }
        }
        spinJob = scope.launch {
            delay((BaseSpinDuration + (sessionState.songCount - 1) * ColumnDelay).toLong().milliseconds)
            finishSpin(pending)
        }
    }

    if (bundle == null || catalog.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(tr("需要先下载歌曲目录"), color = MiuixTheme.colorScheme.onBackgroundVariant)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentTopPadding)
                .nestedScroll(topBarScrollConnection),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                TabRowWithContour(
                    tabs = listOf(tr("一次 3 首"), tr("一次 4 首")),
                    selectedTabIndex = sessionState.songCount - 3,
                    onTabSelected = { index ->
                        val nextCount = index + 3
                        if (nextCount != sessionState.songCount) {
                            animationJobs.forEach(Job::cancel)
                            spinJob?.cancel()
                            isSpinning = false
                            displayedColumns = List(4) { emptyList() }
                            sessionState.resultIds = emptyList()
                            sessionState.songCount = nextCount
                            scope.launch { slotOffsets.forEach { it.snapTo(0f) } }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.72f),
                    minWidth = 96.dp,
                    maxWidth = 144.dp,
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                    insideMargin = PaddingValues(12.dp),
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(slotHeight(sessionState.songCount)),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        repeat(sessionState.songCount) { index ->
                            RandomSlotColumn(
                                songs = displayedColumns[index],
                                offset = slotOffsets[index].value,
                                slotHeight = slotHeight(sessionState.songCount),
                                jacketSize = if (sessionState.songCount == 4) 64.dp else 78.dp,
                                jacketBaseUrl = jacketBaseUrl,
                                localJacketPath = localJacketPath,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                Button(
                    onClick = { if (isSpinning) skipSpin() else spin() },
                    modifier = Modifier.fillMaxWidth(0.78f),
                    enabled = filteredSongs.isNotEmpty(),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Icon(
                        imageVector = if (isSpinning) Icons.Rounded.FastForward else Icons.Rounded.Casino,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(if (isSpinning) tr("直接跳过") else tr("立刻随机抽取"))
                }
            }
            AnimatedVisibility(
                visible = !isSpinning && results.isNotEmpty(),
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 },
                exit = fadeOut(tween(180)),
                modifier = Modifier.weight(1f),
            ) {
                LazyColumn(
                    state = resultsListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text(
                            text = tr("抽选结果"),
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                        )
                    }
                    itemsIndexed(
                        items = results,
                        key = { index, song -> "$index-${song.songId}" },
                    ) { _, song ->
                        SongCard(
                            song = song,
                            jacketBaseUrl = jacketBaseUrl,
                            localJacketPath = localJacketPath,
                            scoresBySheetKey = scoresBySheetKey,
                            onClick = { onOpenSong(song.songId) },
                        )
                    }
                }
            }
            if (results.isEmpty() || isSpinning) Spacer(Modifier.weight(1f))
        }
    }

    if (bundle != null) {
        CatalogFilterDialog(
            show = showFilter,
            bundle = bundle,
            settings = sessionState.filterSettings,
            onSettingsChange = {
                sessionState.filterSettings = it
            },
            onDismiss = { showFilter = false },
        )
    }
}

@Composable
private fun RandomSlotColumn(
    songs: List<CatalogSong>,
    offset: Float,
    slotHeight: Dp,
    jacketSize: Dp,
    jacketBaseUrl: String,
    localJacketPath: (String) -> String?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(slotHeight).clipToBounds(),
        cornerRadius = 14.dp,
        insideMargin = PaddingValues(0.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.035f)),
    ) {
        if (songs.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().height(slotHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Help,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                )
                Text(tr("准备好了吗？"), style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.34f))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(align = Alignment.Top, unbounded = true)
                    .offset { IntOffset(x = 0, y = offset.roundToInt()) },
            ) {
                songs.forEach { song ->
                    Box(
                        modifier = Modifier.fillMaxWidth().height(slotHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        val model = localJacketPath(song.imageName)?.let(::File)
                            ?: (jacketBaseUrl.trimEnd('/') + "/" + song.imageName.trimStart('/'))
                        coil.compose.AsyncImage(
                            model = model,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .size(jacketSize)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun RandomSongHomeCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .height(140.dp)
            .squircleBorder(
                width = 1.dp,
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                cornerRadius = 16.dp,
                extension = SquircleExtension,
            ),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(16.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
        onClick = onClick,
    ) {
        Icon(
            imageVector = Icons.Rounded.Casino,
            contentDescription = null,
            modifier = Modifier.size(30.dp),
            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                tr("随机歌曲"),
                style = MiuixTheme.textStyles.body1,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                maxLines = 2,
            )
            Text(
                tr("老虎机式随机抽曲"),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 2,
            )
        }
    }
}

private fun slotHeight(songCount: Int): Dp = if (songCount == 4) 92.dp else 112.dp

private const val SlotFillerCount = 20
private const val BaseSpinDuration = 2_000
private const val ColumnDelay = 400
private val SlotEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
