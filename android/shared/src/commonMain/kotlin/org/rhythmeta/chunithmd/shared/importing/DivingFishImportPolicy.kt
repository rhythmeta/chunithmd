package org.rhythmeta.chunithmd.shared.importing

import kotlinx.serialization.json.*
import okio.ByteString.Companion.encodeUtf8
import org.rhythmeta.chunithmd.shared.*

/** A best-score snapshot, not a list of timestamped plays. */
data class DivingFishScore(val title: String, val difficulty: Int, val score: Int, val fullCombo: String?)
data class DivingFishPayload(val fetched: Int, val scores: List<DivingFishScore>)
data class DivingFishImportPlan(val records: List<ScoreRecord>, val result: ScoreImportResult)

object DivingFishImportPolicy {
    fun decode(response: JsonObject): DivingFishPayload {
        val best = (response["records"] as? JsonObject)?.get("best") as? JsonArray
            ?: throw DivingFishException("invalid_response")
        val scores = best.mapNotNull { element ->
            val row = element as? JsonObject ?: return@mapNotNull null
            val title = row.string("title")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val index = (row["level_index"] as? JsonPrimitive)?.intOrNull?.takeIf { it in 0..5 } ?: return@mapNotNull null
            val score = (row["score"] as? JsonPrimitive)?.intOrNull?.takeIf(ChunithmScoreRules::isValid) ?: return@mapNotNull null
            val combo = when (row.string("fc").orEmpty().lowercase()) {
                "" -> null
                "fullcombo" -> FullComboType.FullCombo.wireValue
                "alljustice" -> FullComboType.AllJustice.wireValue
                "alljusticecritical" -> FullComboType.AllJusticeCritical.wireValue
                else -> return@mapNotNull null
            }
            DivingFishScore(title, index, score, if (score == 1_010_000) FullComboType.AllJusticeCritical.wireValue else combo)
        }
        return DivingFishPayload(best.size, scores)
    }

    fun plan(payload: DivingFishPayload, catalog: CatalogBundle, profileId: String, existing: List<ScoreRecord>, importedAt: Long): DivingFishImportPlan {
        if (catalog.catalog.songs.isEmpty()) throw DivingFishException("catalog_empty")
        val matcher = ImportChartMatcher(catalog)
        val histories = existing.filter { it.profileId == profileId }.groupBy { it.sheetKey }
            .mapValues { it.value.toMutableList() }.toMutableMap()
        val additions = mutableListOf<ScoreRecord>()
        var matched = 0
        for (item in payload.scores) {
            val (song, sheet) = matcher.match(item.title, item.difficulty) ?: continue
            matched++
            val sheetKey = song.sheetKey(sheet)
            val history = histories.getOrPut(sheetKey) { mutableListOf() }
            val best = history.bestScoreSummary()
            val improvesCombo = comboStrength(item.fullCombo) > comboStrength(best?.fullCombo)
            if (best != null && item.score <= best.score && !improvesCombo) continue
            val fingerprint = JsonArray(listOf(profileId, sheetKey, item.score.toString(), item.fullCombo.orEmpty()).map(::JsonPrimitive))
                .toString().encodeUtf8().sha256().hex()
            val record = ScoreRecord(
                id = normalizeDivingFishRecordId("diving-fish:$fingerprint"), profileId = profileId, songId = song.songId, sheetKey = sheetKey,
                score = item.score, rank = ChunithmScoreRules.rank(item.score), playedAt = importedAt,
                // Neither clear lamps, full chain nor play timestamps are provided by this API.
                clear = null, fullCombo = item.fullCombo,
            )
            if (history.none { it.id == record.id }) { additions += record; history += record }
        }
        return DivingFishImportPlan(additions, ScoreImportResult(payload.fetched, additions.size, payload.fetched - matched))
    }

    private fun comboStrength(value: String?) = when (FullComboType.fromWire(value)) {
        FullComboType.AllJusticeCritical -> 3
        FullComboType.AllJustice -> 2
        FullComboType.FullCombo -> 1
        null -> 0
    }
}

/** Keep deterministic import IDs compatible with the UUID-based backup contract. */
internal fun normalizeDivingFishRecordId(id: String): String {
    val hash = id.removePrefix("diving-fish:")
    if (!id.startsWith("diving-fish:") || !hash.matches(Regex("[0-9a-f]{64}"))) return id
    // UUIDv8: application-defined hash, with RFC version and variant bits.
    return "${hash.take(8)}-${hash.substring(8, 12)}-8${hash.substring(13, 16)}-8${hash.substring(17, 20)}-${hash.substring(20, 32)}"
}
