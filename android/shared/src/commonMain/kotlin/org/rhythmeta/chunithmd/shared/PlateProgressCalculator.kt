package org.rhythmeta.chunithmd.shared

enum class PlateType(val title: String, val requirement: String) {
    Spirit("Spirit", "RANK S"),
    Tribute("Tribute", "RANK SSS"),
    Legend("Legend", "ALL JUSTICE"),
    ;

    fun isAchieved(record: ScoreRecord): Boolean = when (this) {
        Spirit -> record.score >= 975_000
        Tribute -> record.score >= 1_007_500
        Legend -> displayFullCombo(record.fullCombo) in setOf("AJ", "AJC")
    }
}

data class VersionPlateGroup(val version: String, val name: String)

data class PlateChartEntry(
    val song: CatalogSong,
    val sheet: CatalogSheet,
    val level: String,
    val achieved: Boolean,
    val achievementLabel: String?,
) {
    val sheetKey: String get() = song.sheetKey(sheet)
}

data class PlateLevelSection(val level: String, val charts: List<PlateChartEntry>) {
    val completedCount: Int get() = charts.count { it.achieved }
}

data class PlateProgressResponse(
    val groups: List<VersionPlateGroup> = emptyList(),
    val selectedGroup: VersionPlateGroup? = null,
    val plateType: PlateType = PlateType.Spirit,
    val charts: List<PlateChartEntry> = emptyList(),
) {
    val totalCount: Int get() = charts.size
    val completedCount: Int get() = charts.count { it.achieved }
    val remainingCount: Int get() = totalCount - completedCount
    val progress: Float get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount
    val achieved: Boolean get() = totalCount > 0 && remainingCount == 0
    val title: String get() = "${plateType.title} of ${selectedGroup?.name.orEmpty()}"

    // Filtering the grid must not change the progress of the whole version's title.
    fun sections(difficulty: String? = null, remainingOnly: Boolean = false): List<PlateLevelSection> = charts
        .filter { (difficulty == null || it.sheet.difficulty.equals(difficulty, true)) && (!remainingOnly || !it.achieved) }
        .groupBy { it.level }
        .map { (level, entries) -> PlateLevelSection(level, entries) }
        .sortedByDescending { it.level.replace("+", ".5").toDoubleOrNull() ?: 0.0 }
}

object PlateProgressCalculator {
    val difficulties: List<String> = listOf("basic", "advanced", "expert", "master")

    fun versionName(version: String): String {
        val name = version.trim().uppercase()
            .removePrefix("CHUNITHM")
            .replace("（初代）", "").replace("(初代)", "")
            .replace("+", " PLUS")
            .trim().replace(Regex("\\s+"), " ")
        return when (name) {
            "", "ORIGIN" -> "ORIGIN"
            "PLUS", "ORIGIN PLUS" -> "ORIGIN PLUS"
            else -> name
        }
    }

    /** Records must belong to the active profile; each historical achievement counts independently. */
    fun calculate(
        bundle: CatalogBundle,
        records: List<ScoreRecord>,
        activeServer: ProfileServer,
        version: String? = null,
        plateType: PlateType = PlateType.Spirit,
    ): PlateProgressResponse {
        val songs = bundle.catalog.songs.filter { song ->
            !song.version.isNullOrBlank() && song.sheets.any { eligible(song, it, activeServer) }
        }
        val names = songs.mapNotNull { it.version }.distinct()
        val orderedVersions = (bundle.catalog.versions.map { it.version } + names).distinct()
        val groups = orderedVersions.filter { candidate -> names.any { it.equals(candidate, true) } }
            .distinctBy(::versionName)
            .map { VersionPlateGroup(it, versionName(it)) }
        val group = groups.firstOrNull { version != null && versionName(it.version) == versionName(version) }
            ?: groups.firstOrNull()
            ?: return PlateProgressResponse(plateType = plateType)
        val scoresBySheet = records.groupBy { it.sheetKey }
        val charts = songs.asSequence()
            .filter { versionName(it.version.orEmpty()) == group.name }
            .flatMap { song ->
                song.sheets.asSequence().filter { eligible(song, it, activeServer) }.map { sheet ->
                    val history = scoresBySheet[song.sheetKey(sheet)].orEmpty()
                    val achievedRecords = history.filter(plateType::isAchieved)
                    val marker = when (plateType) {
                        PlateType.Legend -> when {
                            achievedRecords.any { displayFullCombo(it.fullCombo) == "AJC" } -> "AJC"
                            achievedRecords.isNotEmpty() -> "AJ"
                            else -> null
                        }
                        else -> achievedRecords.maxOfOrNull { it.score }?.let(ChunithmScoreRules::rank)
                    }
                    val level = if (activeServer == ProfileServer.Cn) {
                        song.regionOverrides["cn"]?.charts?.get("${sheet.type}:${sheet.difficulty}")?.level ?: sheet.level
                    } else sheet.level
                    PlateChartEntry(song, sheet, level.ifBlank { "?" }, achievedRecords.isNotEmpty(), marker)
                }
            }
            .distinctBy { it.sheetKey }
            .sortedWith(compareBy({ CatalogSongFormatter.displayTitle(it.song).lowercase() },
                { difficulties.indexOf(it.sheet.difficulty.lowercase()) }, { it.sheetKey }))
            .toList()
        return PlateProgressResponse(groups, group, plateType, charts)
    }

    private fun eligible(song: CatalogSong, sheet: CatalogSheet, server: ProfileServer): Boolean =
        (sheet.type.equals("std", true) || sheet.type.equals("standard", true)) &&
            sheet.difficulty.lowercase() in difficulties &&
            !song.category.contains("utage", true) && !song.category.contains("宴") &&
            song.isSheetPlayableIn(sheet, server.wireValue)
}
