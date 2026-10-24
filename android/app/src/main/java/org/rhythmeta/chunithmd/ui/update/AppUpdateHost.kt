package org.rhythmeta.chunithmd.ui.update

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri
import org.rhythmeta.chunithmd.BuildConfig
import org.rhythmeta.chunithmd.core.update.ApkInstaller
import org.rhythmeta.chunithmd.core.update.AppUpdatePhase
import org.rhythmeta.chunithmd.core.update.AppUpdatePolicy
import org.rhythmeta.chunithmd.shared.localization.tr

@Composable
fun AppUpdateHost(viewModel: AppUpdateViewModel, blocked: Boolean) {
    val context = LocalContext.current
    val installer = remember(context) { ApkInstaller(context) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val requested by viewModel.dialogRequested.collectAsStateWithLifecycle()
    val manualRequest by viewModel.manualDialogRequested.collectAsStateWithLifecycle()
    val shouldInstall by viewModel.shouldInstall.collectAsStateWithLifecycle()
    var canInstall by remember { mutableStateOf(installer.canInstall()) }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val resumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val installation = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        canInstall = installer.canInstall()
        if (canInstall) viewModel.install { file -> installation.launch(installer.installIntent(file)) }
    }
    LaunchedEffect(viewModel, resumed) {
        if (resumed) canInstall = installer.canInstall()
        viewModel.setForeground(resumed)
    }
    LaunchedEffect(viewModel) { viewModel.check(automatic = true) }
    LaunchedEffect(state.phase, shouldInstall, resumed, blocked, requested) {
        if (state.phase == AppUpdatePhase.Ready && shouldInstall && resumed && !blocked && requested) {
            viewModel.consumeInstallRequest()
            if (canInstall) viewModel.install { file -> installation.launch(installer.installIntent(file)) }
        }
    }
    AppUpdateDialog(
        show = requested && (manualRequest || (!blocked && resumed)),
        state = state,
        canInstall = canInstall,
        onDismiss = viewModel::dismiss,
        onShare = {
            state.artifact?.let { artifact ->
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, artifact.url)
                }
                context.startActivity(Intent.createChooser(sendIntent, tr("分享 APK 下载链接")))
            }
        },
        onDownload = viewModel::download,
        onCancel = viewModel::cancel,
        onCheck = { viewModel.check() },
        onInstall = {
            if (installer.canInstall()) viewModel.install { file -> installation.launch(installer.installIntent(file)) }
            else runCatching { permission.launch(installer.permissionIntent()) }.onFailure { viewModel.installationUnavailable() }
        },
        onViewChanges = { state.manifest?.let { manifest ->
            context.startActivity(Intent(Intent.ACTION_VIEW,
                AppUpdatePolicy.compareUrl(BuildConfig.GIT_SHA, manifest.sha).toUri()))
        } },
    )
}
