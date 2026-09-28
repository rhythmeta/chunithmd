package org.rhythmeta.chunithmd

import android.os.Bundle
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.unit.Density
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import androidx.compose.runtime.withFrameNanos
import kotlinx.serialization.Serializable
import org.rhythmeta.chunithmd.shared.CatalogBundle
import org.rhythmeta.chunithmd.shared.CatalogFilters
import org.rhythmeta.chunithmd.shared.CatalogJson
import org.rhythmeta.chunithmd.shared.CatalogQuery
import org.rhythmeta.chunithmd.shared.CatalogRepository
import org.rhythmeta.chunithmd.shared.CatalogSong
import org.rhythmeta.chunithmd.shared.CatalogSort
import org.rhythmeta.chunithmd.shared.CatalogSyncStage
import org.rhythmeta.chunithmd.shared.CatalogSyncState
import org.rhythmeta.chunithmd.shared.VersionPalette
import org.rhythmeta.chunithmd.ui.theme.ChunithmdTheme
import org.rhythmeta.chunithmd.ui.theme.AppThemeSettings
import org.rhythmeta.chunithmd.ui.theme.DefaultAppThemeSettings
import org.rhythmeta.chunithmd.ui.theme.ThemePreferencesRepository
import org.rhythmeta.chunithmd.ui.theme.LocalEnableFloatingBottomBar
import org.rhythmeta.chunithmd.ui.theme.LocalEnableFloatingBottomBarBlur
import org.rhythmeta.chunithmd.ui.theme.LocalEnableBlur
import org.rhythmeta.chunithmd.ui.theme.LocalEnablePredictiveBack
import org.rhythmeta.chunithmd.ui.components.LiquidGlassTab
import org.rhythmeta.chunithmd.ui.components.LiquidGlassTabBar
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import org.rhythmeta.chunithmd.ui.settings.ThemeSettingsScreen
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.window.WindowListPopup

import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransitions

@Serializable
private sealed interface AppRoute : NavKey {
    @Serializable data object Home : AppRoute
    @Serializable data object Scan : AppRoute
    @Serializable data object Catalog : AppRoute
    @Serializable data object Settings : AppRoute
    @Serializable data object Theme : AppRoute
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        window.isNavigationBarContrastEnforced = false
        val repository = CatalogRepository(filesDir.absolutePath)
        val themeRepository = ThemePreferencesRepository(applicationContext)
        setContent {
            val themeSettings by produceState(DefaultAppThemeSettings, themeRepository) {
                themeRepository.settings.collect { value = it }
            }
            ChunithmdTheme(themeSettings) {
                val baseDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(baseDensity.density * themeSettings.pageScale, baseDensity.fontScale),
                ) {
                    CatalogApp(repository, themeRepository, themeSettings)
                }
            }
        }
    }
}

