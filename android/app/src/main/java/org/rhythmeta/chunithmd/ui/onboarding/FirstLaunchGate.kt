package org.rhythmeta.chunithmd.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.rhythmeta.chunithmd.CatalogStateViewModel
import org.rhythmeta.chunithmd.shared.CatalogRepository
import org.rhythmeta.chunithmd.shared.OnboardingPolicy
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun FirstLaunchGate(
    catalog: CatalogStateViewModel,
    repository: CatalogRepository,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { OnboardingPreferences(context) }
    val completed by preferences.completed.collectAsState(initial = null)
    LaunchedEffect(repository, catalog) { catalog.loadLocal(repository) }
    LaunchedEffect(catalog.localLoaded, catalog.bundle != null) {
        if (!catalog.localLoaded) return@LaunchedEffect
        // Resolve the whole-download size up front so onboarding can state it before the tap.
        if (catalog.bundle == null) catalog.prefetchDownloadSize(repository)
        // The catalog alone is enough to enter the app; jackets keep downloading in the background,
        // which also resumes a pass that a previous process left unfinished.
        if (catalog.bundle != null) catalog.startImageSync(context)
    }
    LaunchedEffect(completed, catalog.localLoaded, catalog.bundle != null) {
        if (catalog.localLoaded && catalog.bundle != null && completed == false) preferences.complete()
        // A completed installation may have had its resource cache removed.
        if (catalog.localLoaded && completed == true && catalog.bundle == null) catalog.refresh(context, repository)
    }
    if (completed == null || !catalog.localLoaded) {
        Box(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (OnboardingPolicy.needsOnboarding(completed == true, catalog.bundle != null)) {
        FirstLaunchScreen(
            sync = catalog.sync,
            downloading = catalog.isRefreshing,
            error = catalog.error,
            totalBytes = catalog.downloadTotalBytes,
        ) { catalog.refresh(context, repository) }
    } else {
        content()
    }
}
