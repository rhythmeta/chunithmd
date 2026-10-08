package org.rhythmeta.chunithmd.shared.scanner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import org.rhythmeta.chunithmd.shared.*

@Serializable
data class ScanSongMatch(val songId: String, val title: String, val similarity: Double)

/** Song lookup never guesses a difficulty or fabricates a score from a title-only detection. */
object SongScanner {
    fun match(catalog: CatalogBundle, title: String, region: String): ScanSongMatch? {
        val key = ScoreScanner.titleKey(title)
        if (key.isEmpty()) return null
        val ranked = catalog.catalog.songs.filter { it.isPlayableIn(region) }.map { song ->
            val similarity = (listOf(song.title) + catalog.aliases[song.songId].orEmpty()).maxOf {
                ScoreScanner.similarity(key, ScoreScanner.titleKey(it))
            }
            ScanSongMatch(song.songId, song.title, similarity)
        }.sortedByDescending { it.similarity }
        val best = ranked.firstOrNull() ?: return null
        if (best.similarity < .78) return null
        if (ranked.getOrNull(1)?.let { best.similarity - it.similarity < .1 } == true) return null
        return best
    }
}

/** Require two agreeing frames, including the first card; clear stale cards after three misses. */
class LiveSongStabilizer {
    private var pending: String? = null
    private var locked: String? = null
    private var misses = 0
    val shouldClear: Boolean get() = misses >= 3
    fun accept(match: ScanSongMatch?): Boolean {
        val id = match?.songId
        val accepted = id != null && (id == pending || id == locked)
        pending = id
        if (accepted) { locked = id; misses = 0 } else { misses++; if (shouldClear) locked = null }
        return accepted
    }
    fun reset() { pending = null; locked = null; misses = 0 }
}

@Serializable
private data class SongScanPresentation(val match: ScanSongMatch?, val accepted: Boolean, val shouldClear: Boolean)

class NativeLiveSongScanner {
    private val stabilizer = LiveSongStabilizer()
    private val mutex = Mutex()
    private var session = ""
    @Throws(Exception::class)
    suspend fun reviewJson(catalog: CatalogBundle, observationsJson: String, region: String, live: Boolean, session: String): String =
        withContext(Dispatchers.Default) { mutex.withLock {
            if (this@NativeLiveSongScanner.session != session) {
                stabilizer.reset(); this@NativeLiveSongScanner.session = session
            }
            val observations = CatalogJson.codec.decodeFromString<List<ScanObservation>>(observationsJson)
            val match = SongScanner.match(catalog, ScoreScanner.fields(observations).title, region)
            val accepted = if (live) stabilizer.accept(match) else match != null
            CatalogJson.codec.encodeToString(SongScanPresentation(match, accepted, live && stabilizer.shouldClear))
        } }
}
