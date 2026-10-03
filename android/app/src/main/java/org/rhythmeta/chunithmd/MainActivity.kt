package org.rhythmeta.chunithmd

import org.rhythmeta.chunithmd.shared.localization.tr

import android.os.Bundle
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.rhythmeta.chunithmd.collection.SongCollectionRepository
import org.rhythmeta.chunithmd.profile.ProfileAvatarStore
import org.rhythmeta.chunithmd.profile.ProfileRepository
import org.rhythmeta.chunithmd.score.ScoreRepository
import org.rhythmeta.chunithmd.shared.BestTablePreferences
import org.rhythmeta.chunithmd.shared.CatalogBundle
import org.rhythmeta.chunithmd.shared.CatalogFilters
import org.rhythmeta.chunithmd.shared.CatalogJson
import org.rhythmeta.chunithmd.shared.CatalogQuery
import org.rhythmeta.chunithmd.shared.CatalogRepository
import org.rhythmeta.chunithmd.shared.CatalogSongFormatter
import org.rhythmeta.chunithmd.shared.CatalogSort
import org.rhythmeta.chunithmd.shared.CatalogSyncStage
import org.rhythmeta.chunithmd.shared.CatalogSyncState
import org.rhythmeta.chunithmd.shared.ScoreQueryDisplayMode
import org.rhythmeta.chunithmd.shared.ScoreQueryFilterSettings
import org.rhythmeta.chunithmd.shared.ScoreQuerySortMode
import org.rhythmeta.chunithmd.shared.ScoreRecord
import org.rhythmeta.chunithmd.shared.latestPlayableVersion
import org.rhythmeta.chunithmd.ui.best.BestTableHomeCard
import org.rhythmeta.chunithmd.ui.best.BestTableScreen
import org.rhythmeta.chunithmd.ui.catalog.CatalogFilterDialog
import org.rhythmeta.chunithmd.ui.catalog.CatalogPreferencesRepository
import org.rhythmeta.chunithmd.ui.catalog.CatalogScreen
import org.rhythmeta.chunithmd.ui.catalog.CatalogSearchField
import org.rhythmeta.chunithmd.ui.catalog.CatalogToolbarActions
import org.rhythmeta.chunithmd.ui.catalog.FavoriteSongRepository
import org.rhythmeta.chunithmd.ui.catalog.SongDetailScreen
import org.rhythmeta.chunithmd.ui.collections.CollectionsHomeCard
import org.rhythmeta.chunithmd.ui.collections.CollectionsToolbarActions
import org.rhythmeta.chunithmd.ui.collections.SongCollectionsScreen
import org.rhythmeta.chunithmd.ui.collections.rememberCollectionsUiState
import org.rhythmeta.chunithmd.ui.components.AppPageScaffold
import org.rhythmeta.chunithmd.ui.components.LiquidGlassTab
import org.rhythmeta.chunithmd.ui.components.LiquidGlassTabBar
import org.rhythmeta.chunithmd.ui.constanttable.ConstantTableHomeCard
import org.rhythmeta.chunithmd.ui.constanttable.ConstantTableScreen
import org.rhythmeta.chunithmd.ui.constanttable.ConstantTableToolbarActions
import org.rhythmeta.chunithmd.ui.plate.PlateProgressHomeCard
import org.rhythmeta.chunithmd.ui.plate.PlateProgressScreen
import org.rhythmeta.chunithmd.ui.profile.CurrentProfileCard
import org.rhythmeta.chunithmd.ui.profile.ProfileEditorSheet
import org.rhythmeta.chunithmd.ui.profile.ProfileScreen
import org.rhythmeta.chunithmd.ui.random.RandomSongHomeCard
import org.rhythmeta.chunithmd.ui.random.RandomSongScreen
import org.rhythmeta.chunithmd.ui.random.RandomSongSessionState
import org.rhythmeta.chunithmd.ui.recommendation.RecommendationHomeCard
import org.rhythmeta.chunithmd.ui.recommendation.RecommendationScreen
import org.rhythmeta.chunithmd.ui.scorequery.ScoreQueryHomeCard
import org.rhythmeta.chunithmd.ui.scorequery.ScoreQueryScreen
import org.rhythmeta.chunithmd.ui.scorequery.ScoreQueryToolbarActions
import org.rhythmeta.chunithmd.ui.settings.SettingsHome
import org.rhythmeta.chunithmd.ui.settings.StaticResourcesScreen
import org.rhythmeta.chunithmd.ui.settings.ThemeSettingsScreen
import org.rhythmeta.chunithmd.ui.theme.AppThemeSettings
import org.rhythmeta.chunithmd.ui.theme.ChunithmdTheme
import org.rhythmeta.chunithmd.ui.theme.DefaultAppThemeSettings
import org.rhythmeta.chunithmd.ui.theme.LocalEnableBlur
import org.rhythmeta.chunithmd.ui.theme.LocalEnableFloatingBottomBar
import org.rhythmeta.chunithmd.ui.theme.LocalEnableFloatingBottomBarBlur
import org.rhythmeta.chunithmd.ui.theme.LocalEnablePredictiveBack
import org.rhythmeta.chunithmd.ui.theme.ThemePreferencesRepository
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.kyant.backdrop.backdrops.layerBackdrop as kyantLayerBackdrop
import org.rhythmeta.chunithmd.shared.community.*
import org.rhythmeta.chunithmd.ui.community.CommunityAliasScreen
import org.rhythmeta.chunithmd.ui.community.CommunityAliasHomeCard
import com.kyant.backdrop.backdrops.rememberLayerBackdrop as rememberKyantLayerBackdrop
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton

