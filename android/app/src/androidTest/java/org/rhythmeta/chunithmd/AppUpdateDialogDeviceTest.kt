package org.rhythmeta.chunithmd

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.rhythmeta.chunithmd.core.update.AppUpdateArtifact
import org.rhythmeta.chunithmd.core.update.AppUpdateChanges
import org.rhythmeta.chunithmd.core.update.AppUpdateCommit
import org.rhythmeta.chunithmd.core.update.AppUpdateManifest
import org.rhythmeta.chunithmd.core.update.AppUpdatePhase
import org.rhythmeta.chunithmd.core.update.AppUpdateState
import org.rhythmeta.chunithmd.shared.localization.AppLanguage
import org.rhythmeta.chunithmd.shared.localization.AppStrings
import org.rhythmeta.chunithmd.shared.localization.tr
import org.rhythmeta.chunithmd.ui.theme.AppThemeSettings
import org.rhythmeta.chunithmd.ui.theme.ChunithmdTheme
import org.rhythmeta.chunithmd.ui.theme.ColorMode
import org.rhythmeta.chunithmd.ui.update.AppUpdateDialog
import top.yukonga.miuix.kmp.basic.Scaffold

class AppUpdateDialogDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun localizedDialogsKeepShareAndDownloadActionsVisible() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = mutableStateOf(Configuration(context.resources.configuration))
        val localeTag = mutableStateOf("zh-Hans")
        val dark = mutableStateOf(false)
        compose.setContent {
            key(localeTag.value, dark.value) {
                AppStrings.language = AppLanguage.fromTag(localeTag.value) ?: AppLanguage.English
                val localizedContext = context.createConfigurationContext(configuration.value)
                CompositionLocalProvider(
                    LocalContext provides localizedContext,
                    LocalConfiguration provides configuration.value,
                ) {
                    ChunithmdTheme(AppThemeSettings(colorMode = if (dark.value) ColorMode.DARK else ColorMode.LIGHT)) {
                        Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) {
                            Box(Modifier.fillMaxSize()) {
                                AppUpdateDialog(
                                    show = true,
                                    state = fixture(),
                                    canInstall = true,
                                    onDismiss = {},
                                    onShare = {},
                                    onDownload = {},
                                    onCancel = {},
                                    onCheck = {},
                                    onInstall = {},
                                    onViewChanges = {},
                                )
                            }
                        }
                    }
                }
            }
        }

        for (tag in listOf("zh-Hans", "zh-Hant", "en", "ja")) {
            for (darkMode in listOf(false, true)) {
                compose.runOnIdle {
                    localeTag.value = tag
                    dark.value = darkMode
                    configuration.value = Configuration(context.resources.configuration).apply {
                        setLocales(LocaleList.forLanguageTags(tag))
                    }
                }
                compose.waitForIdle()
                val language = AppLanguage.fromTag(tag) ?: AppLanguage.English
                compose.onNodeWithText(AppStrings.textFor(language, "发现新 CI 构建")).assertIsDisplayed()
                compose.onNodeWithText("${BuildConfig.VERSION_CODE} → ${BuildConfig.VERSION_CODE + 2}").assertIsDisplayed()
                compose.onNodeWithText(AppStrings.textFor(language, "分享")).assertIsDisplayed()
                compose.onNodeWithText(AppStrings.textFor(language, "下载更新")).assertIsDisplayed()
                compose.onNodeWithText(AppStrings.textFor(language, "查看完整变更")).performScrollTo().assertIsDisplayed()
                compose.onNodeWithText(AppStrings.textFor(language, "下载更新")).assertIsDisplayed()
            }
        }
    }

    private fun fixture(): AppUpdateState {
        val sha = "b".repeat(40)
        val artifact = AppUpdateArtifact(
            abi = "arm64-v8a",
            url = "https://assets.rhythmeta.org/android/chunithmd/ci/builds/1-1/chunithmd-arm64-v8a.apk",
            size = 12_345,
            sha256 = "a".repeat(64),
        )
        val manifest = AppUpdateManifest(
            schemaVersion = 1,
            packageName = BuildConfig.APPLICATION_ID,
            versionName = "0.4.1",
            versionCode = BuildConfig.VERSION_CODE.toLong() + 2,
            minSdk = 29,
            branch = "main",
            sha = sha,
            author = "Rhythmeta",
            builtAt = "2026-10-20T10:00:00Z",
            historyUrl = "https://assets.rhythmeta.org/android/chunithmd/ci/builds/1-1/history.json",
            artifacts = listOf(artifact),
        )
        val commits = (1..20).map { index ->
            AppUpdateCommit(
                sha = index.toString(16).padStart(40, '0'),
                parents = emptyList(),
                title = "feat(android): support long update change descriptions and wrapping #$index",
                author = "Rhythmeta",
            )
        }
        return AppUpdateState(AppUpdatePhase.Available, manifest, artifact, AppUpdateChanges(commits, incomplete = true))
    }
}