@Composable
private fun CatalogApp(
    repository: CatalogRepository,
    themeRepository: ThemePreferencesRepository,
    themeSettings: AppThemeSettings,
) {
    var bundle by remember { mutableStateOf<CatalogBundle?>(null) }
    var manifest by remember { mutableStateOf<org.rhythmeta.chunithmd.shared.StaticManifest?>(null) }
    var sync by remember { mutableStateOf(CatalogSyncState()) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var animateRootTransition by remember { mutableStateOf(true) }
    val rootBackProgress = remember { Animatable(0f) }
    var search by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(CatalogSort.Default) }
    var ascending by remember { mutableStateOf(true) }
    var filters by remember { mutableStateOf(CatalogFilters()) }
    var filterOpen by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // Miuix's surface is the page canvas used by the reference navigation shell.
    val pageBackground = MiuixTheme.colorScheme.surface
    val navigationBackdrop = rememberLayerBackdrop {
        drawRect(pageBackground)
        drawContent()
    }
    val enableBlur = LocalEnableBlur.current
    val topBarScrollBehavior = MiuixScrollBehavior()
    var searchVisible by remember { mutableStateOf(true) }
    val searchScrollConnection = remember {
        object : NestedScrollConnection {
            private var downDistance = 0f
            private var upDistance = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                when {
                    available.y < 0f -> {
                        downDistance = 0f
                        upDistance -= available.y
                        if (upDistance >= 36f) {
                            searchVisible = false
                            upDistance = 0f
                        }
                    }
                    available.y > 0f -> {
                        upDistance = 0f
                        downDistance += available.y
                        if (downDistance >= 24f) {
                            searchVisible = true
                            downDistance = 0f
                        }
                    }
                }
                return Offset.Zero
            }
        }
    }

    fun refresh() {
        scope.launch {
            error = null
            runCatching {
                repository.downloadAndApply { state -> sync = state }
            }.onSuccess { snapshot ->
                manifest = snapshot.manifest
                bundle = CatalogJson.decodeBundle(snapshot.bundleJson)
            }.onFailure {
                error = it.message ?: "资源同步失败"
                sync = CatalogSyncState(CatalogSyncStage.Failed, error)
            }
        }
    }

    LaunchedEffect(Unit) {
        runCatching { repository.loadLocal() }.getOrNull()?.let { snapshot ->
            manifest = snapshot.manifest
            bundle = CatalogJson.decodeBundle(snapshot.bundleJson)
            scope.launch {
                runCatching { repository.checkForUpdate() }.onSuccess { check ->
                    if (check.updateAvailable) sync = CatalogSyncState(CatalogSyncStage.Idle, "发现可用更新")
                }
            }
        }
        if (bundle == null) refresh()
    }

    val isDark = isSystemInDarkTheme()
    val songs = remember(bundle, search, sort, ascending, filters) {
        bundle?.let { CatalogQuery.filterAndSort(it, search, sort, ascending, filters) }.orEmpty()
    }
    val filterActive = filters.categories.isNotEmpty() || filters.versions.isNotEmpty() ||
        filters.difficulties.isNotEmpty() || filters.types.isNotEmpty() || filters.playableOnly
    // Keep the detail page in the same navigation state as the root pages so its
    // enter/exit transition is observable and back can return to Settings.
    val navBackStack = rememberNavBackStack<AppRoute>(AppRoute.Home)
    fun navigateToTab(index: Int) {
        animateRootTransition = true
        selectedTab = index
    }

    fun returnToHomeWithoutSecondTransition() {
        animateRootTransition = false
        selectedTab = 0
    }

    val rootBackEnabled = navBackStack.lastOrNull() == AppRoute.Home && selectedTab != 0
    if (LocalEnablePredictiveBack.current) {
        PredictiveBackHandler(enabled = rootBackEnabled) { progress: Flow<BackEventCompat> ->
            try {
                progress.collect { event ->
                    rootBackProgress.snapTo(event.progress.coerceIn(0f, 1f))
                }
                rootBackProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                )
                returnToHomeWithoutSecondTransition()
                rootBackProgress.snapTo(0f)
                withFrameNanos { }
                animateRootTransition = true
            } catch (cancelled: CancellationException) {
                withContext(NonCancellable) {
                    rootBackProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = 1f, stiffness = 146f),
                    )
                }
                throw cancelled
            }
        }
    } else {
        BackHandler(enabled = rootBackEnabled) {
            returnToHomeWithoutSecondTransition()
        }
    }

    @Composable
    fun AppFrame(page: Int, content: @Composable (PaddingValues) -> Unit) {
        MiuixScaffold(
            // Keep the scaffold's underlay identical to the page. The floating bar is
            // positioned above it; a transparent scaffold would expose the window's default
            // color as a full-width strip beneath the bar.
            containerColor = MiuixTheme.colorScheme.surface,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                MiuixTopAppBar(
                    title = when (page) { 4 -> "主题"; 3 -> "设置"; 0 -> "主页"; 1 -> "扫描"; else -> "歌曲" },
                    largeTitle = when (page) { 4 -> "主题"; 3 -> "设置"; 0 -> "主页"; 1 -> "扫描"; else -> "歌曲" },
                    navigationIcon = {
                        if (page == 4) MiuixIconButton(onClick = { navBackStack.removeLastOrNull() }) {
                            MiuixIcon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                        }
                    },
                    modifier = Modifier.drawPlainBackdrop(
                        backdrop = navigationBackdrop,
                        shape = { RoundedCornerShape(0.dp) },
                        effects = { if (enableBlur) blur(24.dp.toPx()) },
                        onDrawSurface = { drawRect(pageBackground.copy(alpha = if (enableBlur) 0.72f else 1f)) },
                    ),
                    actions = {
                        if (page == 2 && bundle != null) {
                            MiuixIconButton(onClick = {}) { MiuixIcon(Icons.Rounded.GridView, contentDescription = "网格视图") }
                            SortAction(sortOpen, sort, ascending, { sortOpen = !sortOpen }, { sort = it; sortOpen = false }, { ascending = !ascending; sortOpen = false })
                            MiuixIconButton(onClick = { filterOpen = true }) {
                                MiuixIcon(Icons.Rounded.FilterList, contentDescription = "筛选", tint = if (filterActive) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface)
                            }
                        }
                    },
                    bottomContent = {
                        if (page == 2 && bundle != null) AnimatedVisibility(visible = searchVisible) {
                            SearchField(search, onSearchChange = { search = it })
                        }
                    },
                    scrollBehavior = if (page == 2) topBarScrollBehavior else null,
                )
            },
        ) { padding -> content(padding) }
    }

    @Composable
    fun RootPage(page: Int, modifier: Modifier = Modifier) {
        Box(modifier) {
            AppFrame(page) { padding ->
            when (page) {
                0 -> BlankDestination(Modifier.padding(padding).fillMaxSize(), "主页")
                1 -> BlankDestination(Modifier.padding(padding).fillMaxSize(), "扫描")
                2 -> if (bundle == null) {
                    InitialLoad(sync, error, ::refresh, Modifier.padding(padding))
                } else {
                    SongList(
                        Modifier.padding(padding).fillMaxSize()
                            .background(MiuixTheme.colorScheme.surface)
                            .layerBackdrop(navigationBackdrop)
                            .nestedScroll(searchScrollConnection)
                            .nestedScroll(topBarScrollBehavior.nestedScrollConnection),
                        songs,
                        manifest?.assets?.jacketBaseUrl.orEmpty(),
                        isDark,
                    )
                }
                else -> SettingsHome(
                    Modifier.padding(padding).fillMaxSize()
                        .background(MiuixTheme.colorScheme.surface)
                        .layerBackdrop(navigationBackdrop),
                    { navBackStack.add(AppRoute.Theme) },
                ) {
                    ResourceSettings(Modifier, manifest, sync, error, {
                        scope.launch {
                            sync = CatalogSyncState(CatalogSyncStage.Checking)
                            runCatching { repository.checkForUpdate() }
                                .onSuccess { check ->
                                    sync = CatalogSyncState(
                                        CatalogSyncStage.Idle,
                                        if (check.updateAvailable) "发现可用更新" else "已是最新版本",
                                    )
                                }
                                .onFailure {
                                    error = it.message
                                    sync = CatalogSyncState(CatalogSyncStage.Failed, error)
                                }
                        }
                    }, ::refresh)
                }
            }
        }
    }
    }

    NavDisplay(
        backStack = navBackStack,
        modifier = Modifier.fillMaxSize(),
        transition = NavTransitions.MiuixDefault,
        effects = NavDisplayEffects(cornerClipRadius = rememberNavSystemCornerRadius()),
        onBack = {
            if (navBackStack.lastOrNull() == AppRoute.Theme) {
                navBackStack.removeLastOrNull()
            } else if (navBackStack.size > 1) {
                navBackStack.removeLastOrNull()
                selectedTab = 0
            }
        },
    ) {
        entry<AppRoute.Theme>(swipeDismiss = NavSwipeDirection.LeftToRight) {
            AppFrame(4) { padding ->
                Box(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.surface)) {
                    ThemeSettingsScreen(themeSettings, padding.calculateTopPadding(), { scope.launch { themeRepository.setColorMode(it) } }, { scope.launch { themeRepository.setKeyColor(it) } }, { scope.launch { themeRepository.setPaletteStyle(it) } }, { scope.launch { themeRepository.setColorSpec(it) } }, { scope.launch { themeRepository.setBlur(it) } }, { scope.launch { themeRepository.setFloatingBar(it) } }, { scope.launch { themeRepository.setFloatingBarBlur(it) } }, { scope.launch { themeRepository.setPredictiveBack(it) } }, { scope.launch { themeRepository.setPageScale(it) } })
                }
            }
        }
        entry<AppRoute.Home> {
            Box(
                Modifier.fillMaxSize()
                    .background(MiuixTheme.colorScheme.surface),
            ) {
                if (selectedTab != 0 && navBackStack.lastOrNull() == AppRoute.Home) {
                    RootPage(
                        0,
                        Modifier.fillMaxSize().graphicsLayer {
                            // Keep the home layer beside the active root page. It only
                            // enters the viewport as the predictive-back gesture advances.
                            translationX = -size.width * (1f - rootBackProgress.value)
                        },
                    )
                }
                AnimatedContent(
                    targetState = selectedTab,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        translationX = size.width * rootBackProgress.value
                    },
                    transitionSpec = {
                        if (!animateRootTransition) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                        val animationSpec = tween<IntOffset>(
                            durationMillis = 360,
                            easing = FastOutSlowInEasing,
                        )
                        if (targetState > initialState) {
                            slideInHorizontally(animationSpec) { it } togetherWith
                                slideOutHorizontally(animationSpec) { -it }
                        } else {
                            slideInHorizontally(animationSpec) { -it } togetherWith
                                slideOutHorizontally(animationSpec) { it }
                        }
                        }
                    },
                    label = "root-page-transition",
                ) { page -> RootPage(page) }

                AppNavigationBar(
                    selectedTab = selectedTab,
                    backdrop = navigationBackdrop,
                    onSelected = { index -> if (index != selectedTab) navigateToTab(index) },
                    blurEnabled = LocalEnableFloatingBottomBarBlur.current,
                    floating = LocalEnableFloatingBottomBar.current,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }

    if (filterOpen && bundle != null) {
        FilterDialog(bundle!!, filters, onApply = { filters = it; filterOpen = false }, onDismiss = { filterOpen = false })
    }

}

