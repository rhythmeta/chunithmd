package org.rhythmeta.chunithmd.shared

import kotlinx.coroutines.flow.Flow

enum class ClearType(
    val wireValue: String,
    val displayName: String,
) {
    Catastrophy("catastrophy", "CATASTROPHY"),
    Absolute("absolute", "ABSOLUTE"),
    Brave("brave", "BRAVE"),
    Hard("hard", "HARD"),
    Clear("clear", "CLEAR"),
    Failed("failed", "FAILED"),
    ;

    companion object {
        fun fromWire(value: String?): ClearType = entries.firstOrNull { it.wireValue.equals(value, true) } ?: Clear
        fun displayName(value: String?): String = entries.firstOrNull { it.wireValue.equals(value, true) }?.displayName
            ?: value?.trim()?.takeIf(String::isNotEmpty)?.uppercase() ?: Clear.displayName
    }
}

enum class FullComboType(
    val wireValue: String,
    val displayName: String,
) {
    AllJusticeCritical("alljusticecritical", "AJC"),
    AllJustice("alljustice", "AJ"),
    FullCombo("fullcombo", "FC"),
    ;

    companion object {
        fun fromWire(value: String?): FullComboType? = entries.firstOrNull { it.wireValue.equals(value, true) }
        fun displayName(value: String?): String? = displayFullCombo(value)
            ?: value?.trim()?.takeIf(String::isNotEmpty)?.uppercase()
    }
}

enum class FullChainType(
    val wireValue: String,
    val displayName: String,
) {
    FullChain("fullchain", "铂 FC"),
    FullChain2("fullchain2", "金 FC"),
    ;

    companion object {
        fun fromWire(value: String?): FullChainType? = entries.firstOrNull { it.wireValue.equals(value, true) }
        fun displayName(value: String?): String? = displayFullChain(value)
            ?: value?.trim()?.takeIf(String::isNotEmpty)?.uppercase()
    }
}

data class ScoreRecord(
    val id: String,
    val profileId: String,
    val songId: String,
    val sheetKey: String,
    val score: Int,
    val rank: String,
    val playedAt: Long,
    val clear: String = ClearType.Clear.wireValue,
    val fullCombo: String? = null,
    val fullChain: String? = null,
)

interface ScoreStore {
    fun observeSongRecords(songId: String): Flow<List<ScoreRecord>>
    fun observeCurrentProfileRecords(): Flow<List<ScoreRecord>>
    suspend fun save(
        songId: String,
        sheetKey: String,
        score: Int,
        clear: ClearType = ClearType.Clear,
        fullCombo: FullComboType? = null,
        fullChain: FullChainType? = null,
    ): ScoreRecord
    suspend fun delete(id: String)
}

enum class ScoreHistorySort { Time, Score }

fun Iterable<ScoreRecord>.bestScore(): ScoreRecord? = maxWithOrNull(compareBy<ScoreRecord> { it.score }.thenBy { it.playedAt })

fun Iterable<ScoreRecord>.sortForHistory(sort: ScoreHistorySort): List<ScoreRecord> = when (sort) {
    ScoreHistorySort.Time -> sortedByDescending(ScoreRecord::playedAt)
    ScoreHistorySort.Score -> sortedWith(compareByDescending<ScoreRecord> { it.score }.thenByDescending { it.playedAt })
}

fun List<ScoreRecord>.page(page: Int, pageSize: Int): List<ScoreRecord> {
    if (pageSize <= 0) return emptyList()
    val safePage = page.coerceAtLeast(1)
    return drop((safePage - 1) * pageSize).take(pageSize)
}

fun CatalogSong.sheetKey(sheet: CatalogSheet): String = "$songId:${sheet.type}:${sheet.difficulty}"

fun CatalogSong.progressSheets(region: String = "jp"): List<CatalogSheet> {
    val available = sheets.filter { isSheetPlayableIn(it, region) }
    val preferred = available.filter { it.type.equals("std", true) || it.type.equals("standard", true) }
    val worldsEnd = available.filter { it.type.equals("we", true) }
    return (preferred + worldsEnd)
        .distinctBy { if (it.type.equals("we", true)) "we" else it.difficulty.lowercase() }
        .sortedByDescending { progressDifficultyOrder(if (it.type.equals("we", true)) "world's end" else it.difficulty) }
}

private fun progressDifficultyOrder(value: String): Int = listOf(
    "basic", "advanced", "expert", "master", "ultima", "world's end",
).indexOfFirst { it.equals(value, true) }

fun scoreProgress(score: Int?): Float = score
    ?.minus(1_000_000)
    ?.coerceIn(0, 10_000)
    ?.div(10_000f)
    ?: 0f
