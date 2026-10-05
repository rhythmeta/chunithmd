package org.rhythmeta.chunithmd.shared.importing

import kotlinx.serialization.json.*
import okio.ByteString.Companion.encodeUtf8
import org.rhythmeta.chunithmd.shared.*
import kotlin.time.Instant

class LxnsException(val code: String) : Exception(code)
data class LxnsScore(val title: String, val difficulty: Int, val score: Int, val clear: String?, val fullCombo: String?, val fullChain: String?, val playedAt: Long?)
data class LxnsPayload(val fetched: Int, val scores: List<LxnsScore>)
data class LxnsImportPlan(val records: List<ScoreRecord>, val result: ScoreImportResult)

object LxnsImportPolicy {
    fun decode(response: JsonObject, songList: JsonObject): LxnsPayload {
        if ((response["success"] as? JsonPrimitive)?.booleanOrNull != true) throw LxnsException("invalid_response")
        val rows = response["data"] as? JsonArray ?: throw LxnsException("invalid_response")
        val songs = (songList["songs"] as? JsonArray ?: throw LxnsException("invalid_response"))
            .mapNotNull { it as? JsonObject }.associateBy { it.int("id") }
        val scores = rows.mapNotNull { value ->
            val row = value as? JsonObject ?: return@mapNotNull null
            val index = row.int("level_index")?.takeIf { it in 0..5 } ?: return@mapNotNull null
            val score = row.int("score")?.takeIf(ChunithmScoreRules::isValid) ?: return@mapNotNull null
            val id = row.int("id")?.takeIf { it > 0 } ?: return@mapNotNull null
            val song = songs[id]
            var title = (song?.string("title") ?: row.string("song_name"))?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            if (index == 5) {
                val chart = (song?.get("difficulties") as? JsonArray)?.mapNotNull { it as? JsonObject }
                    ?.singleOrNull { it.int("difficulty") == 5 }
                chart?.string("kanji")?.takeIf { it.isNotBlank() }?.let { title = "[${it.removeSurrounding("【", "】")}]$title" }
            }
            val clear = row.string("clear")?.takeIf { it.isNotBlank() }?.lowercase()
            val combo = row.string("full_combo")?.takeIf { it.isNotBlank() }?.lowercase()
            val chain = row.string("full_chain")?.takeIf { it.isNotBlank() }?.lowercase()
            if (clear != null && ClearType.fromWire(clear) == null ||
                combo != null && FullComboType.fromWire(combo) == null ||
                chain != null && FullChainType.fromWire(chain) == null) return@mapNotNull null
            val time = row.string("play_time")?.takeIf { it.isNotBlank() }
            val playedAt = time?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() }
            if (time != null && (playedAt == null || playedAt <= 0)) return@mapNotNull null
            LxnsScore(title, index, score, clear,
                if (score == 1_010_000) FullComboType.AllJusticeCritical.wireValue else combo, chain, playedAt)
        }
        return LxnsPayload(rows.size, scores)
    }

    fun plan(payload: LxnsPayload, catalog: CatalogBundle, profileId: String, existing: List<ScoreRecord>, importedAt: Long): LxnsImportPlan {
        if (catalog.catalog.songs.isEmpty()) throw LxnsException("catalog_empty")
        val matcher = ImportChartMatcher(catalog)
        val histories = existing.filter { it.profileId == profileId }.groupBy { it.sheetKey }
            .mapValues { it.value.toMutableList() }.toMutableMap()
        val fingerprints = existing.filter { it.profileId == profileId }.mapTo(mutableSetOf()) { fingerprint(it) }
        val ids = existing.mapTo(mutableSetOf()) { it.id }
        val additions = mutableListOf<ScoreRecord>()
        var matched = 0
        for (item in payload.scores) {
            val (song, sheet) = matcher.match(item.title, item.difficulty) ?: continue
            matched++
            val sheetKey = song.sheetKey(sheet)
            val history = histories.getOrPut(sheetKey) { mutableListOf() }
            val best = history.bestScoreSummary()
            // Undated snapshots only add improvements. Dated plays also preserve history.
            if (item.playedAt == null && best != null && item.score <= best.score &&
                !stronger(ClearType.fromWire(item.clear)?.ordinal, ClearType.fromWire(best.clear)?.ordinal) &&
                !stronger(FullComboType.fromWire(item.fullCombo)?.ordinal, FullComboType.fromWire(best.fullCombo)?.ordinal) &&
                !stronger(FullChainType.fromWire(item.fullChain)?.ordinal, FullChainType.fromWire(best.fullChain)?.ordinal)) continue
            val hash = JsonArray(listOf("lxns", profileId, sheetKey, item.playedAt?.toString().orEmpty(),
                item.score.toString(), item.clear.orEmpty(), item.fullCombo.orEmpty(), item.fullChain.orEmpty()).map(::JsonPrimitive))
                .toString().encodeUtf8().sha256().hex()
            val id = "${hash.take(8)}-${hash.substring(8, 12)}-8${hash.substring(13, 16)}-8${hash.substring(17, 20)}-${hash.substring(20, 32)}"
            val record = ScoreRecord(id, profileId, song.songId, sheetKey, item.score, ChunithmScoreRules.rank(item.score),
                item.playedAt ?: importedAt, item.clear, item.fullCombo, item.fullChain)
            if (!ids.add(id) || !fingerprints.add(fingerprint(record))) continue
            additions += record
            history += record
        }
        return LxnsImportPlan(additions, ScoreImportResult(payload.fetched, additions.size, payload.fetched - matched))
    }

    private fun stronger(new: Int?, old: Int?) = new != null && (old == null || new < old)
    private fun fingerprint(record: ScoreRecord) = listOf(record.sheetKey, record.playedAt, record.score,
        ClearType.fromWire(record.clear)?.wireValue, FullComboType.fromWire(record.fullCombo)?.wireValue,
        FullChainType.fromWire(record.fullChain)?.wireValue)
    private fun JsonObject.int(key: String) = (this[key] as? JsonPrimitive)?.intOrNull
}
