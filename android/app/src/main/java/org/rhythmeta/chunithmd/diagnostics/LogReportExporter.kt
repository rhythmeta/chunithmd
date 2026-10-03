package org.rhythmeta.chunithmd.diagnostics

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Process
import androidx.core.content.FileProvider
import java.io.File
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.rhythmeta.chunithmd.BuildConfig
import org.rhythmeta.chunithmd.shared.diagnostics.DiagnosticLogRedactor

internal object LogReportExporter {
    suspend fun export(context: Context, crashLogStore: CrashLogStore): Uri = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "diagnostic-logs").apply { mkdirs() }
        directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > TimeUnit.DAYS.toMillis(7) }
            ?.forEach { it.delete() }
        val file = File.createTempFile("chunithmd-logs-", ".txt", directory)
        val report = buildString {
            appendLine("chunithmd diagnostic log")
            appendLine("Generated: ${Instant.now()}")
            appendLine("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine("Process: ${Process.myPid()}")
            appendLine()
            appendLine("== previous crash ==")
            appendLine(crashLogStore.read() ?: "No previous crash report")
            appendLine()
            appendLine("== app logcat ==")
            appendLine(readRecentLogcat(context))
        }
        file.writeText(DiagnosticLogRedactor.redact(report))
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun readRecentLogcat(context: Context): String = runCatching {
        val output = File.createTempFile("logcat-", ".tmp", context.cacheDir)
        try {
            val process = ProcessBuilder(
                "logcat", "-d", "-v", "threadtime", "--pid=${Process.myPid()}", "-t", "4000",
            ).redirectErrorStream(true).redirectOutput(output).start()
            try {
                if (process.waitFor(5, TimeUnit.SECONDS)) output.readText().ifBlank { "No recent app logs" }
                else "Unable to read logcat: timed out"
            } finally {
                if (process.isAlive) process.destroyForcibly()
            }
        } finally {
            output.delete()
        }
    }.getOrElse { "Unable to read logcat: ${it::class.simpleName}: ${it.message}" }
}
