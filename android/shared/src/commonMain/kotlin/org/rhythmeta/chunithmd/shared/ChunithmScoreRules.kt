package org.rhythmeta.chunithmd.shared

/** Score validation and rank thresholds shared by Android and future clients. */
object ChunithmScoreRules {
    data class RankThreshold(val rank: String, val score: Int)

    const val maximumScore: Int = 1_010_000

    val rankThresholds: List<RankThreshold> = listOf(
        RankThreshold("D", 0),
        RankThreshold("C", 500_000),
        RankThreshold("B", 600_000),
        RankThreshold("BB", 700_000),
        RankThreshold("BBB", 800_000),
        RankThreshold("A", 900_000),
        RankThreshold("AA", 925_000),
        RankThreshold("AAA", 950_000),
        RankThreshold("S", 975_000),
        RankThreshold("S+", 990_000),
        RankThreshold("SS", 1_000_000),
        RankThreshold("SS+", 1_005_000),
        RankThreshold("SSS", 1_007_500),
        RankThreshold("SSS+", 1_009_000),
    )

    /** Manual entry accepts integer scores, including pasted grouping separators, without OCR substitutions. */
    fun parseEntryScore(raw: String): Int? {
        val text = raw.trim().map { if (it in '！'..'～') (it.code - 0xfee0).toChar() else it }.joinToString("")
        if (!Regex("(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)").matches(text)) return null
        return text.replace(",", "").toIntOrNull()?.takeIf(::isValid)
    }

    fun isValid(score: Int): Boolean = score in 0..maximumScore

    fun rank(score: Int): String = when {
        score >= 1_009_000 -> "SSS+"
        score >= 1_007_500 -> "SSS"
        score >= 1_005_000 -> "SS+"
        score >= 1_000_000 -> "SS"
        score >= 990_000 -> "S+"
        score >= 975_000 -> "S"
        score >= 950_000 -> "AAA"
        score >= 925_000 -> "AA"
        score >= 900_000 -> "A"
        score >= 800_000 -> "BBB"
        score >= 700_000 -> "BB"
        score >= 600_000 -> "B"
        score >= 500_000 -> "C"
        else -> "D"
    }
}
