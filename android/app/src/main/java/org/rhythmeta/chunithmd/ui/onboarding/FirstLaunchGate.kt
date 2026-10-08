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
    LaunchedEffect(completed, catalog.localLoaded, catalog.bundle != null) {
        if (catalog.localLoaded && catalog.bundle != null && completed == false) preferences.complete()
        // A completed installation may have had its resource cache removed.
        if (catalog.localLoaded && completed == true && catalog.bundle == null) catalog.refresh(repository)
    }
    if (completed == null || !catalog.localLoaded) {
        Box(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (OnboardingPolicy.needsOnboarding(completed == true, catalog.bundle != null)) {
        FirstLaunchScreen(catalog.sync, catalog.isRefreshing, catalog.error) { catalog.refresh(repository) }
    } else {
        content()
    }
}
