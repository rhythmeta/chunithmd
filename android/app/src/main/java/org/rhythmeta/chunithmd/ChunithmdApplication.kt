package org.rhythmeta.chunithmd

import android.app.Application
import org.rhythmeta.chunithmd.diagnostics.CrashLogStore

class ChunithmdApplication : Application() {
    internal val crashLogStore by lazy { CrashLogStore(this) }
    internal val otogameClient by lazy { org.rhythmeta.chunithmd.shared.importing.OtogameClient() }

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
