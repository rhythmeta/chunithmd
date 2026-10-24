package org.rhythmeta.chunithmd.shared.scanner

import kotlinx.serialization.Serializable
import org.rhythmeta.chunithmd.shared.*
import kotlin.math.max

@Serializable
data class ScanObservation(val field: String, val text: String, val confidence: Float, val box: ScanBox)

@Serializable
data class ScanFields(val title: String = "", val difficulty: String = "", val level: String = "", val score: String = "", val clear: String = "", val combo: String = "")

@Serializable
data class ScanChartCandidate(val songId: String, val title: String, val type: String, val difficulty: String, val level: String, val similarity: Double) {
    val key: String get() = "$songId:$type:$difficulty"
}

@Serializable
data class ScanReview(val fields: ScanFields, val parsedScore: Int?, val clear: String, val combo: String,
    val candidates: List<ScanChartCandidate>, val recommendedKey: String?)

data class ScanSave(val songId: String, val sheetKey: String, val type: String, val difficulty: String,
    val score: Int, val clear: String, val combo: String)

/** OCR parsing and chart identity rules are shared by both native clients. */
object ScoreScanner {
    fun fields(observations: List<ScanObservation>): ScanFields {
        val text = observations.groupBy { it.field }.mapValues { (_, v) -> v.maxBy { it.confidence }.text.trim() }
        return ScanFields(text["title"].orEmpty(), text["difficulty"].orEmpty(), text["level"].orEmpty(),
            text["score"].orEmpty(), text["clear"].orEmpty(), text["combo"].orEmpty())
    }

    fun parseScore(raw: String): Int? {
        val text = fold(raw).replace('O', '0').replace('o', '0')
        val values = Regex("(?<![0-9])[0-9](?:[0-9,，. ]*[0-9])?(?![0-9])").findAll(text)
            .map { match -> match.value.filter(Char::isDigit).toIntOrNull() }.toList()
        return values.singleOrNull()?.takeIf(ChunithmScoreRules::isValid)
    }

    fun difficulty(raw: String): String? {
        val text = fold(raw).uppercase().filter(Char::isLetterOrDigit)
        return when {
            text.contains("WORLD") || text == "WE" -> "we"
            text.contains("ULTIMA") || text.contains("究极") || text.contains("究極") -> "ultima"
            text.contains("MASTER") || text.contains("大师") || text.contains("大師") -> "master"
            text.contains("EXPERT") || text.contains("专家") || text.contains("專家") -> "expert"
            text.contains("ADVANCED") -> "advanced"
            text.contains("BASIC") -> "basic"
            else -> null
        }
    }

    fun attribute(raw: String): String = fold(raw).trim().removeSurrounding("【", "】").removeSurrounding("[", "]")
        .filterNot { it.isWhitespace() || it in "☆★" }
        // Multilingual recognition can emit the simplified form of a Japanese WE attribute.
        .map { when (it) { '时' -> '時'; '击' -> '撃'; '觉', '覺' -> '覚'; '弹', '彈' -> '弾'; '两', '兩' -> '両'; '噓' -> '嘘'; else -> it } }.joinToString("")

    fun review(catalog: CatalogBundle, fields: ScanFields, region: String): ScanReview {
        val diff = difficulty(fields.difficulty)
        val title = titleKey(fields.title)
        val level = attribute(fields.level)
        val candidates = if (title.isEmpty()) emptyList() else catalog.catalog.songs.flatMap { song ->
            val titles = listOf(song.title) + catalog.aliases[song.songId].orEmpty()
            val similarity = titles.maxOf { similarity(title, titleKey(it)) }
            if (similarity < 0.42) emptyList() else song.sheets.filter { sheet ->
                song.isSheetPlayableIn(sheet, region) && when {
                    diff == "we" -> sheet.type.equals("we", true) && (level.isEmpty() || attribute(sheet.difficulty) == level)
                    diff != null -> !sheet.type.equals("we", true) && sheet.difficulty.equals(diff, true)
                    else -> true
                }
            }.map { sheet ->
                val regional = song.sheetForServer(sheet, ProfileServer.fromWire(region))
                ScanChartCandidate(song.songId, song.title, sheet.type, sheet.difficulty, regional.level, similarity)
            }
        }.distinctBy { it.key }.sortedWith(compareByDescending<ScanChartCandidate> { it.similarity }.thenBy { it.key }).take(12)
        val exact = candidates.filter { it.similarity == 1.0 }
        return ScanReview(fields, parseScore(fields.score), clear(fields.clear), combo(fields.combo), candidates,
            exact.singleOrNull()?.key)
    }

