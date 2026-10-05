package org.rhythmeta.chunithmd.shared.importing

import kotlinx.serialization.json.*
import okio.ByteString.Companion.encodeUtf8
import org.rhythmeta.chunithmd.shared.*

class OtogameException(val code: String) : Exception(code)
data class OtogamePlay(val musicId: String, val title: String, val difficulty: Int, val track: Int,
    val score: Int, val playedAt: Long, val clear: String?, val fullCombo: String?, val fullChain: String?)
data class OtogamePayload(val fetched: Int, val plays: List<OtogamePlay>)
data class OtogamePage(val page: Int, val totalPages: Int, val payload: OtogamePayload)
data class OtogameImportPlan(val records: List<ScoreRecord>, val result: ScoreImportResult)

object OtogameImportPolicy {
    const val PAGE_LIMIT = 4
    fun isEligible(server: ProfileServer?) = server == ProfileServer.Jp

    fun decode(response: JsonObject): OtogamePage {
        if (response.string("code") != "ok") throw OtogameException("invalid_response")
        val data = response["data"] as? JsonObject ?: throw OtogameException("invalid_response")
        val rows = data["data"] as? JsonArray ?: throw OtogameException("invalid_response")
        val pagination = data["pagination"] as? JsonObject ?: throw OtogameException("invalid_response")
        val page = pagination.int("page")?.takeIf { it > 0 } ?: throw OtogameException("invalid_response")
        val total = pagination.int("total_page")?.takeIf { it >= 0 } ?: throw OtogameException("invalid_response")
        val plays = rows.mapNotNull { value ->
            val row = value as? JsonObject ?: return@mapNotNull null
            val music = row["music"] as? JsonObject ?: return@mapNotNull null
            val id = music.string("music_id")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val title = music.string("name")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val difficulty = row.int("difficulty")?.takeIf { it in 0..5 } ?: return@mapNotNull null
            val track = row.int("track")?.takeIf { it > 0 } ?: return@mapNotNull null
            val score = row.int("score")?.takeIf(ChunithmScoreRules::isValid) ?: return@mapNotNull null
            val time = (row["play_date"] as? JsonPrimitive)?.longOrNull
                ?.takeIf { it in 1..Long.MAX_VALUE / 1000 } ?: return@mapNotNull null
            val clear = when (row.boolean("is_clear")) {
                false -> ClearType.Failed.wireValue
                true -> clearForSkill(row.int("skill_id")).wireValue
                null -> null
            }
            val combo = when {
                score == 1_010_000 -> FullComboType.AllJusticeCritical.wireValue
                row.boolean("is_all_justice") == true -> FullComboType.AllJustice.wireValue
                row.boolean("is_full_combo") == true -> FullComboType.FullCombo.wireValue
                else -> null
            }
            val chain = when (row.int("full_chain_kind")) {
                1 -> FullChainType.FullChain2.wireValue
                2 -> FullChainType.FullChain.wireValue
                else -> null
            }
            OtogamePlay(id, title, difficulty, track, score, time * 1000, clear, combo, chain)
        }
        return OtogamePage(page, total.coerceIn(1, PAGE_LIMIT), OtogamePayload(rows.size, plays))
    }

    fun plan(payload: OtogamePayload, catalog: CatalogBundle, profileId: String, existing: List<ScoreRecord>): OtogameImportPlan {
        if (catalog.catalog.songs.isEmpty()) throw OtogameException("catalog_empty")
        val matcher = ImportChartMatcher(catalog)
        val ids = existing.mapTo(mutableSetOf()) { it.id }
        val fingerprints = existing.filter { it.profileId == profileId }.mapTo(mutableSetOf(), ::fingerprint)
        val additions = payload.plays.mapNotNull { play ->
            val (song, sheet) = matcher.match(play.title, play.difficulty) ?: return@mapNotNull null
            // Use provider play identity, independent of score corrections or catalog naming.
            val hash = JsonArray(listOf("otogame", profileId, play.musicId, play.difficulty.toString(),
                play.playedAt.toString(), play.track.toString()).map(::JsonPrimitive)).toString().encodeUtf8().sha256().hex()
            val id = "${hash.take(8)}-${hash.substring(8, 12)}-8${hash.substring(13, 16)}-8${hash.substring(17, 20)}-${hash.substring(20, 32)}"
            val record = ScoreRecord(id, profileId, song.songId, song.sheetKey(sheet), play.score,
                ChunithmScoreRules.rank(play.score), play.playedAt, play.clear, play.fullCombo, play.fullChain)
            if (!ids.add(id) || !fingerprints.add(fingerprint(record))) return@mapNotNull null
            record
        }
        return OtogameImportPlan(additions, ScoreImportResult(payload.fetched, additions.size, payload.fetched - additions.size))
    }

    // Skill groups published by Otogame's CHUNITHM frontend. Unknown skills only establish CLEAR.
    private fun clearForSkill(id: Int?): ClearType = when (id) {
        100005, 100006, 101004, 101005, 101006, 102004, 102005, 102006,
        103002, 103003, 103004, 104002, 104003, 104004 -> ClearType.Hard
        101007, 102007, 103005, 104005 -> ClearType.Brave
        100008, 101008, 102008, 103006, 104006 -> ClearType.Absolute
        100009, 102009, 103007, 104007 -> ClearType.Catastrophy
        else -> ClearType.Clear
    }
    private fun fingerprint(record: ScoreRecord) = listOf(record.sheetKey, record.playedAt, record.score,
        ClearType.fromWire(record.clear)?.wireValue, FullComboType.fromWire(record.fullCombo)?.wireValue,
        FullChainType.fromWire(record.fullChain)?.wireValue)
    private fun JsonObject.int(key: String) = (this[key] as? JsonPrimitive)?.intOrNull
    private fun JsonObject.boolean(key: String) = (this[key] as? JsonPrimitive)?.booleanOrNull
}
