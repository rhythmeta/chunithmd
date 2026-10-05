package org.rhythmeta.chunithmd.ui.settings

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.concurrent.atomic.AtomicBoolean
import org.rhythmeta.chunithmd.shared.importing.OtogameClient
import org.rhythmeta.chunithmd.shared.importing.OtogameImportController
import org.rhythmeta.chunithmd.shared.localization.tr
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun OtogameLoginScreen(controller: OtogameImportController, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val state by controller.state.collectAsStateWithLifecycle()
    var webView by remember { mutableStateOf<WebView?>(null) }
    val profileId = state.profileId
    Column(modifier.fillMaxSize()) {
        if (state.eligible && profileId != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (state.connected) tr("已连接") else tr("请先登录 Otogame"), modifier = Modifier.weight(1f),
                    style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                IconButton(onClick = { webView?.reload() }) { Icon(Icons.Rounded.Refresh, tr("刷新")) }
                TextButton(tr("完成"), onClick = onDone)
            }
            key(profileId) {
                OtogameWebView(Modifier.fillMaxWidth().weight(1f),
                    onAuthorization = { controller.captureAuthorization(profileId, it) },
                    onView = { webView = it })
            }
        } else {
            Text(tr("需要启用一个日服档案。"), modifier = Modifier.padding(24.dp))
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun OtogameWebView(modifier: Modifier, onAuthorization: (String) -> Unit, onView: (WebView?) -> Unit) {
    val released = remember { AtomicBoolean(false) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.javaScriptCanOpenWindowsAutomatically = false
                settings.setSupportMultipleWindows(false)
                CookieManager.getInstance().setAcceptCookie(true)
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean =
                        request.url.scheme !in setOf("https", "http")

                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
                        val uri = request.url
                        if (uri.scheme == "https" && uri.host.equals("u.otogame.net", true) &&
                            uri.port in listOf(-1, 443) && uri.path?.startsWith("/api/") == true) {
                            request.requestHeaders.entries.firstOrNull { it.key.equals("Authorization", true) }?.value?.let { header ->
                                view?.post { if (!released.get()) onAuthorization(header) }
                            }
                        }
                        return null
                    }
                }
                onView(this)
                loadUrl(OtogameClient.MUSIC_URL)
            }
        },
        onRelease = { view ->
            released.set(true)
            onView(null)
            view.stopLoading()
            view.destroy()
        },
    )
}