@Composable
private fun SearchField(search: String, onSearchChange: (String) -> Unit) {
    SearchBar(
        inputField = {
            InputField(
                query = search,
                onQueryChange = onSearchChange,
                onSearch = {},
                expanded = false,
                onExpandedChange = {},
                label = "歌曲、艺术家、别名...",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { MiuixIcon(Icons.Rounded.Search, contentDescription = null) },
            )
        },
        onExpandedChange = {},
        insideMargin = DpSize(width = 16.dp, height = 10.dp),
        expanded = false,
        content = {},
    )
}

@Composable
private fun SortAction(
    expanded: Boolean,
    sort: CatalogSort,
    ascending: Boolean,
    onToggle: () -> Unit,
    onSort: (CatalogSort) -> Unit,
    onAscending: () -> Unit,
) {
    Box {
        MiuixIconButton(onClick = onToggle) {
            MiuixIcon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "排序")
        }
        WindowListPopup(
            show = expanded,
            alignment = PopupPositionProvider.Align.End,
            enableWindowDim = true,
            onDismissRequest = onToggle,
        ) {
            ListPopupColumn {
                CatalogSort.entries.forEachIndexed { index, option ->
                    DropdownImpl(
                        text = sortLabel(option),
                        optionSize = CatalogSort.entries.size,
                        isSelected = option == sort,
                        index = index,
                        onSelectedIndexChange = { onSort(option) },
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClick = onAscending)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiuixText(if (ascending) "↑" else "↓", style = MiuixTheme.textStyles.title3, color = MiuixTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    MiuixText(if (ascending) "升序" else "降序", style = MiuixTheme.textStyles.body1)
                }
            }
        }
    }
}

