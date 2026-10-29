package org.rhythmeta.chunithmd.shared

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.rhythmeta.chunithmd.shared.localization.tr

internal fun formatCatalogBytes(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.coerceAtLeast(0L).toDouble()
    var unit = 0
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit += 1
    }
    val rounded = if (unit == 0) value.toLong().toString() else {
        val tenths = (value * 10).toLong()
        "${tenths / 10}.${tenths % 10}"
    }
    return "$rounded ${units[unit]}"
}

internal class DownloadSpeedTracker {
    private data class Sample(val timestampMillis: Long, val downloadedBytes: Long)

    private val samples = ArrayDeque<Sample>()
    private val startedAt = kotlin.time.TimeSource.Monotonic.markNow()
    private var downloadedBytes = 0L

    fun addBytes(byteCount: Long): Long {
        val now = startedAt.elapsedNow().inWholeMilliseconds
        downloadedBytes += byteCount.coerceAtLeast(0L)
        samples.addLast(Sample(now, downloadedBytes))
        while (samples.size > 2 && samples[1].timestampMillis <= samples.last().timestampMillis - 2_000L) {
            samples.removeFirst()
        }
        val oldest = samples.first()
        val elapsedMillis = samples.last().timestampMillis - oldest.timestampMillis
        return if (elapsedMillis > 0L) {
            ((downloadedBytes - oldest.downloadedBytes) * 1_000L / elapsedMillis).coerceAtLeast(0L)
        } else {
            0L
        }
    }
}

/**
 * Publishes a single download stage as a [CatalogSyncState].
 *
 * The reporter is driven from several threads: item counters come from the coroutine that owns the
 * stage, while byte counters come from the download workers, hence the synchronized accessors.
 *
 * @property baselineBytes bytes that were already on disk before this stage started. The reported
 * [CatalogSyncState.downloadedBytes] is always `baselineBytes + <bytes moved in this stage>`, so the
 * counter keeps climbing across stages instead of restarting.
 * @property expectedTotalBytes a trusted denominator for the whole first-run download, taken from
 * the static manifest. Without it a multi-item stage reports no total at all rather than a sum that
 * grows while the download runs.
 */
internal class DownloadProgressReporter(
    private val stage: CatalogSyncStage,
    private val totalItems: Int,
    private val message: String,
    private val progressByBytes: Boolean = false,
    private val baselineBytes: Long = 0L,
    private val expectedTotalBytes: Long? = null,
    private val overallStart: Float = 0f,
    private val overallEnd: Float = 1f,
    private val onState: (CatalogSyncState) -> Unit,
) {
    private val mutex = Mutex()
    private val speedTracker = DownloadSpeedTracker()
    private var completedItems = 0
    private var downloadedBytes = 0L
    private var stageExpectedBytes = 0L
    private var bytesPerSecond = 0L

    suspend fun start() = publish()

    suspend fun onTransfer(byteCount: Long, expectedBytes: Long?) = mutex.withLock {
        if (expectedBytes != null) stageExpectedBytes += expectedBytes.coerceAtLeast(0L)
        downloadedBytes += byteCount.coerceAtLeast(0L)
        bytesPerSecond = speedTracker.addBytes(byteCount)
        publishLocked()
    }

    suspend fun onItemCompleted() = mutex.withLock {
        completedItems += 1
        publishLocked()
    }

    suspend fun complete() = mutex.withLock {
        completedItems = totalItems
        publishLocked(progress = overallEnd)
    }

    /** Everything accounted for up to the end of this stage: the baseline plus the bytes moved. */
    suspend fun transferredBytes(): Long = mutex.withLock { baselineBytes + downloadedBytes }

    private suspend fun publish() = mutex.withLock { publishLocked() }

    private fun publishLocked(progress: Float? = calculatedProgress()) {
        val totalBytes = publishedTotalBytes()
        onState(
            CatalogSyncState(
                stage = stage,
                message = progressMessage(totalBytes),
                progress = progress,
                completedItems = completedItems,
                totalItems = totalItems,
                downloadedBytes = reportedBytes(totalBytes),
                totalBytes = totalBytes,
                bytesPerSecond = bytesPerSecond,
            ),
        )
    }

    /**
     * Only a manifest-provided total, or a single-response stage's `Content-Length`, is trustworthy.
     * The running sum of the responses started so far is not a denominator: it grows while the
     * download runs, which makes the reported total jump around instead of describing the download.
     */
    private fun publishedTotalBytes(): Long? =
        expectedTotalBytes ?: stageExpectedBytes.takeIf { progressByBytes && it > 0L }

    private fun reportedBytes(totalBytes: Long?): Long =
        (baselineBytes + downloadedBytes).let { bytes -> totalBytes?.let(bytes::coerceAtMost) ?: bytes }

    private fun progressMessage(totalBytes: Long?): String {
        val speed = if (bytesPerSecond > 0L) "${formatCatalogBytes(bytesPerSecond)}/s" else tr("计算速度中")
        val bytes = totalBytes
            ?.let { "${formatCatalogBytes(reportedBytes(it))} / ${formatCatalogBytes(it)}" }
            ?: formatCatalogBytes(baselineBytes + downloadedBytes)
        return "$message ($completedItems/$totalItems) · $bytes · $speed"
    }

    private fun calculatedProgress(): Float? = when {
        progressByBytes && stageExpectedBytes > 0L -> overallProgress(downloadedBytes.toFloat() / stageExpectedBytes)
        totalItems > 0 -> overallProgress(completedItems.toFloat() / totalItems)
        else -> null
    }

    private fun overallProgress(stageProgress: Float): Float =
        (overallStart + (overallEnd - overallStart) * stageProgress.coerceIn(0f, 1f)).coerceIn(0f, 1f)
}