@Serializable
private sealed interface AppRoute : NavKey {
    @Serializable data object Home : AppRoute
//    @Serializable data object Scan : AppRoute
//    @Serializable data object Catalog : AppRoute
//    @Serializable data object Settings : AppRoute
    @Serializable data object Theme : AppRoute
    @Serializable data object Resources : AppRoute
    @Serializable data object Account : AppRoute
    @Serializable data object CommunityAliases : AppRoute
    @Serializable data object Profiles : AppRoute
    @Serializable data object BestTable : AppRoute
    @Serializable data object RandomSong : AppRoute
    @Serializable data object Recommendations : AppRoute
    @Serializable data object ScoreQuery : AppRoute
    @Serializable data object ConstantTable : AppRoute
    @Serializable data object PlateProgress : AppRoute
    @Serializable data object Collections : AppRoute
    @Serializable data class CollectionDetail(val collectionId: String) : AppRoute
    @Serializable data class SongDetail(val songId: String) : AppRoute
}

/** Keeps the decoded catalog across Activity recreation caused by rotation. */
class CatalogStateViewModel : ViewModel() {
    var bundle: CatalogBundle? by mutableStateOf(null)
    var manifest: org.rhythmeta.chunithmd.shared.StaticManifest? by mutableStateOf(null)
    var sync: CatalogSyncState by mutableStateOf(CatalogSyncState())
    var error: String? by mutableStateOf(null)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var localLoadJob: Job? = null

    fun loadLocal(repository: CatalogRepository) {
        if (bundle != null || localLoadJob?.isActive == true) return
        localLoadJob = scope.launch {
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
    }

    override fun onCleared() {
        scope.cancel()
//        super.onCleared()
    }
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
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(org.rhythmeta.chunithmd.localization.AndroidAppLanguage.localizedContext(newBase))
    }

    private var exportingLogs = false

