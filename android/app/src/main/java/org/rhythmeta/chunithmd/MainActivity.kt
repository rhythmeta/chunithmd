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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.unit.Density
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import androidx.compose.runtime.withFrameNanos
import kotlinx.serialization.Serializable
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import org.rhythmeta.chunithmd.shared.CatalogBundle
import org.rhythmeta.chunithmd.shared.CatalogFilters
import org.rhythmeta.chunithmd.shared.CatalogJson
import org.rhythmeta.chunithmd.shared.CatalogQuery
import org.rhythmeta.chunithmd.shared.CatalogRepository
import org.rhythmeta.chunithmd.shared.CatalogSort
import org.rhythmeta.chunithmd.shared.CatalogSyncStage
import org.rhythmeta.chunithmd.shared.CatalogSyncState
import org.rhythmeta.chunithmd.ui.theme.ChunithmdTheme
import org.rhythmeta.chunithmd.ui.theme.AppThemeSettings
import org.rhythmeta.chunithmd.ui.theme.DefaultAppThemeSettings
import org.rhythmeta.chunithmd.ui.theme.ThemePreferencesRepository
import org.rhythmeta.chunithmd.ui.theme.LocalEnableFloatingBottomBar
import org.rhythmeta.chunithmd.ui.theme.LocalEnableFloatingBottomBarBlur
import org.rhythmeta.chunithmd.ui.theme.LocalEnableBlur
import org.rhythmeta.chunithmd.ui.theme.LocalEnablePredictiveBack
import org.rhythmeta.chunithmd.ui.catalog.CatalogFilterDialog
import org.rhythmeta.chunithmd.ui.catalog.CatalogScreen
import org.rhythmeta.chunithmd.ui.catalog.CatalogSearchField
import org.rhythmeta.chunithmd.ui.catalog.CatalogToolbarActions
import org.rhythmeta.chunithmd.ui.components.AppPageScaffold
import org.rhythmeta.chunithmd.ui.components.LiquidGlassTab
import org.rhythmeta.chunithmd.ui.components.LiquidGlassTabBar
import org.rhythmeta.chunithmd.ui.settings.SettingsHome
import org.rhythmeta.chunithmd.ui.settings.ThemeSettingsScreen
import org.rhythmeta.chunithmd.ui.settings.StaticResourcesScreen
import com.kyant.backdrop.backdrops.layerBackdrop as kyantLayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop as rememberKyantLayerBackdrop
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme

import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitions

@Serializable
private sealed interface AppRoute : NavKey {
    @Serializable data object Home : AppRoute
    @Serializable data object Scan : AppRoute
    @Serializable data object Catalog : AppRoute
    @Serializable data object Settings : AppRoute
    @Serializable data object Theme : AppRoute
    @Serializable data object Resources : AppRoute
}

private val SettingsDetailTransition = object : NavTransition by NavTransitions.MiuixDefault {
    override val motion: NavMotion = NavMotion(
        commit = NavTransitions.MiuixDefault.motion.commit,
        cancel = NavTransitions.MiuixDefault.motion.cancel,
        programmatic = NavSettleSpec.Tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing,
        ),
    )
}

