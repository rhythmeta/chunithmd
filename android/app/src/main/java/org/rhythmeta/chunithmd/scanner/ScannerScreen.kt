package org.rhythmeta.chunithmd.scanner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import org.rhythmeta.chunithmd.profile.ProfileRepository
import org.rhythmeta.chunithmd.score.ScoreRepository
import org.rhythmeta.chunithmd.shared.*
import org.rhythmeta.chunithmd.shared.localization.tr
import org.rhythmeta.chunithmd.shared.scanner.ScoreScanner
import org.rhythmeta.chunithmd.ui.catalog.ScoreEntrySheet
import org.rhythmeta.chunithmd.ui.components.SquircleExtension
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleSurface

/** maimaid's camera-first scanner, with CHUNITHM fields reviewed before saving. */
@Composable
fun ScannerScreen(
    catalog: CatalogBundle?, profile: UserProfile?, profiles: ProfileRepository,
    scores: ScoreRepository, enabled: Boolean, bottomPadding: Dp,
    showBoxes: Boolean, onOpenSong: (String) -> Unit,
    jacketBaseUrl: String, localJacketPath: (String) -> String?,
    modifier: Modifier = Modifier, model: ScannerViewModel = viewModel(),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val state by model.state.collectAsStateWithLifecycle()
    val modelState by model.models.state.collectAsStateWithLifecycle()
    val matchedSong = catalog?.catalog?.songs?.firstOrNull { it.songId == state.match?.songId }
    val matchedSheet = matchedSong?.sheets?.firstOrNull { matchedSong.sheetKey(it) == state.match?.key }
    val recordsFlow = remember(scores, matchedSong?.songId) {
        matchedSong?.let { scores.observeSongRecords(it.songId) } ?: flowOf(emptyList())
    }
    val records by recordsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    // Navigation entries may be capped at STARTED even while the Activity is RESUMED.
    // Use the Activity for foreground gating; `enabled` already tracks the selected tab.
    val foregroundLifecycle = (activity as? LifecycleOwner)?.lifecycle ?: LocalLifecycleOwner.current.lifecycle
    val lifecycleState by foregroundLifecycle.currentStateFlow.collectAsState()
    val resumed = enabled && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val controller = remember { ScannerCameraController() }
    var permissionGranted by remember { mutableStateOf(false) }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var capturing by remember { mutableStateOf(false) }
    val ready = catalog != null && profile != null && modelState.usable
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted = it }
    val photos = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let(model::recognize) }
    LaunchedEffect(catalog, profile) { model.bind(catalog, profile) }
    LaunchedEffect(resumed, modelState.usable) {
        model.setActive(resumed && modelState.usable)
        if (resumed && modelState.usable) {
            permissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted && !permissionRequested) {
                permissionRequested = true
                permission.launch(Manifest.permission.CAMERA)
            }
        }
    }
    LaunchedEffect(enabled) { if (!enabled) model.resumeLive() }
    LaunchedEffect(message) { if (message != null) { delay(2500); message = null } }
    LaunchedEffect(state.saved) { if (state.saved) message = tr("已保存") }
    DisposableEffect(Unit) { onDispose { model.setActive(false); model.resumeLive() } }
    DisposableEffect(enabled) {
        val window = (context as? Activity)?.window
        val bars = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        val previous = bars?.isAppearanceLightStatusBars
        if (enabled) bars?.isAppearanceLightStatusBars = false
        onDispose { if (enabled && previous != null) bars.isAppearanceLightStatusBars = previous }
    }
    BackHandler(enabled = enabled && state.image != null && !state.reviewVisible && !state.saving) { model.resumeLive() }
    Box(modifier.fillMaxSize().background(Color.Black)) {
        if (!modelState.usable) {
            ScannerModelDownloadPanel(modelState, model.models, Modifier.align(Alignment.Center).padding(24.dp))
        } else if (state.image != null) {
            AsyncImage(state.image, contentDescription = tr("成绩图"), contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize())
        } else if (permissionGranted) {
            ScannerCameraPreview(
                enabled = resumed, analyzing = ready && !state.reviewVisible && !state.saving,
                isProcessingFrame = { model.isProcessingFrame }, onLandscapeChanged = model::setLandscape,
                controller = controller, onFrame = model::analyzeLiveFrame, onError = { cameraError = it },
                modifier = Modifier.fillMaxSize(),
            )
            if (showBoxes) ScannerDebugOverlay(state, Modifier.fillMaxSize())
        } else {
            Column(Modifier.align(Alignment.Center).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.CameraAlt, null, tint = Color.White, modifier = Modifier.size(48.dp))
                Text(tr("允许访问相机以实时识别成绩"), color = Color.White)
                Button(onClick = { permission.launch(Manifest.permission.CAMERA) }, colors = ButtonDefaults.buttonColorsPrimary()) {
                    Text(tr("允许访问相机"))
                }
                TextButton(text = tr("打开设置"), onClick = {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                })
            }
        }
        if (modelState.usable && modelState.stage != "ready") {
            ScannerModelDownloadPanel(modelState, model.models, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 64.dp, start = 24.dp, end = 24.dp), compact = true)
        }
        Row(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 8.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.image != null) ScannerOverlayButton(Icons.Rounded.Close, tr("返回实时扫描"), !state.saving, model::resumeLive)
            ScannerOverlayButton(Icons.Rounded.PhotoLibrary, tr("选择成绩图"), ready && !state.busy && !state.saving, {
                photos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            })
        }
        val status = when {
            !modelState.usable -> null
            state.busy -> tr("正在识别…")
            message != null -> message
            state.error != null -> state.error
            !ready -> tr("请先加载曲库并选择玩家档案。")
            state.image == null && cameraError != null -> cameraError
            else -> null
        }
        if (status != null) Row(
            Modifier.align(Alignment.Center).padding(horizontal = 28.dp)
                .squircleSurface(Color.Black.copy(alpha = .65f), 20.dp, SquircleExtension)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.busy) CircularProgressIndicator(size = 18.dp, strokeWidth = 2.dp)
            Text(status, color = Color.White, modifier = Modifier.weight(1f, fill = false))
        }
        Column(Modifier.align(Alignment.BottomCenter).widthIn(max = 560.dp).fillMaxWidth().padding(bottom = bottomPadding + 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedVisibility(state.match != null && state.image == null && cameraError == null,
                enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                val captureLabel = tr("保存成绩照片")
                IconButton(onClick = {
                    capturing = true
                    controller.capture(context, state.fields?.title.orEmpty()) { saved ->
                        capturing = false
                        message = tr(if (saved) "照片已保存" else "照片保存失败")
                    }
                }, enabled = !capturing && !state.reviewVisible,
                    modifier = Modifier.padding(bottom = 12.dp).size(64.dp)
                        .semantics { contentDescription = captureLabel }
                        .squircleBorder(3.dp, Color.White, 32.dp, SquircleExtension)) {
                    Box(Modifier.size(52.dp).squircleSurface(Color.White, 26.dp, SquircleExtension))
                    if (capturing) CircularProgressIndicator(size = 22.dp, strokeWidth = 2.dp)
                }
            }
            AnimatedVisibility(state.songMatch != null,
                enter = scaleIn(initialScale = .9f) + fadeIn(), exit = scaleOut(targetScale = .95f) + fadeOut()) {
                state.songMatch?.let { song ->
                    ScannerSongCard(song, catalog, jacketBaseUrl, localJacketPath) {
                        model.setActive(false)
                        onOpenSong(song.songId)
                    }
                }
            }
            AnimatedVisibility(state.match != null,
                enter = scaleIn(initialScale = .9f) + fadeIn(), exit = scaleOut(targetScale = .95f) + fadeOut()) {
                ScannerResultCard(state, catalog, jacketBaseUrl, localJacketPath, model::openReview)
            }
        }
    }
    if (matchedSong != null && matchedSheet != null && profile != null) {
        ScoreEntrySheet(
            visible = state.reviewVisible,
            song = matchedSong,
            sheet = matchedSong.sheetForServer(matchedSheet, profile.server),
            bestRecord = records.filter { it.sheetKey == state.match?.key }.bestScoreSummary(),
            saving = state.saving,
            initialScore = ScoreScanner.parseScore(state.fields?.score.orEmpty()),
            initialClear = ClearType.fromWire(state.clear) ?: ClearType.Clear,
            initialFullCombo = FullComboType.fromWire(state.combo),
            errorMessage = state.error,
            onInputChanged = model::markInputChanged,
            onSave = { score, clear, combo, chain -> model.save(score, clear, combo, chain, profiles, scores) },
            onDismiss = model::dismissReview,
        )
    }
}

@Composable
private fun ScannerOverlayButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String,
    enabled: Boolean, onClick: () -> Unit, selected: Boolean = false) {
    IconButton(onClick = onClick, enabled = enabled,
        modifier = Modifier.squircleSurface(Color.Black.copy(alpha = .32f), 22.dp, SquircleExtension)) {
        Icon(icon, label, tint = if (selected) Color(0xFFFFD60A) else Color.White.copy(alpha = if (enabled) 1f else .4f))
    }
}