    private fun sendLogs() {
        if (exportingLogs) return
        exportingLogs = true
        lifecycleScope.launch {
            try {
                val app = application as ChunithmdApplication
                val uri = org.rhythmeta.chunithmd.diagnostics.LogReportExporter.export(applicationContext, app.crashLogStore)
                val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    clipData = android.content.ClipData.newRawUri("chunithmd logs", uri)
                }
                startActivity(android.content.Intent.createChooser(share, tr("分享 chunithmd 日志")))
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                android.widget.Toast.makeText(this@MainActivity, tr("无法生成或分享日志文件"), android.widget.Toast.LENGTH_LONG).show()
            } finally {
                exportingLogs = false
            }
        }
    }

    private var pendingCollectionLink by mutableStateOf<String?>(null)
    private fun receiveCollection(intent: android.content.Intent?) {
        val url = intent?.data?.takeIf { it.scheme == "chunithmd" && it.host == "collection" } ?: return
        pendingCollectionLink = url.toString()
    }

    private val accountClient by lazy { org.rhythmeta.chunithmd.shared.account.RhythmetaClient(org.rhythmeta.chunithmd.account.AndroidSecretStore(applicationContext)) }
    private fun receiveAuth(intent: android.content.Intent?) {
        val url = intent?.data?.takeIf { it.scheme == "chunithmd" && it.host == "auth" } ?: return
        lifecycleScope.launch {
            try { accountClient.handleCallback(url.toString()) }
            catch(error: Exception) { android.widget.Toast.makeText(this@MainActivity, error.message ?: tr("登录失败"), android.widget.Toast.LENGTH_LONG).show() }
        }
    }
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveAuth(intent)
        receiveCollection(intent)
    }

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
        val profileRepository = ProfileRepository(applicationContext)
        val scoreRepository = ScoreRepository(applicationContext, profileRepository)
        val favoriteSongRepository = FavoriteSongRepository(applicationContext)
        val profileAvatarStore = ProfileAvatarStore(applicationContext)
        val bestTablePreferencesRepository = org.rhythmeta.chunithmd.ui.best.BestTablePreferencesRepository(applicationContext)
        val catalogPreferencesRepository = CatalogPreferencesRepository(applicationContext)
        val themeRepository = ThemePreferencesRepository(applicationContext)
        val catalogState = ViewModelProvider(this)[CatalogStateViewModel::class.java]
        val collectionRepository = SongCollectionRepository(applicationContext)
        val backupCoordinator = org.rhythmeta.chunithmd.shared.backup.BackupCoordinator(accountClient,
            org.rhythmeta.chunithmd.account.AndroidSnapshotStore(applicationContext, profileRepository, scoreRepository, collectionRepository, favoriteSongRepository))
        receiveAuth(intent)
        receiveCollection(intent)
        setContent {
            var recovered by remember { mutableStateOf(false) }
            var recoveryError by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(Unit) {
                try { backupCoordinator.recover(); recovered=true }
                catch(error: Exception) { recoveryError=error.message }
            }
            if (!recovered) {
                ChunithmdTheme(DefaultAppThemeSettings) {
                    top.yukonga.miuix.kmp.basic.Text(recoveryError ?: tr("正在恢复本地数据…"), modifier=Modifier.padding(32.dp))
                }
                return@setContent
            }
            val themeSettings by produceState(DefaultAppThemeSettings, themeRepository) {
                themeRepository.settings.collect { value = it }
            }
            ChunithmdTheme(themeSettings) {
                val baseDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(baseDensity.density * themeSettings.pageScale, baseDensity.fontScale),
                ) {
                    CatalogApp(repository, catalogPreferencesRepository, themeRepository, themeSettings, profileRepository, profileAvatarStore, scoreRepository, favoriteSongRepository, bestTablePreferencesRepository, catalogState, collectionRepository, accountClient, backupCoordinator, pendingCollectionLink, ::sendLogs) {
                        pendingCollectionLink = null
                        intent?.data = null
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogApp(
    repository: CatalogRepository,
    catalogPreferencesRepository: CatalogPreferencesRepository,
    themeRepository: ThemePreferencesRepository,
    themeSettings: AppThemeSettings,
    profileRepository: ProfileRepository,
    profileAvatarStore: ProfileAvatarStore,
    scoreRepository: ScoreRepository,
    favoriteSongRepository: FavoriteSongRepository,
    bestTablePreferencesRepository: org.rhythmeta.chunithmd.ui.best.BestTablePreferencesRepository,
    catalogState: CatalogStateViewModel,
    collectionRepository: SongCollectionRepository,
    accountClient: org.rhythmeta.chunithmd.shared.account.RhythmetaClient,
    backupCoordinator: org.rhythmeta.chunithmd.shared.backup.BackupCoordinator,
    pendingCollectionLink: String?,
    onSendLogs: () -> Unit,
    onCollectionLinkConsumed: () -> Unit,
) {
    var catalogBundle by catalogState::bundle
    val communityContext = LocalContext.current.applicationContext
    val communityStore = remember(accountClient) {
        CommunityAliasStore(RhythmetaCommunityApi(accountClient), FileCommunityAliasCache(communityContext.filesDir.absolutePath))
    }
    val communityState by communityStore.state.collectAsState()
    val bundle = remember(catalogBundle, communityState.approvedAliases, communityState.personalAliases) {
        catalogBundle?.withCommunityAliases(communityState)
    }
    LaunchedEffect(communityStore) { communityStore.observeAccount() }
    LaunchedEffect(communityStore) { communityStore.syncApproved() }
    val collections by collectionRepository.collections.collectAsState(initial = emptyList())
    val collectionsUiState = rememberCollectionsUiState()
    var manifest by catalogState::manifest
    var sync by catalogState::sync
    var error by catalogState::error
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var animateRootTransition by remember { mutableStateOf(true) }
    val rootBackProgress = remember { Animatable(0f) }
    var search by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(CatalogSort.Default) }
    var ascending by remember { mutableStateOf(true) }
    var filters by remember { mutableStateOf(CatalogFilters()) }
    var filterOpen by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }
    var profileCreateRequested by remember { mutableStateOf(false) }
    var bestTableShareRequested by remember { mutableStateOf(false) }
    var randomSongFilterRequested by remember { mutableStateOf(false) }
    var randomSongFilterActive by remember { mutableStateOf(false) }
    var recommendationSelectedPage by rememberSaveable { mutableIntStateOf(0) }
    var recommendationSwitcherVisible by rememberSaveable { mutableStateOf(true) }
    var scoreQuerySearch by rememberSaveable { mutableStateOf("") }
    var scoreQueryFilterSettings by remember { mutableStateOf(ScoreQueryFilterSettings()) }
    var scoreQueryDisplayMode by rememberSaveable { mutableStateOf(ScoreQueryDisplayMode.List) }
    var scoreQuerySortMode by rememberSaveable { mutableStateOf(ScoreQuerySortMode.Rating) }
    var scoreQueryAscending by rememberSaveable { mutableStateOf(false) }
    var scoreQueryFilterOpen by remember { mutableStateOf(false) }
    var scoreQuerySearchVisible by remember { mutableStateOf(true) }
    var scoreQuerySearchExpanded by remember { mutableStateOf(false) }
    val scoreQuerySearchInteractionSource = remember { MutableInteractionSource() }
    val scoreQuerySearchFocused by scoreQuerySearchInteractionSource.collectIsFocusedAsState()
    var constantTableFilterSettings by remember { mutableStateOf(CatalogFilters()) }
    var constantTableFilterOpen by remember { mutableStateOf(false) }
    var constantTableExportRequested by remember { mutableStateOf(false) }
    var quickEditProfile by remember { mutableStateOf<org.rhythmeta.chunithmd.shared.UserProfile?>(null) }
    val randomSongSessionState = remember { RandomSongSessionState() }
    val activeProfile by profileRepository.activeProfile.collectAsState(initial = null)
    val profileScores by scoreRepository.observeCurrentProfileRecords().collectAsState(initial = emptyList())
    val bestTablePreferences by bestTablePreferencesRepository.preferences.collectAsState(initial = BestTablePreferences())
    val favoriteSongIds by favoriteSongRepository.favoriteSongIds.collectAsState(initial = emptySet())
    val scoresBySheetKey = remember(profileScores) {
        profileScores.groupBy(ScoreRecord::sheetKey)
            .mapValues { (_, records) -> records.maxByOrNull(ScoreRecord::score)!! }
    }
    val profileVersions = remember(bundle) {
        org.rhythmeta.chunithmd.shared.ProfileServer.entries.associateWith { server ->
            bundle?.latestPlayableVersion(server)
        }
    }
    val catalogListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = rememberCoroutineScope()
    val recommendationSwitcherShowThreshold = with(LocalDensity.current) { 56.dp.toPx() }
    val recommendationSwitcherHideThreshold = with(LocalDensity.current) { 36.dp.toPx() }
    val recommendationSwitcherScrollConnection = remember(
        recommendationSwitcherShowThreshold,
        recommendationSwitcherHideThreshold,
    ) {
        object : NestedScrollConnection {
            private var downwardDistance = 0f
            private var upwardDistance = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return super.onPreScroll(available, source)
                when {
                    available.y > 0f -> {
                        upwardDistance = 0f
                        if (recommendationSwitcherVisible) {
                            downwardDistance = 0f
                        } else {
                            downwardDistance += available.y
                            if (downwardDistance >= recommendationSwitcherShowThreshold) {
                                recommendationSwitcherVisible = true
                                downwardDistance = 0f
                            }
                        }
                    }
                    available.y < 0f -> {
                        downwardDistance = 0f
                        if (recommendationSwitcherVisible) {
                            upwardDistance -= available.y
                            if (upwardDistance >= recommendationSwitcherHideThreshold) {
                                recommendationSwitcherVisible = false
                                upwardDistance = 0f
                            }
                        } else {
                            upwardDistance = 0f
                        }
                    }
                }
                return super.onPreScroll(available, source)
            }
        }
    }

    LaunchedEffect(catalogPreferencesRepository) {
        val saved = catalogPreferencesRepository.preferences.first()
        sort = saved.sort
        ascending = saved.ascending
        filters = saved.filters
    }
    LaunchedEffect(profileRepository) { profileRepository.ensureDefaultProfile() }
    // Miuix's surface is the page canvas used by the reference navigation shell.
    val pageBackground = MiuixTheme.colorScheme.surface
    var songDetailBackground by remember { mutableStateOf<Color?>(null) }
    val enableBlur = LocalEnableBlur.current
    val navigationBackdrop = rememberKyantLayerBackdrop {
        drawRect(pageBackground)
        drawContent()
    }
    val homeTopBarScrollBehavior = MiuixScrollBehavior()
    var navigationBarHeightPx by remember { mutableIntStateOf(0) }
    val navigationBarBottomSpace = with(LocalDensity.current) { navigationBarHeightPx.toDp() } + 16.dp
    val scanTopBarScrollBehavior = MiuixScrollBehavior()
    val catalogTopBarScrollBehavior = MiuixScrollBehavior()
    val settingsTopBarScrollBehavior = MiuixScrollBehavior()
    val themeTopBarScrollBehavior = MiuixScrollBehavior()
    val resourcesTopBarScrollBehavior = MiuixScrollBehavior()
    val bestTopBarScrollBehavior = MiuixScrollBehavior()
    val randomSongTopBarScrollBehavior = MiuixScrollBehavior()
    val recommendationTopBarScrollBehavior = MiuixScrollBehavior()
    val scoreQueryTopBarScrollBehavior = MiuixScrollBehavior()
    val constantTableTopBarScrollBehavior = MiuixScrollBehavior()
    val plateProgressTopBarScrollBehavior = MiuixScrollBehavior()
    val collectionsTopBarScrollBehavior = MiuixScrollBehavior()
    val collectionDetailTopBarScrollBehavior = MiuixScrollBehavior()
    val communityTopBarScrollBehavior = MiuixScrollBehavior()
    var searchVisible by remember { mutableStateOf(true) }
    var searchExpanded by remember { mutableStateOf(false) }
    val searchInteractionSource = remember { MutableInteractionSource() }
    val searchFocused by searchInteractionSource.collectIsFocusedAsState()
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
    val scoreQuerySearchScrollConnection = remember {
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
                            scoreQuerySearchVisible = false
                            upDistance = 0f
                        }
                    }
                    available.y > 0f -> {
                        upDistance = 0f
                        downDistance += available.y
                        if (downDistance >= 24f) {
                            scoreQuerySearchVisible = true
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
                catalogBundle = withContext(Dispatchers.Default) {
                    CatalogJson.decodeBundle(snapshot.bundleJson)
                }
            }.onFailure {
                error = it.message ?: tr("资源同步失败")
                sync = CatalogSyncState(CatalogSyncStage.Failed, error)
            }
        }
    }

    LaunchedEffect(repository, catalogState) { catalogState.loadLocal(repository) }

    val playableRegion = activeProfile?.server?.wireValue ?: "jp"
    val songs = remember(bundle, search, sort, ascending, filters, playableRegion, favoriteSongIds) {
        bundle?.let {
            CatalogQuery.filterAndSort(
                it,
                search,
                sort,
                ascending,
                filters,
                playableRegion = playableRegion,
                favoriteSongIds = favoriteSongIds,
            )
        }.orEmpty()
    }
    val filterActive = filters.categories.isNotEmpty() || filters.versions.isNotEmpty() ||
        filters.difficulties.isNotEmpty() || filters.types.isNotEmpty() ||
        filters.minLevel != 1.0 || filters.maxLevel != 16.0 ||
        filters.playableOnly || filters.hideDeleted || filters.favoritesOnly
    // Keep the detail page in the same navigation state as the root pages so its
    // enter/exit transition is observable and back can return to Settings.
    val navBackStack = rememberNavBackStack<AppRoute>(AppRoute.Home)
    fun pushRoute(route: AppRoute) {
        if (route == AppRoute.Recommendations) recommendationSwitcherVisible = true
        if (navBackStack.lastOrNull() != route) {
            navBackStack.add(route)
        }
    }
    LaunchedEffect(pendingCollectionLink) {
        pendingCollectionLink?.let { link ->
            collectionsUiState.importValue = link
            collectionsUiState.importRequested = true
            pushRoute(AppRoute.Collections)
            onCollectionLinkConsumed()
        }
    }
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
        titleOverride: String? = null,
        content: @Composable (PaddingValues, NestedScrollConnection) -> Unit,
    ) {
        AppPageScaffold(
            title = titleOverride ?: when (page) { 16 -> tr("社区别名"); 15 -> tr("云端账户"); 14, 13 -> tr("收藏夹"); 12 -> tr("牌子进度"); 11 -> tr("定数表"); 10 -> tr("成绩查询"); 9 -> tr("吃分推荐"); 8 -> tr("随机歌曲"); 7 -> tr("Best 表"); 6 -> tr("用户档案"); 5 -> tr("静态数据"); 4 -> tr("主题"); 3 -> tr("设置"); 0 -> tr("主页"); 1 -> tr("扫描"); else -> tr("歌曲") },
            pageBackground = pageBackground,
            blurEnabled = enableBlur,
            topBarScrollBehavior = topBarScrollBehavior,
            navigationIcon = {
                if (page in 4..16) MiuixIconButton(onClick = { navBackStack.removeLastOrNull() }) {
                    MiuixIcon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = tr("返回"))
                }
            },
            actions = {
                if (page == 13 || page == 14) {
                    CollectionsToolbarActions(collectionsUiState, detail = page == 14)
                }
                if (page == 2 && bundle != null) {
                    CatalogToolbarActions(
                        sortOpen = sortOpen,
                        sort = sort,
                        ascending = ascending,
                        filterActive = filterActive,
                        onSortToggle = { sortOpen = !sortOpen },
                        onSort = {
                            sort = it
                            sortOpen = false
                            scope.launch { catalogPreferencesRepository.setSort(it) }
                        },
                        onAscending = {
                            ascending = !ascending
                            sortOpen = false
                            scope.launch { catalogPreferencesRepository.setAscending(ascending) }
                        },
                        onFilter = { filterOpen = true },
                    )
                }
                if (page == 6) {
                    MiuixIconButton(onClick = { profileCreateRequested = true }) {
                        MiuixIcon(Icons.Rounded.PersonAdd, contentDescription = tr("新建档案"))
                    }
                }
                if (page == 7) {
                    MiuixIconButton(onClick = { bestTableShareRequested = true }) {
                        MiuixIcon(Icons.Rounded.IosShare, contentDescription = tr("分享 Best Table"))
                    }
                }
                if (page == 8) {
                    MiuixIconButton(onClick = { randomSongFilterRequested = true }) {
                        MiuixIcon(
                            Icons.Rounded.FilterList,
                            contentDescription = tr("筛选"),
                            tint = if (randomSongFilterActive) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }
                if (page == 10) {
                    ScoreQueryToolbarActions(
                        displayMode = scoreQueryDisplayMode,
                        sortMode = scoreQuerySortMode,
                        ascending = scoreQueryAscending,
                        filterActive = !scoreQueryFilterSettings.isEmpty,
                        onDisplayModeChange = { scoreQueryDisplayMode = it },
                        onSortModeChange = { scoreQuerySortMode = it },
                        onAscendingChange = { scoreQueryAscending = it },
                        onFilter = { scoreQueryFilterOpen = true },
                    )
                }
                if (page == 11) {
                    ConstantTableToolbarActions(
                        filterActive = constantTableFilterSettings != CatalogFilters(),
                        onFilter = { constantTableFilterOpen = true },
                    )
                }
            },
            bottomContent = {
                if (page == 2 && bundle != null) {
                    CatalogSearchField(
                        search = search,
                        onSearchChange = { search = it },
                        visible = searchVisible || searchFocused,
                        expanded = searchExpanded,
                        backEnabled = searchFocused,
                        onExpandedChange = { searchExpanded = it },
                        interactionSource = searchInteractionSource,
                    )
                }
                if (page == 9) {
                    org.rhythmeta.chunithmd.ui.recommendation.RecommendationPageSwitcher(
                        selectedPage = recommendationSelectedPage,
                        visible = recommendationSwitcherVisible,
                        onSelectedPageChange = { recommendationSelectedPage = it },
                    )
                }
                if (page == 10) {
                    CatalogSearchField(
                        search = scoreQuerySearch,
                        onSearchChange = { scoreQuerySearch = it },
                        visible = scoreQuerySearchVisible || scoreQuerySearchFocused,
                        expanded = scoreQuerySearchExpanded,
                        backEnabled = scoreQuerySearchFocused,
                        onExpandedChange = { scoreQuerySearchExpanded = it },
                        interactionSource = scoreQuerySearchInteractionSource,
                    )
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
                8 -> randomSongTopBarScrollBehavior
                9 -> recommendationTopBarScrollBehavior
                10 -> scoreQueryTopBarScrollBehavior
                11 -> constantTableTopBarScrollBehavior
                else -> settingsTopBarScrollBehavior
            }
            AppFrame(page, topBarScrollBehavior) { padding, topBarScrollConnection ->
            when (page) {
                0 -> Column(
                    Modifier.padding(padding)
                        .fillMaxSize()
                        .nestedScroll(topBarScrollConnection)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = navigationBarBottomSpace),
                ) {
                    CurrentProfileCard(activeProfile) { activeProfile?.let { quickEditProfile = it } }
                    BestTableHomeCard(
                        bestCount = bestTablePreferences.bestCount,
                        newCount = bestTablePreferences.newCount,
                        onClick = { pushRoute(AppRoute.BestTable) },
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
                    ) {
                        RandomSongHomeCard(
                            modifier = Modifier.weight(1f),
                            onClick = { pushRoute(AppRoute.RandomSong) },
                        )
                        RecommendationHomeCard(
                            modifier = Modifier.weight(1f),
                            onClick = { pushRoute(AppRoute.Recommendations) },
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
                    ) {
                        ScoreQueryHomeCard(
                            modifier = Modifier.weight(1f),
                            onClick = { pushRoute(AppRoute.ScoreQuery) },
                        )
                        ConstantTableHomeCard(
                            modifier = Modifier.weight(1f).height(140.dp),
                            onClick = { pushRoute(AppRoute.ConstantTable) },
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
                    ) {
                        PlateProgressHomeCard(
                            modifier = Modifier.weight(1f),
                            onClick = { pushRoute(AppRoute.PlateProgress) },
                        )
                        CommunityAliasHomeCard(
                            modifier = Modifier.weight(1f),
                            onClick = { pushRoute(AppRoute.CommunityAliases) },
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
                    ) {
                        CollectionsHomeCard(
                            modifier = Modifier.weight(1f),
                            onClick = { pushRoute(AppRoute.Collections) },
                        )
                        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    }
                }
                1 -> BlankDestination(Modifier.padding(padding).fillMaxSize())
                2 -> CatalogScreen(
                    modifier = Modifier.fillMaxSize(),
                    contentTopPadding = padding.calculateTopPadding(),
                    bundle = bundle,
                    sync = sync,
                    error = error,
                    songs = songs,
                    scoresBySheetKey = scoresBySheetKey,
                    jacketBaseUrl = manifest?.assets?.jacketBaseUrl.orEmpty(),
                    localJacketPath = repository::localJacketPath,
                    listState = catalogListState,
                    navigationBackdrop = navigationBackdrop,
                    searchScrollConnection = searchScrollConnection,
                    topBarScrollConnection = topBarScrollConnection,
                    onRetry = ::refresh,
                    onSongClick = { song -> pushRoute(AppRoute.SongDetail(song.songId)) },
                )
                else -> SettingsHome(
                    Modifier.padding(padding).fillMaxSize()
                        .background(MiuixTheme.colorScheme.surface)
                        .kyantLayerBackdrop(navigationBackdrop)
                        .nestedScroll(topBarScrollConnection),
                    { pushRoute(AppRoute.Theme) },
                    { pushRoute(AppRoute.Resources) },
                    { pushRoute(AppRoute.Profiles) },
                    { pushRoute(AppRoute.Account) },
                    accountClient = accountClient,
                    onSendLogs = onSendLogs,
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
            if (navBackStack.lastOrNull() == AppRoute.CommunityAliases || navBackStack.lastOrNull() == AppRoute.Account || navBackStack.lastOrNull() == AppRoute.Theme || navBackStack.lastOrNull() == AppRoute.Resources || navBackStack.lastOrNull() == AppRoute.Profiles || navBackStack.lastOrNull() == AppRoute.BestTable || navBackStack.lastOrNull() == AppRoute.RandomSong || navBackStack.lastOrNull() == AppRoute.Recommendations || navBackStack.lastOrNull() == AppRoute.ScoreQuery || navBackStack.lastOrNull() == AppRoute.ConstantTable || navBackStack.lastOrNull() == AppRoute.PlateProgress) {
                navBackStack.removeLastOrNull()
            } else if (navBackStack.lastOrNull() is AppRoute.SongDetail || navBackStack.lastOrNull() == AppRoute.Collections || navBackStack.lastOrNull() is AppRoute.CollectionDetail) {
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
                        topAppBarScrollBehavior = resourcesTopBarScrollBehavior,
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
                                                if (check.updateAvailable) tr("发现可用更新") else tr("已是最新静态数据"),
                                                updateAvailable = check.updateAvailable,
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
        entry<AppRoute.CommunityAliases>(
            transition = SettingsDetailTransition,
            swipeDismiss = if (predictiveBackEnabled) NavSwipeDirection.LeftToRight else NavSwipeDirection.None,
        ) {
            AppFrame(16, communityTopBarScrollBehavior) { padding, connection ->
                CommunityAliasScreen(
                    store = communityStore,
                    songs = bundle?.catalog?.songs.orEmpty(),
                    jacketBaseUrl = manifest?.assets?.jacketBaseUrl.orEmpty(),
                    localJacketPath = repository::localJacketPath,
                    contentTopPadding = padding.calculateTopPadding(),
                    topBarScrollConnection = connection,
                    onOpenSong = { pushRoute(AppRoute.SongDetail(it)) },
                    onLogin = { pushRoute(AppRoute.Account) },
                )
            }
        }
        entry<AppRoute.Account>(
            transition = SettingsDetailTransition,
            swipeDismiss = if (predictiveBackEnabled) NavSwipeDirection.LeftToRight else NavSwipeDirection.None,
        ) {
            AppFrame(15, resourcesTopBarScrollBehavior) { padding, connection ->
                org.rhythmeta.chunithmd.account.RhythmetaScreen(accountClient, backupCoordinator, Modifier.padding(padding).nestedScroll(connection))
            }
        }
        entry<AppRoute.Profiles>(
            transition = SettingsDetailTransition,
            swipeDismiss = if (predictiveBackEnabled) NavSwipeDirection.LeftToRight else NavSwipeDirection.None,
        ) {
            AppFrame(6, resourcesTopBarScrollBehavior) { padding, topBarScrollConnection ->
                ProfileScreen(
                    modifier = Modifier.padding(padding).nestedScroll(topBarScrollConnection),
                    repository = profileRepository,
                    avatarStore = profileAvatarStore,
                    currentVersionByServer = profileVersions,
                    createRequested = profileCreateRequested,
                    onCreateRequestHandled = { profileCreateRequested = false },
                )
            }
        }
        entry<AppRoute.BestTable>(
            transition = SettingsDetailTransition,
            swipeDismiss = if (predictiveBackEnabled) NavSwipeDirection.LeftToRight else NavSwipeDirection.None,
        ) {
            AppFrame(7, bestTopBarScrollBehavior) { padding, topBarScrollConnection ->
                BestTableScreen(
                    bundle = bundle,
                    activeServer = activeProfile?.server ?: org.rhythmeta.chunithmd.shared.ProfileServer.Jp,
                    records = profileScores,
                    jacketBaseUrl = manifest?.assets?.jacketBaseUrl.orEmpty(),
                    localJacketPath = repository::localJacketPath,
                    contentTopPadding = padding.calculateTopPadding(),
                    topBarScrollConnection = topBarScrollConnection,
                    onOpenSong = { songId -> pushRoute(AppRoute.SongDetail(songId)) },
                    preferencesRepository = bestTablePreferencesRepository,
                    profileName = activeProfile?.name,
                    shareRequested = bestTableShareRequested,
                    onShareRequestHandled = { bestTableShareRequested = false },
                )
            }
        }
        entry<AppRoute.RandomSong>(
            transition = SettingsDetailTransition,
            swipeDismiss = if (predictiveBackEnabled) NavSwipeDirection.LeftToRight else NavSwipeDirection.None,
        ) {
            AppFrame(8, randomSongTopBarScrollBehavior) { padding, topBarScrollConnection ->
                RandomSongScreen(
                    bundle = bundle,
                    jacketBaseUrl = manifest?.assets?.jacketBaseUrl.orEmpty(),
                    localJacketPath = repository::localJacketPath,
                    scores = profileScores,
                    favoriteSongIds = favoriteSongIds,
                    playableRegion = playableRegion,
                    sessionState = randomSongSessionState,
                    filterRequested = randomSongFilterRequested,
                    onFilterRequestHandled = { randomSongFilterRequested = false },
                    onFilterActiveChanged = { randomSongFilterActive = it },
                    contentTopPadding = padding.calculateTopPadding(),
                    topBarScrollConnection = topBarScrollConnection,
                    onOpenSong = { songId -> pushRoute(AppRoute.SongDetail(songId)) },
                )
            }
        }
        entry<AppRoute.Recommendations>(
            transition = SettingsDetailTransition,
            swipeDismiss = if (predictiveBackEnabled) NavSwipeDirection.LeftToRight else NavSwipeDirection.None,
        ) {
            AppFrame(9, recommendationTopBarScrollBehavior) { padding, topBarScrollConnection ->
                RecommendationScreen(
                    bundle = bundle,
                    records = profileScores,
                    activeServer = activeProfile?.server ?: org.rhythmeta.chunithmd.shared.ProfileServer.Jp,
                    preferences = bestTablePreferences,
                    jacketBaseUrl = manifest?.assets?.jacketBaseUrl.orEmpty(),
                    localJacketPath = repository::localJacketPath,
                    contentTopPadding = padding.calculateTopPadding(),
                    topBarScrollConnection = topBarScrollConnection,
                    switcherScrollConnection = recommendationSwitcherScrollConnection,
                    selectedPage = recommendationSelectedPage,
                    onOpenSong = { songId -> pushRoute(AppRoute.SongDetail(songId)) },
                )
            }
        }
        entry<AppRoute.ScoreQuery>(
            transition = SettingsDetailTransition,
            swipeDismiss = if (predictiveBackEnabled) NavSwipeDirection.LeftToRight else NavSwipeDirection.None,
        ) {
            AppFrame(10, scoreQueryTopBarScrollBehavior) { padding, topBarScrollConnection ->
                ScoreQueryScreen(
                    bundle = bundle,
                    activeServer = activeProfile?.server ?: org.rhythmeta.chunithmd.shared.ProfileServer.Jp,
                    records = profileScores,
                    jacketBaseUrl = manifest?.assets?.jacketBaseUrl.orEmpty(),
                    localJacketPath = repository::localJacketPath,
                    contentTopPadding = padding.calculateTopPadding(),
                    searchScrollConnection = scoreQuerySearchScrollConnection,
                    topBarScrollConnection = topBarScrollConnection,
                    searchText = scoreQuerySearch,
                    filterSettings = scoreQueryFilterSettings,
                    displayMode = scoreQueryDisplayMode,
                    sortMode = scoreQuerySortMode,
                    ascending = scoreQueryAscending,
                    filterOpen = scoreQueryFilterOpen,
                    onFilterSettingsChange = { scoreQueryFilterSettings = it },
                    onFilterDismiss = { scoreQueryFilterOpen = false },
                    onOpenSong = { songId -> pushRoute(AppRoute.SongDetail(songId)) },
                )
            }
        }
        entry<AppRoute.ConstantTable>(
            transition = SettingsDetailTransition,
            swipeDismiss = if (predictiveBackEnabled) NavSwipeDirection.LeftToRight else NavSwipeDirection.None,
        ) {
            AppFrame(11, constantTableTopBarScrollBehavior) { padding, topBarScrollConnection ->
                ConstantTableScreen(
                    bundle = bundle,
                    activeServer = activeProfile?.server ?: org.rhythmeta.chunithmd.shared.ProfileServer.Jp,
                    records = profileScores,
                    favoriteSongIds = favoriteSongIds,
                    jacketBaseUrl = manifest?.assets?.jacketBaseUrl.orEmpty(),
                    localJacketPath = repository::localJacketPath,
                    contentTopPadding = padding.calculateTopPadding(),
                    topBarScrollConnection = topBarScrollConnection,
                    filterSettings = constantTableFilterSettings,
                    filterOpen = constantTableFilterOpen,
                    exportRequested = constantTableExportRequested,
                    onFilterSettingsChange = { constantTableFilterSettings = it },
                    onFilterDismiss = { constantTableFilterOpen = false },
                    onExportRequestHandled = { constantTableExportRequested = false },
                    onOpenSong = { songId -> pushRoute(AppRoute.SongDetail(songId)) },
                )
            }
        }
        entry<AppRoute.PlateProgress>(
            transition = SettingsDetailTransition,
            swipeDismiss = if (predictiveBackEnabled) NavSwipeDirection.LeftToRight else NavSwipeDirection.None,
        ) {
            AppFrame(12, plateProgressTopBarScrollBehavior) { padding, topBarScrollConnection ->
                PlateProgressScreen(
                    bundle = bundle,
                    activeServer = activeProfile?.server ?: org.rhythmeta.chunithmd.shared.ProfileServer.Jp,
                    records = profileScores,
                    jacketBaseUrl = manifest?.assets?.jacketBaseUrl.orEmpty(),
                    localJacketPath = repository::localJacketPath,
                    contentTopPadding = padding.calculateTopPadding(),
                    topBarScrollConnection = topBarScrollConnection,
                    onOpenSong = { songId -> pushRoute(AppRoute.SongDetail(songId)) },
                )
            }
        }
        entry<AppRoute.Collections>(
            transition = SettingsDetailTransition,
            swipeDismiss = if (predictiveBackEnabled) NavSwipeDirection.LeftToRight else NavSwipeDirection.None,
        ) {
            AppFrame(13, collectionsTopBarScrollBehavior) { padding, connection ->
                SongCollectionsScreen(
                    repository = collectionRepository,
                    uiState = collectionsUiState,
                    bundle = bundle,
                    activeServer = activeProfile?.server ?: org.rhythmeta.chunithmd.shared.ProfileServer.Jp,
                    jacketBaseUrl = manifest?.assets?.jacketBaseUrl.orEmpty(),
                    localJacketPath = repository::localJacketPath,
                    contentTopPadding = padding.calculateTopPadding(),
                    topBarScrollConnection = connection,
                    onOpenCollection = { pushRoute(AppRoute.CollectionDetail(it)) },
                    onOpenSong = { pushRoute(AppRoute.SongDetail(it)) },
                )
            }
        }
        entry<AppRoute.CollectionDetail>(
            transition = SettingsDetailTransition,
            swipeDismiss = if (predictiveBackEnabled) NavSwipeDirection.LeftToRight else NavSwipeDirection.None,
        ) { route ->
            AppFrame(14, collectionDetailTopBarScrollBehavior, titleOverride = collections.firstOrNull { it.id == route.collectionId }?.name) { padding, connection ->
                SongCollectionsScreen(
                    repository = collectionRepository,
                    uiState = collectionsUiState,
                    collectionId = route.collectionId,
                    bundle = bundle,
                    activeServer = activeProfile?.server ?: org.rhythmeta.chunithmd.shared.ProfileServer.Jp,
                    jacketBaseUrl = manifest?.assets?.jacketBaseUrl.orEmpty(),
                    localJacketPath = repository::localJacketPath,
                    contentTopPadding = padding.calculateTopPadding(),
                    topBarScrollConnection = connection,
                    onOpenCollection = { pushRoute(AppRoute.CollectionDetail(it)) },
                    onOpenSong = { pushRoute(AppRoute.SongDetail(it)) },
                )
            }
        }
        entry<AppRoute.SongDetail>(
            transition = SettingsDetailTransition,
            swipeDismiss = if (predictiveBackEnabled) NavSwipeDirection.LeftToRight else NavSwipeDirection.None,
        ) { route ->
            val song = bundle?.catalog?.songs?.firstOrNull { it.songId == route.songId }
            AppPageScaffold(
                title = song?.let(CatalogSongFormatter::displayTitle) ?: tr("歌曲详情"),
                pageBackground = songDetailBackground ?: pageBackground,
                blurEnabled = enableBlur,
                largeTitle = false,
                topBarScrollBehavior = MiuixScrollBehavior(),
                navigationIcon = {
                    MiuixIconButton(onClick = { navBackStack.removeLastOrNull() }) {
                        MiuixIcon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = tr("返回"))
                    }
                },
                actions = {
                    if (song != null) {
                        val isFavorite = song.songId in favoriteSongIds
                        MiuixIconButton(onClick = {
                            scope.launch { favoriteSongRepository.setFavorite(song.songId, !isFavorite) }
                        }) {
                            MiuixIcon(
                                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = if (isFavorite) tr("取消喜爱") else tr("添加到喜爱"),
                                tint = if (isFavorite) Color(0xFFE85D5D) else MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
            ) { padding, topBarScrollConnection ->
                SongDetailScreen(
                    song = song,
                    loading = bundle == null,
                    aliases = bundle?.aliases?.get(route.songId).orEmpty(),
                    jacketBaseUrl = manifest?.assets?.jacketBaseUrl.orEmpty(),
                    localJacketPath = repository::localJacketPath,
                    contentTopPadding = padding.calculateTopPadding(),
                    topBarScrollConnection = topBarScrollConnection,
                    communityStore = communityStore,
                    onOpenCommunity = { pushRoute(AppRoute.CommunityAliases) },
                    onLogin = { pushRoute(AppRoute.Account) },
                    onBackgroundChanged = { songDetailBackground = it },
                    scoreRepository = scoreRepository,
                    collectionRepository = collectionRepository,
                )
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
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .onSizeChanged { navigationBarHeightPx = it.height },
                )
            }
        }
    }
    }

    if (bundle != null) {
        CatalogFilterDialog(
            show = filterOpen,
            bundle = bundle,
            settings = filters,
            onSettingsChange = {
                filters = it
                scope.launch { catalogPreferencesRepository.setFilters(it) }
            },
            onDismiss = { filterOpen = false },
        )
    }

    ProfileEditorSheet(
        visible = quickEditProfile != null,
        profile = quickEditProfile,
        repository = profileRepository,
        avatarStore = profileAvatarStore,
        onDismiss = { quickEditProfile = null },
    )

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
        LiquidGlassTab(Icons.Rounded.Home, tr("主页")),
        LiquidGlassTab(Icons.Rounded.DocumentScanner, tr("扫描")),
        LiquidGlassTab(Icons.Rounded.Search, tr("歌曲")),
        LiquidGlassTab(Icons.Rounded.Settings, tr("设置")),
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
private fun BlankDestination(modifier: Modifier) {
    Box(
        modifier.fillMaxSize().background(MiuixTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        top.yukonga.miuix.kmp.basic.Text(
            "Working in progress",
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}