@Composable
private fun NavigationEventGate(
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val parent = LocalNavigationEventDispatcherOwner.current
    if (parent == null) {
        content()
        return
    }
    val dispatcher = remember(parent) {
        NavigationEventDispatcher(parent = parent.navigationEventDispatcher)
    }
    SideEffect { dispatcher.isEnabled = enabled }
    DisposableEffect(dispatcher) {
        onDispose { dispatcher.dispose() }
    }
    val owner = remember(dispatcher) {
        object : NavigationEventDispatcherOwner {
            override val navigationEventDispatcher: NavigationEventDispatcher = dispatcher
        }
    }
    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner, content = content)
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
    val enableBlur = LocalEnableBlur.current
    val navigationBackdrop = rememberKyantLayerBackdrop {
        drawRect(pageBackground)
        drawContent()
    }
    val homeTopBarScrollBehavior = MiuixScrollBehavior()
    val scanTopBarScrollBehavior = MiuixScrollBehavior()
    val catalogTopBarScrollBehavior = MiuixScrollBehavior()
    val settingsTopBarScrollBehavior = MiuixScrollBehavior()
    val themeTopBarScrollBehavior = MiuixScrollBehavior()
    val resourcesTopBarScrollBehavior = MiuixScrollBehavior()
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
                withContext(Dispatchers.IO) {
                    repository.downloadAndApply { state -> sync = state }
                }
            }.onSuccess { snapshot ->
                manifest = snapshot.manifest
                bundle = withContext(Dispatchers.Default) {
                    CatalogJson.decodeBundle(snapshot.bundleJson)
                }
            }.onFailure {
                error = it.message ?: "资源同步失败"
                sync = CatalogSyncState(CatalogSyncStage.Failed, error)
            }
        }
    }

    LaunchedEffect(Unit) {
        val localSnapshot = withContext(Dispatchers.IO) {
            runCatching { repository.loadLocal() }.getOrNull()
        }
        localSnapshot?.let { snapshot ->
            manifest = snapshot.manifest
            bundle = withContext(Dispatchers.Default) {
                CatalogJson.decodeBundle(snapshot.bundleJson)
            }
        }
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
    val predictiveBackEnabled = LocalEnablePredictiveBack.current
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
            navigateToTab(0)
        }
    }

    @Composable
    fun AppFrame(
        page: Int,
        topBarScrollBehavior: ScrollBehavior,
        content: @Composable (PaddingValues, NestedScrollConnection) -> Unit,
    ) {
        AppPageScaffold(
            title = when (page) { 5 -> "静态数据"; 4 -> "主题"; 3 -> "设置"; 0 -> "主页"; 1 -> "扫描"; else -> "歌曲" },
            pageBackground = pageBackground,
            blurEnabled = enableBlur,
            topBarScrollBehavior = topBarScrollBehavior,
            navigationIcon = {
                if (page == 4 || page == 5) MiuixIconButton(onClick = { navBackStack.removeLastOrNull() }) {
                    MiuixIcon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                }
            },
            actions = {
                if (page == 2 && bundle != null) {
                    CatalogToolbarActions(
                        sortOpen = sortOpen,
                        sort = sort,
                        ascending = ascending,
                        filterActive = filterActive,
                        onSortToggle = { sortOpen = !sortOpen },
                        onSort = { sort = it; sortOpen = false },
                        onAscending = { ascending = !ascending; sortOpen = false },
                        onFilter = { filterOpen = true },
                    )
                }
            },
            bottomContent = {
                if (page == 2 && bundle != null) AnimatedVisibility(visible = searchVisible) {
                    CatalogSearchField(search, onSearchChange = { search = it })
                }
            },
            content = content,
        )
    }

    @Composable
    fun RootPage(page: Int, modifier: Modifier = Modifier) {
        Box(modifier) {
            val topBarScrollBehavior = when (page) {
                0 -> homeTopBarScrollBehavior
                1 -> scanTopBarScrollBehavior
                2 -> catalogTopBarScrollBehavior
                else -> settingsTopBarScrollBehavior
            }
            AppFrame(page, topBarScrollBehavior) { padding, topBarScrollConnection ->
            when (page) {
                0 -> BlankDestination(Modifier.padding(padding).fillMaxSize(), "主页")
                1 -> BlankDestination(Modifier.padding(padding).fillMaxSize(), "扫描")
                2 -> CatalogScreen(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    bundle = bundle,
                    sync = sync,
                    error = error,
                    songs = songs,
                    jacketBaseUrl = manifest?.assets?.jacketBaseUrl.orEmpty(),
                    localJacketPath = repository::localJacketPath,
                    isDark = isDark,
                    navigationBackdrop = navigationBackdrop,
                    searchScrollConnection = searchScrollConnection,
                    topBarScrollConnection = topBarScrollConnection,
                    onRetry = ::refresh,
                )
                else -> SettingsHome(
                    Modifier.padding(padding).fillMaxSize()
                        .background(MiuixTheme.colorScheme.surface)
                        .kyantLayerBackdrop(navigationBackdrop)
                        .nestedScroll(topBarScrollConnection),
                    { navBackStack.add(AppRoute.Theme) },
                    { navBackStack.add(AppRoute.Resources) },
                )
            }
        }
    }
    }

    if (!predictiveBackEnabled) {
        BackHandler(enabled = navBackStack.size > 1) {
            navBackStack.removeLastOrNull()
        }
    }

    NavigationEventGate(predictiveBackEnabled) {
    NavDisplay(
        backStack = navBackStack,
        modifier = Modifier.fillMaxSize(),
        transition = NavTransitions.MiuixDefault,
        effects = NavDisplayEffects(cornerClipRadius = rememberNavSystemCornerRadius()),
        onBack = {
            if (navBackStack.lastOrNull() == AppRoute.Theme || navBackStack.lastOrNull() == AppRoute.Resources) {
                navBackStack.removeLastOrNull()
            } else if (navBackStack.size > 1) {
                navBackStack.removeLastOrNull()
                selectedTab = 0
            }
        },
    ) {
        entry<AppRoute.Theme>(
            transition = SettingsDetailTransition,
            swipeDismiss = if (predictiveBackEnabled) NavSwipeDirection.LeftToRight else NavSwipeDirection.None,
        ) {
            AppFrame(4, themeTopBarScrollBehavior) { padding, topBarScrollConnection ->
                Box(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.surface)) {
                    ThemeSettingsScreen(
                        settings = themeSettings,
                        contentTopPadding = padding.calculateTopPadding(),
                        onColorModeChange = { scope.launch { themeRepository.setColorMode(it) } },
                        onKeyColorChange = { scope.launch { themeRepository.setKeyColor(it) } },
                        onPaletteStyleChange = { scope.launch { themeRepository.setPaletteStyle(it) } },
                        onColorSpecChange = { scope.launch { themeRepository.setColorSpec(it) } },
                        onEnableBlurChange = { scope.launch { themeRepository.setBlur(it) } },
                        onEnableFloatingBottomBarChange = { scope.launch { themeRepository.setFloatingBar(it) } },
                        onEnableFloatingBottomBarBlurChange = { scope.launch { themeRepository.setFloatingBarBlur(it) } },
                        onEnablePredictiveBackChange = { scope.launch { themeRepository.setPredictiveBack(it) } },
                        onPageScaleChange = { scope.launch { themeRepository.setPageScale(it) } },
                        modifier = Modifier.nestedScroll(topBarScrollConnection),
                    )
                }
            }
        }
        entry<AppRoute.Resources>(
            transition = SettingsDetailTransition,
            swipeDismiss = if (predictiveBackEnabled) NavSwipeDirection.LeftToRight else NavSwipeDirection.None,
        ) {
            AppFrame(5, resourcesTopBarScrollBehavior) { padding, topBarScrollConnection ->
                Box(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.surface)) {
                    StaticResourcesScreen(
                        modifier = Modifier.padding(padding).nestedScroll(topBarScrollConnection),
                        manifest = manifest,
                        sync = sync,
                        error = error,
                        onCheck = {
                            if (sync.stage !in setOf(
                                    CatalogSyncStage.Checking,
                                    CatalogSyncStage.Downloading,
                                    CatalogSyncStage.Validating,
                                    CatalogSyncStage.Applying,
                                )) {
                                scope.launch {
                                    error = null
                                    sync = CatalogSyncState(CatalogSyncStage.Checking)
                                    runCatching {
                                        withContext(Dispatchers.IO) { repository.checkForUpdate() }
                                    }
                                        .onSuccess { check ->
                                            sync = CatalogSyncState(
                                                CatalogSyncStage.Idle,
                                                if (check.updateAvailable) "发现可用更新" else "已是最新静态数据",
                                            )
                                        }
                                        .onFailure {
                                            error = it.message
                                            sync = CatalogSyncState(CatalogSyncStage.Failed, error)
                                        }
                                }
                            }
                        },
                        onDownload = ::refresh,
                    )
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
    }

    if (filterOpen && bundle != null) {
        CatalogFilterDialog(bundle!!, filters, onApply = { filters = it; filterOpen = false }, onDismiss = { filterOpen = false })
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
private fun BlankDestination(modifier: Modifier, title: String) {
    Box(modifier.fillMaxSize().background(MiuixTheme.colorScheme.surface))
}
