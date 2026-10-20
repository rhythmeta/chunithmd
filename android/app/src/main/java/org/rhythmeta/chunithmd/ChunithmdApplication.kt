package org.rhythmeta.chunithmd

import android.app.Application
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.rhythmeta.chunithmd.diagnostics.CrashLogStore

class ChunithmdApplication : Application() {
    internal val crashLogStore by lazy { CrashLogStore(this) }
    internal val otogameClient by lazy { org.rhythmeta.chunithmd.shared.importing.OtogameClient() }
    private val updateScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
    internal val appUpdateRepository by lazy {
        val context = applicationContext
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        org.rhythmeta.chunithmd.core.update.AppUpdateRepository(
            remote = org.rhythmeta.chunithmd.core.update.AppUpdateClient(BuildConfig.UPDATE_URL, json),
            store = org.rhythmeta.chunithmd.core.update.AppUpdateStateStore(context, json),
            downloader = org.rhythmeta.chunithmd.core.update.AndroidAppUpdateDownloader(context),
            verifier = org.rhythmeta.chunithmd.core.update.AndroidAppUpdateVerifier(context),
            scope = updateScope,
            updateUrl = BuildConfig.UPDATE_URL,
            packageName = context.packageName,
            installedCode = BuildConfig.VERSION_CODE.toLong(),
            installedSha = BuildConfig.GIT_SHA,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            sdk = Build.VERSION.SDK_INT,
        )
    }

    internal val divingFishClient by lazy {
        org.rhythmeta.chunithmd.shared.importing.DivingFishClient(
            org.rhythmeta.chunithmd.account.AndroidSecretStore(this),
        )
    }

    internal val lxnsClient by lazy {
        org.rhythmeta.chunithmd.shared.importing.LxnsClient(
            org.rhythmeta.chunithmd.account.AndroidSecretStore(this),
        )
    }

    override fun onCreate() {
        super.onCreate()
        crashLogStore.install()
    }
}