@Composable
private fun SongList(modifier: Modifier, songs: List<CatalogSong>, jacketBaseUrl: String, isDark: Boolean) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(songs, key = CatalogSong::songId) { song ->
            SongCard(song, jacketBaseUrl, isDark)
        }
    }
}

@Composable
private fun SongCard(song: CatalogSong, jacketBaseUrl: String, isDark: Boolean) {
    val accentColor = song.sheets.maxByOrNull { difficultyOrder(it.difficulty) }?.let { difficultyColor(it.difficulty) }
        ?: difficultyColor("world's end")
    val palette = VersionPalette.forVersion(song.version, isDark)
    val badgeBackground = if (isDark) palette.darkBackground else palette.lightBackground
    val badgeForeground = if (isDark) palette.darkForeground else palette.lightForeground

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .squircleSurface(
                color = if (isDark) Color(0xFF111612) else Color(0xFFF8FBF9),
                cornerRadius = 14.dp,
            )
            .squircleBorder(width = 1.dp, color = accentColor.copy(alpha = if (isDark) 0.16f else 0.12f), cornerRadius = 14.dp)
            .padding(vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxHeight()
                .width(4.dp)
                .squircleSurface(color = accentColor, cornerRadius = 2.dp),
        )
        Row(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 10.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = jacketBaseUrl.trimEnd('/') + "/" + song.imageName.trimStart('/'),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(MiuixTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                MiuixText(
                    text = song.title,
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
                    text = versionLabel(song.version),
                    modifier = Modifier.squircleSurface(color = Color(badgeBackground.toInt()), cornerRadius = 4.dp).padding(horizontal = 7.dp, vertical = 3.dp),
                    color = Color(badgeForeground.toInt()),
                    style = MiuixTheme.textStyles.footnote1.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DIFFICULTIES.forEachIndexed { index, difficulty ->
                        val available = song.sheets.any { it.difficulty.equals(difficulty, true) && it.regions["jp"] == true }
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

@Composable
private fun AppNavigationBar(
    selectedTab: Int,
    backdrop: com.kyant.backdrop.Backdrop,
    onSelected: (Int) -> Unit,
    blurEnabled: Boolean,
    floating: Boolean,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        LiquidGlassTab(Icons.Rounded.Home, "主页"),
        LiquidGlassTab(Icons.Rounded.DocumentScanner, "扫描"),
        LiquidGlassTab(Icons.Rounded.Search, "歌曲"),
        LiquidGlassTab(Icons.Rounded.Settings, "设置"),
    )
    if (floating) {
        // The full-width parent is only a transparent positioning surface. The
        // bar itself keeps its intrinsic capsule width and is centered like KernelSU.
        Box(modifier.fillMaxWidth()) {
            LiquidGlassTabBar(
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 8.dp),
                selectedIndex = selectedTab,
                onSelected = onSelected,
                backdrop = backdrop,
                tabs = tabs,
                isBlurEnabled = blurEnabled,
            )
        }
    } else {
        NavigationBar(modifier = modifier, color = MiuixTheme.colorScheme.surface) {
            tabs.forEachIndexed { index, tab ->
                NavigationItem(tab.icon, tab.label, index == selectedTab) { onSelected(index) }
            }
        }
    }
}

@Composable
private fun RowScope.NavigationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        label = label,
        modifier = Modifier.weight(1f),
    )
}

@Composable
private fun InitialLoad(sync: CatalogSyncState, error: String?, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        MiuixText("CHUNITHM", style = MiuixTheme.textStyles.title1, color = MiuixTheme.colorScheme.onSurface)
        Spacer(Modifier.height(12.dp))
        MiuixText(error ?: sync.message ?: "正在下载歌曲目录", style = MiuixTheme.textStyles.body1)
        Spacer(Modifier.height(18.dp))
        if (sync.stage in setOf(CatalogSyncStage.Checking, CatalogSyncStage.Downloading, CatalogSyncStage.Validating, CatalogSyncStage.Applying)) CircularProgressIndicator()
        else MiuixButton(onClick = onRetry) { MiuixText("重试") }
    }
}

