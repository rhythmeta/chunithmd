package org.rhythmeta.chunithmd.shared

data class ConstantTableEntry(
    val sheetKey: String,
    val songId: String,
    val title: String,
    val imageName: String,
    val difficulty: String,
    val type: String,
    val constant: Double,
    val rank: String? = null,
    val fullCombo: String? = null,
    val fullChain: String? = null,
    val category: String? = null,
    val version: String? = null,
    val isFavorite: Boolean = false,
)

data class ConstantTableSection(
    val constantLabel: String,
    val entries: List<ConstantTableEntry>,
)

data class ConstantTableResponse(
    val entries: List<ConstantTableEntry> = emptyList(),
) {
    val availableBaseLevels: List<Int>
        get() = entries.map(::constantTableBaseLevel).distinct().sortedDescending()

    fun sections(baseLevel: Int): List<ConstantTableSection> = constantTableSections(entries, baseLevel)
}

/** Builds the playable regular-chart table and attaches each chart's best imported score. */
fun buildConstantTableResponse(
    bundle: CatalogBundle,
    records: Iterable<ScoreRecord>,
    activeServer: ProfileServer,
    favoriteSongIds: Set<String> = emptySet(),
): ConstantTableResponse {
    val bestRecords = records.groupBy(ScoreRecord::sheetKey).mapValues { (_, values) ->
        values.maxWithOrNull(compareBy<ScoreRecord> { it.score }.thenBy { it.playedAt })!!
    }
    val region = activeServer.wireValue
    val entries = bundle.catalog.songs.asSequence().flatMap { song ->
        if (song.category.contains("utage", ignoreCase = true) || song.category.contains("宴")) {
            return@flatMap emptySequence()
        }
        song.sheets.asSequence().mapNotNull { sheet ->
            if (sheet.type.equals("we", ignoreCase = true) || sheet.type.contains("utage", ignoreCase = true) || !song.isSheetPlayableIn(sheet, region)) {
                return@mapNotNull null
            }
            val constant = if (activeServer == ProfileServer.Cn) {
                song.regionOverrides["cn"]?.charts?.get("${sheet.type}:${sheet.difficulty}")?.levelValue
                    ?: sheet.internalLevelValue ?: sheet.levelValue
            } else {
                sheet.internalLevelValue ?: sheet.levelValue
            }
                ?.takeIf { it.isFinite() && it > 0.0 }
                ?: return@mapNotNull null
            val record = bestRecords[song.sheetKey(sheet)]
            ConstantTableEntry(
                sheetKey = song.sheetKey(sheet),
                songId = song.songId,
                title = CatalogSongFormatter.displayTitle(song),
                imageName = song.imageName,
                difficulty = sheet.difficulty,
                type = sheet.type,
                constant = constant,
                rank = record?.let { ChunithmScoreRules.rank(it.score) },
                fullCombo = record?.fullCombo?.let(::displayFullCombo),
                fullChain = record?.fullChain?.let(::displayFullChain),
                category = song.category,
                version = song.version,
                isFavorite = song.songId in favoriteSongIds,
            )
        }
    }.sortedWith(CONSTANT_TABLE_ENTRY_COMPARATOR).toList()
    return ConstantTableResponse(entries)
}

fun filterConstantTableEntries(
    entries: List<ConstantTableEntry>,
    filters: CatalogFilters,
): List<ConstantTableEntry> = entries.filter { entry ->
    (!filters.favoritesOnly || entry.isFavorite) &&
        (filters.categories.isEmpty() || entry.category in filters.categories) &&
        (filters.versions.isEmpty() || entry.version in filters.versions)
}

fun constantTableSections(
    entries: List<ConstantTableEntry>,
    baseLevel: Int,
): List<ConstantTableSection> = entries
    .filter { constantTableBaseLevel(it) == baseLevel }
    .groupBy { constantTableLabel(it.constant) }
    .map { (label, group) -> ConstantTableSection(label, group.sortedWith(CONSTANT_TABLE_ENTRY_COMPARATOR)) }
    .sortedByDescending { it.constantLabel.toDoubleOrNull() ?: 0.0 }

fun constantTableBaseLevel(entry: ConstantTableEntry): Int = constantTableBaseLevel(entry.constant)

fun constantTableBaseLevel(constant: Double): Int =
    if (constant >= 16.0) 15 else kotlin.math.floor(constant).toInt()

fun constantTableBaseLevelLabel(baseLevel: Int): String = if (baseLevel == 15) "15~16" else baseLevel.toString()

private fun constantTableLabel(constant: Double): String {
    val tenths = kotlin.math.floor(constant * 10.0).toInt()
    return "${tenths / 10}.${tenths % 10}"
}

private val CONSTANT_TABLE_ENTRY_COMPARATOR = compareBy<ConstantTableEntry>(
    { it.title.lowercase() },
    { -constantTableDifficultyOrder(it.difficulty, it.type) },
    { it.type.lowercase() },
    { it.sheetKey },
)

private fun constantTableDifficultyOrder(difficulty: String, type: String): Int = when {
    type.equals("we", ignoreCase = true) -> 6
    difficulty.equals("ultima", ignoreCase = true) -> 5
    difficulty.equals("master", ignoreCase = true) -> 4
    difficulty.equals("expert", ignoreCase = true) -> 3
    difficulty.equals("advanced", ignoreCase = true) -> 2
    difficulty.equals("basic", ignoreCase = true) -> 1
    else -> 0
}
