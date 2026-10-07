package org.rhythmeta.chunithmd.shared.scanner

/** Show the first valid chart immediately; stabilize subsequent changes by chart identity, not OCR spelling. */
class LiveScanStabilizer {
    private data class Identity(val chartKey: String, val score: Int)
    private var locked: Identity? = null
    private var pending: Identity? = null
    private var unstableFrames = 0
    val shouldClear: Boolean get() = unstableFrames >= 3

    fun accept(review: ScanReview): Boolean {
        val match = ScoreScanner.automaticMatch(review)
        val score = review.parsedScore
        if (match == null || score == null) {
            pending = null
            miss()
            return false
        }
        val identity = Identity(match.key, score)
        val accepted = locked == null || identity == locked || identity == pending
        pending = identity
        if (accepted) {
            locked = identity
            unstableFrames = 0
        } else {
            miss()
        }
        return accepted
    }

    private fun miss() {
        unstableFrames++
        if (shouldClear) locked = null
    }

    fun reset() { locked = null; pending = null; unstableFrames = 0 }
}