@Composable
private fun ResourceSettings(
    modifier: Modifier,
    manifest: org.rhythmeta.chunithmd.shared.StaticManifest?,
    sync: CatalogSyncState,
    error: String?,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
) {
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        MiuixText("当前版本", style = MiuixTheme.textStyles.title3)
        MiuixText(manifest?.version ?: "未安装")
        MiuixText("SHA-256\n${manifest?.sha256 ?: "-"}", style = MiuixTheme.textStyles.body2)
        MiuixText("更新时间\n${manifest?.createdAt ?: "-"}", style = MiuixTheme.textStyles.body2)
        MiuixText(sync.message ?: stageLabel(sync.stage), style = MiuixTheme.textStyles.body1)
        if (sync.stage in setOf(CatalogSyncStage.Checking, CatalogSyncStage.Downloading, CatalogSyncStage.Validating, CatalogSyncStage.Applying)) CircularProgressIndicator()
        error?.let { MiuixText(it, color = MiuixTheme.colorScheme.error) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiuixButton(onClick = onCheck) { MiuixText("检查更新") }
            MiuixButton(onClick = onDownload) { MiuixText(if (manifest == null) "下载资源" else "下载 / 重新安装") }
        }
    }
}

@Composable
private fun SettingsHome(
    modifier: Modifier,
    onAppearance: () -> Unit,
    resourceContent: @Composable () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MiuixText("外观", style = MiuixTheme.textStyles.title3, modifier = Modifier.padding(horizontal = 4.dp))
        }
        item {
            MiuixCard(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onAppearance),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiuixIcon(Icons.Rounded.Settings, contentDescription = null, tint = MiuixTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        MiuixText("主题", style = MiuixTheme.textStyles.body1)
                        MiuixText("颜色、导航栏和页面缩放", style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
            }
        }
        item {
            MiuixText("静态资源", style = MiuixTheme.textStyles.title3, modifier = Modifier.padding(horizontal = 4.dp))
        }
        item { resourceContent() }
    }
}

