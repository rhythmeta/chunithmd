package org.rhythmeta.chunithmd.ui.components

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

fun Context.openInAppBrowser(url: String): Boolean {
    val uri = url.toUri()
    return uri.scheme in setOf("http", "https") && runCatching {
        CustomTabsIntent.Builder()
            .setColorScheme(CustomTabsIntent.COLOR_SCHEME_SYSTEM)
            .setShowTitle(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .build()
            .launchUrl(this, uri)
    }.isSuccess
}
