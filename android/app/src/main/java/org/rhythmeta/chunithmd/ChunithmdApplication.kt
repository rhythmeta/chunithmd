package org.rhythmeta.chunithmd

import android.app.Application
import org.rhythmeta.chunithmd.diagnostics.CrashLogStore

class ChunithmdApplication : Application() {
    internal val crashLogStore by lazy { CrashLogStore(this) }

    override fun onCreate() {
        super.onCreate()
        crashLogStore.install()
    }
}