@Composable
private fun BlankDestination(modifier: Modifier, title: String) {
    Box(modifier.fillMaxSize().background(MiuixTheme.colorScheme.surface))
}

@Composable
private fun FilterDialog(bundle: CatalogBundle, current: CatalogFilters, onApply: (CatalogFilters) -> Unit, onDismiss: () -> Unit) {
    var draft by remember(current) { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { androidx.compose.material3.Text("筛选歌曲") },
        text = {
            LazyColumn(Modifier.height(440.dp)) {
                item { FilterToggle("仅显示 JP 可玩", draft.playableOnly) { draft = draft.copy(playableOnly = it) } }
                item { FilterChoices("分类", CatalogQuery.availableCategories(bundle), draft.categories) { draft = draft.copy(categories = it) } }
                item { FilterChoices("版本", CatalogQuery.availableVersions(bundle), draft.versions) { draft = draft.copy(versions = it) } }
                item { FilterChoices("难度", DIFFICULTIES, draft.difficulties) { draft = draft.copy(difficulties = it) } }
                item { FilterChoices("谱面类型", CatalogQuery.availableTypes(bundle), draft.types) { draft = draft.copy(types = it) } }
            }
        },
        confirmButton = { TextButton(onClick = { onApply(draft) }) { androidx.compose.material3.Text("应用") } },
        dismissButton = { TextButton(onClick = onDismiss) { androidx.compose.material3.Text("取消") } },
    )
}

@Composable
private fun FilterToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, onCheckedChange = onChange)
        androidx.compose.material3.Text(label)
    }
}

@Composable
private fun FilterChoices(title: String, options: List<String>, selected: Set<String>, onChange: (Set<String>) -> Unit) {
    Column(Modifier.padding(vertical = 8.dp)) {
        androidx.compose.material3.Text(title, style = MaterialTheme.typography.titleSmall)
        options.forEach { option -> FilterToggle(option, option in selected) { enabled -> onChange(if (enabled) selected + option else selected - option) } }
    }
}

private val DIFFICULTIES = listOf("basic", "advanced", "expert", "master", "ultima", "world's end")

private fun difficultyOrder(value: String): Int = DIFFICULTIES.indexOfFirst { it.equals(value, true) }

private fun difficultyColor(value: String): Color = when (value.lowercase()) {
    "basic" -> Color(0xFF65B94A)
    "advanced" -> Color(0xFFE6BD31)
    "expert" -> Color(0xFFE34A47)
    "master" -> Color(0xFF9A50C9)
    "ultima" -> Color(0xFF222222)
    else -> Color(0xFF4AA8C2)
}

private fun versionLabel(version: String?): String = version.orEmpty().removePrefix("CHUNITHM ").removeSuffix(" PLUS").ifBlank { "CHUNITHM" }.take(12)

private fun sortLabel(sort: CatalogSort) = when (sort) {
    CatalogSort.Default -> "默认顺序"
    CatalogSort.Title -> "标题"
    CatalogSort.VersionDate -> "版本 / 发行日期"
    CatalogSort.Difficulty -> "最高定数"
}

private fun stageLabel(stage: CatalogSyncStage) = when (stage) {
    CatalogSyncStage.Idle -> "资源已就绪"
    CatalogSyncStage.Checking -> "正在检查更新"
    CatalogSyncStage.Downloading -> "正在下载 bundle"
    CatalogSyncStage.Validating -> "正在校验资源"
    CatalogSyncStage.Applying -> "正在保存本地快照"
    CatalogSyncStage.Ready -> "资源已更新"
    CatalogSyncStage.Failed -> "更新失败"
}
