package org.rhythmeta.chunithmd.shared.scanner

import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.rhythmeta.chunithmd.shared.CatalogBundle
import org.rhythmeta.chunithmd.shared.CatalogJson

@Serializable
private data class ScanPresentation(val review: ScanReview, val match: ScanChartCandidate?, val accepted: Boolean, val shouldClear: Boolean)

/** Swift uses the same automatic matching and temporal confirmation as the Android camera. */
class NativeLiveScoreScanner {
    private val stabilizer = LiveScanStabilizer()
    private val mutex = Mutex()
    private var session = ""

    @Throws(Exception::class)
    suspend fun reviewJson(catalog: CatalogBundle, observationsJson: String, region: String, live: Boolean, session: String): String = withContext(Dispatchers.Default) { mutex.withLock {
        if (this@NativeLiveScoreScanner.session != session) {
            stabilizer.reset()
            this@NativeLiveScoreScanner.session = session
        }
        val observations = CatalogJson.codec.decodeFromString<List<ScanObservation>>(observationsJson)
        val review = ScoreScanner.review(catalog, ScoreScanner.fields(observations), region)
        val match = ScoreScanner.automaticMatch(review)
        val accepted = if (live) stabilizer.accept(review) else match != null && review.parsedScore != null
        CatalogJson.codec.encodeToString(ScanPresentation(review, match, accepted, live && stabilizer.shouldClear))
    } }
}
