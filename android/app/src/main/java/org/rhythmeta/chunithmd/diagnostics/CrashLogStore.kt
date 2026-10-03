package org.rhythmeta.chunithmd.diagnostics

import android.content.Context
import java.time.Instant
import org.rhythmeta.chunithmd.BuildConfig
import org.rhythmeta.chunithmd.shared.diagnostics.DiagnosticLogRedactor

internal class CrashLogStore(context: Context) {
    private val file = context.applicationContext.noBackupFilesDir.resolve("last-crash.txt")

    fun install() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                runCatching {
                    file.writeText(DiagnosticLogRedactor.redact(buildString {
                        appendLine("chunithmd crash report")
                        appendLine("Time: ${Instant.now()}")
                        appendLine("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                        appendLine("Thread: ${thread.name}")
                        appendLine()
                        append(throwable.stackTraceToString())
                    }))
                }
            } finally {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    fun read(): String? = file.takeIf { it.isFile }?.runCatching { readText() }?.getOrNull()
}