    /** Resolve a scanner card without a separate chart picker. Ambiguous frames keep scanning. */
    fun automaticMatch(review: ScanReview): ScanChartCandidate? {
        val difficulty = difficulty(review.fields.difficulty) ?: return null
        if (difficulty == "we" && attribute(review.fields.level).isEmpty()) return null
        review.candidates.firstOrNull { it.key == review.recommendedKey }?.let { return it }
        val best = review.candidates.firstOrNull() ?: return null
        if (best.similarity < 0.65) return null
        val runnerUp = review.candidates.getOrNull(1)
        if (runnerUp != null && best.similarity - runnerUp.similarity < 0.1) return null
        return best
    }

    /** Re-resolve the matched chart and validate edits at save time. Saving still requires user input. */
    fun prepareSave(catalog: CatalogBundle, region: String, key: String, scoreText: String, clear: String, combo: String): ScanSave? {
        val score = parseScore(scoreText) ?: return null
        if (clear.isNotEmpty() && ClearType.fromWire(clear) == null) return null
        if (combo.isNotEmpty() && FullComboType.fromWire(combo) == null) return null
        val pair = catalog.catalog.songs.firstNotNullOfOrNull { song -> song.sheets.firstOrNull {
            song.sheetKey(it) == key && song.isSheetPlayableIn(it, region)
        }?.let { song to it } } ?: return null
        return ScanSave(pair.first.songId, key, pair.second.type, pair.second.difficulty, score, clear, combo)
    }

    private fun clear(raw: String): String {
        val text = fold(raw).uppercase().filter(Char::isLetterOrDigit)
        return when {
            "FAILED" in text || "失败" in text || "失敗" in text -> "failed"
            "CLEAR" in text || "通关" in text || "通關" in text -> "clear"
            else -> ""
        }
    }
    private fun combo(raw: String): String {
        val text = fold(raw).uppercase().filter(Char::isLetterOrDigit)
        return when {
            "AJC" in text || "ALLJUSTICECRITICAL" in text -> "alljusticecritical"
            "ALLJUSTICE" in text || "全正义" in text || "全正義" in text || text == "AJ" -> "alljustice"
            "FULLCOMBO" in text || "全连" in text || "全連" in text || text == "FC" -> "fullcombo"
            else -> ""
        }
    }
    private fun fold(raw: String): String = raw.map { if (it in '！'..'～') (it.code - 0xfee0).toChar() else it }.joinToString("")
    internal fun titleKey(raw: String): String = fold(raw).lowercase().filter(Char::isLetterOrDigit)
    internal fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        var row = IntArray(b.length + 1) { it }
        a.forEachIndexed { i, ca ->
            val next = IntArray(b.length + 1); next[0] = i + 1
            b.forEachIndexed { j, cb -> next[j + 1] = minOf(next[j] + 1, row[j + 1] + 1, row[j] + if (ca == cb) 0 else 1) }
            row = next
        }
        return 1.0 - row[b.length].toDouble() / max(a.length, b.length)
    }
}

/** JSON boundary avoids exposing native OCR types to KMP/Swift UI. */
object NativeScoreScanner {
    @Throws(Exception::class)
    fun reviewJson(catalog: CatalogBundle, observationsJson: String, region: String): String = CatalogJson.codec.encodeToString(
        ScoreScanner.review(catalog, ScoreScanner.fields(CatalogJson.codec.decodeFromString<List<ScanObservation>>(observationsJson)), region))

    fun matchJson(catalog: CatalogBundle, title: String, difficulty: String, level: String, region: String): String =
        CatalogJson.codec.encodeToString(ScoreScanner.review(catalog, ScanFields(title, difficulty, level), region))
}
