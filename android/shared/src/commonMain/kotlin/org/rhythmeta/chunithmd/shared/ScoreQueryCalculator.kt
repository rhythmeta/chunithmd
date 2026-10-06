package org.rhythmeta.chunithmd.shared

enum class ScoreQueryDisplayMode { Grid, List }

enum class ScoreQuerySortMode { Rating, Score, Level }

data class ScoreQueryFilterSettings(
    val difficulties: Set<String> = emptySet(),
    val ranks: Set<String> = emptySet(),
    val fullCombos: Set<String> = emptySet(),
    val fullChains: Set<String> = emptySet(),
) {
    val isEmpty: Boolean
        get() = difficulties.isEmpty() && ranks.isEmpty() && fullCombos.isEmpty() && fullChains.isEmpty()
}

data class ScoreQueryEntry(
    val sheetKey: String,
    val songId: String,
    val title: String,
    val artist: String,
    val aliases: List<String>,
    val imageName: String,
    val difficulty: String,
    val type: String,
    val level: Double,
    val score: Int,
    val rank: String,
    val rating: Double,
    val clear: String?,
    val fullCombo: String?,
    val fullChain: String?,
)

data class ScoreQueryStats(
    val chartCount: Int = 0,
    val songCount: Int = 0,
    val sssPlusCount: Int = 0,
    val sssCount: Int = 0,
    val fcCount: Int = 0,
    val ajCount: Int = 0,
    val ajcCount: Int = 0,
    val platinumFullChainCount: Int = 0,
    val goldFullChainCount: Int = 0,
) {
    val fullChainCount: Int get() = platinumFullChainCount + goldFullChainCount
}

data class ScoreQueryResponse(
    val entries: List<ScoreQueryEntry> = emptyList(),
    val stats: ScoreQueryStats = ScoreQueryStats(),
)

/** Builds one query row per played chart using the best score recorded for it. */
fun buildScoreQueryResponse(
    bundle: CatalogBundle,
    records: Iterable<ScoreRecord>,
    activeServer: ProfileServer,
): ScoreQueryResponse {
    val bestRecords = records.groupBy(ScoreRecord::sheetKey).mapValues { (_, values) ->
        values.maxWithOrNull(compareBy<ScoreRecord> { it.score }.thenBy { it.playedAt })!!
    }
    val region = activeServer.wireValue
    val entries = bundle.catalog.songs.flatMap { song ->
        song.sheets.mapNotNull { sheet ->
            if (sheet.type.contains("utage", true) || !song.isSheetPlayableIn(sheet, region)) return@mapNotNull null
            val record = bestRecords[song.sheetKey(sheet)] ?: return@mapNotNull null
            val constant = if (activeServer == ProfileServer.Cn) {
                song.regionOverrides["cn"]?.charts?.get("${sheet.type}:${sheet.difficulty}")?.levelValue
                    ?: sheet.internalLevelValue ?: sheet.levelValue
            } else {
                sheet.internalLevelValue ?: sheet.levelValue
            } ?: return@mapNotNull null
            val rating = calculateSingleRating(constant, record.score)
            ScoreQueryEntry(
                sheetKey = song.sheetKey(sheet),
                songId = song.songId,
                title = CatalogSongFormatter.displayTitle(song),
                artist = song.artist,
                aliases = bundle.aliases[song.songId].orEmpty(),
                imageName = song.imageName,
                difficulty = sheet.difficulty,
                type = sheet.type,
                level = constant,
                score = record.score,
                rank = ChunithmScoreRules.rank(record.score),
                rating = rating,
                clear = record.clear,
                fullCombo = canonicalFullCombo(record.fullCombo),
                fullChain = canonicalFullChain(record.fullChain),
            )
        }
    }.sortedWith(compareByDescending<ScoreQueryEntry> { it.rating }.thenBy { it.title }.thenBy { it.sheetKey })
    return ScoreQueryResponse(entries, calculateScoreQueryStats(entries))
}

fun filterAndSortScoreQueryEntries(
    entries: List<ScoreQueryEntry>,
    searchText: String,
    settings: ScoreQueryFilterSettings,
    sortMode: ScoreQuerySortMode,
    ascending: Boolean,
): List<ScoreQueryEntry> {
    val query = normalizeScoreQuerySearch(searchText)
    val filtered = entries.filter { entry ->
        (query.isEmpty() || sequenceOf(entry.title, entry.artist, entry.songId)
            .plus(entry.aliases.asSequence())
            .any { normalizeScoreQuerySearch(it).contains(query) }) &&
            (settings.difficulties.isEmpty() || entry.difficulty.lowercase() in settings.difficulties) &&
            (settings.ranks.isEmpty() || entry.rank in settings.ranks) &&
            (settings.fullCombos.isEmpty() || displayFullCombo(entry.fullCombo) in settings.fullCombos) &&
            (settings.fullChains.isEmpty() || displayFullChain(entry.fullChain) in settings.fullChains)
    }
    val primary = when (sortMode) {
        ScoreQuerySortMode.Rating -> compareBy<ScoreQueryEntry> { it.rating }
        ScoreQuerySortMode.Score -> compareBy { it.score }
        ScoreQuerySortMode.Level -> compareBy { it.level }
    }
    val directed = if (ascending) primary else primary.reversed()
    return filtered.sortedWith(directed.thenBy { it.title }.thenBy { it.sheetKey })
}

fun displayFullCombo(value: String?): String? = when (normalizeStatus(value)) {
    "fc", "fc+", "fullcombo", "fullcombo+" -> FullComboType.FullCombo.displayName
    "aj", "ap", "alljustice" -> FullComboType.AllJustice.displayName
    "ajc", "ap+", "alljusticecritical" -> FullComboType.AllJusticeCritical.displayName
    else -> null
}

fun displayFullChain(value: String?): String? = when (normalizeStatus(value)) {
    "fs", "fs+", "fullchain", "铂fullchain", "铂fc" -> FullChainType.FullChain.displayName
    "fdx", "fdx+", "fullchain2", "fullchainplus2", "金fullchain", "金fc" -> FullChainType.FullChain2.displayName
    else -> null
}

private fun canonicalFullCombo(value: String?): String? = when (displayFullCombo(value)) {
    "FC" -> "fc"
    "AJ" -> "aj"
    "AJC" -> "ajc"
    else -> null
}

private fun canonicalFullChain(value: String?): String? = when (displayFullChain(value)) {
    "铂 FC" -> "fs"
    "金 FC" -> "fdx"
    else -> null
}

private fun calculateScoreQueryStats(entries: List<ScoreQueryEntry>): ScoreQueryStats = ScoreQueryStats(
    chartCount = entries.size,
    songCount = entries.map(ScoreQueryEntry::songId).distinct().size,
    sssPlusCount = entries.count { it.rank == "SSS+" },
    sssCount = entries.count { it.rank == "SSS" },
    fcCount = entries.count { displayFullCombo(it.fullCombo) == "FC" },
    ajCount = entries.count { displayFullCombo(it.fullCombo) == "AJ" },
    ajcCount = entries.count { displayFullCombo(it.fullCombo) == "AJC" },
    platinumFullChainCount = entries.count { displayFullChain(it.fullChain) == "铂 FC" },
    goldFullChainCount = entries.count { displayFullChain(it.fullChain) == "金 FC" },
)

private fun normalizeStatus(value: String?): String = value.orEmpty()
    .trim()
    .lowercase()
    .replace(" ", "")
    .replace("_", "")
    .replace("-", "")

private fun normalizeScoreQuerySearch(value: String): String = value.trim().lowercase().replace(Regex("\\s+"), " ")
