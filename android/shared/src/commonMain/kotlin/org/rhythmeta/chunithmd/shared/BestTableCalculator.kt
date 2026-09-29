package org.rhythmeta.chunithmd.shared

data class BestTableEntry(
    val chartId: String,
    val songId: String,
    val title: String,
    val imageName: String,
    val type: String,
    val difficulty: String,
    val constant: Double,
    val score: Int,
    val rank: String,
    val rating: Double,
    val isNew: Boolean,
    val clear: String,
    val fullCombo: String?,
    val fullChain: String?,
)

data class BestTableShareEntry(
    val title: String,
    val difficulty: String,
    val type: String,
    val score: Int,
    val rank: String,
    val rating: Double,
    val level: String,
    val jacketPath: String?,
    val clear: String,
    val fullCombo: String?,
    val fullChain: String?,
)

fun buildBestTableEntries(
    bundle: CatalogBundle,
    records: Iterable<ScoreRecord>,
    activeServer: ProfileServer,
    selectedVersion: String? = null,
): List<BestTableEntry> {
    val bestScores = records.groupBy(ScoreRecord::sheetKey)
        .mapValues { (_, values) -> values.maxByOrNull(ScoreRecord::score)!! }
    val latestVersion = selectedVersion ?: bundle.latestPlayableVersion(activeServer)
    val selectedVersionIndex = selectedVersion?.let { selected ->
        bundle.catalog.versions.indexOfFirst { it.version.equals(selected, true) }.takeIf { it >= 0 }
    }

    return bundle.catalog.songs.flatMap { song ->
        song.sheets.filter { sheet ->
            val songVersionIndex = song.version?.let { songVersion ->
                bundle.catalog.versions.indexOfFirst { it.version.equals(songVersion, true) }
            }
            val releasedBySelectedVersion = selectedVersionIndex == null || songVersionIndex == null || songVersionIndex <= selectedVersionIndex
            releasedBySelectedVersion && (selectedVersion != null || song.isSheetPlayableIn(sheet, activeServer.wireValue))
        }.mapNotNull { sheet ->
            val key = song.sheetKey(sheet)
            val record = bestScores[key] ?: return@mapNotNull null
            val constant = if (activeServer == ProfileServer.Cn) {
                song.regionOverrides["cn"]?.charts?.get("${sheet.type}:${sheet.difficulty}")?.levelValue
                    ?: sheet.internalLevelValue
                    ?: sheet.levelValue
            } else {
                sheet.internalLevelValue ?: sheet.levelValue
            } ?: return@mapNotNull null
            val rating = calculateSingleRating(constant, record.score)
            if (rating <= 0.0) return@mapNotNull null
            BestTableEntry(
                chartId = key,
                songId = song.songId,
                title = CatalogSongFormatter.displayTitle(song),
                imageName = song.imageName,
                type = sheet.type,
                difficulty = sheet.difficulty,
                constant = constant,
                score = record.score,
                rank = record.rank,
                rating = rating,
                isNew = latestVersion != null && song.version.equals(latestVersion, true),
                clear = record.clear,
                fullCombo = record.fullCombo,
                fullChain = record.fullChain,
            )
        }
    }.sortedByDescending(BestTableEntry::rating)
}

data class RatingTableRow(
    val rank: String,
    val score: Int,
    val rating: Double,
    val delta: Double,
)

fun buildRatingTable(constant: Double): List<RatingTableRow> {
    if (!constant.isFinite() || constant <= 0.0) return emptyList()
    val values = ChunithmScoreRules.rankThresholds.asReversed().map { threshold ->
        threshold to calculateSingleRating(constant, threshold.score)
    }
    return values.mapIndexed { index, (threshold, rating) ->
        RatingTableRow(
            rank = threshold.rank,
            score = threshold.score,
            rating = rating,
            delta = (rating - (values.getOrNull(index + 1)?.second ?: 0.0)).coerceAtLeast(0.0),
        )
    }
}

data class NoteCountBreakdown(
    val label: String,
    val count: Int,
    val weight: Double,
    val fraction: Double,
)

fun CatalogNoteCounts.breakdown(): List<NoteCountBreakdown> {
    val values = listOfNotNull(
        tap?.let { "TAP" to (it to 1.0) },
        hold?.let { "HOLD" to (it to 2.0) },
        slide?.let { "SLIDE" to (it to 3.0) },
        air?.let { "AIR" to (it to 1.0) },
        flick?.let { "FLICK" to (it to 1.0) },
    ).filter { it.second.first > 0 }
    val totalWeight = values.sumOf { it.second.first * it.second.second }
    return values.map { (label, value) ->
        val (count, weight) = value
        NoteCountBreakdown(label, count, weight, if (totalWeight > 0.0) count * weight / totalWeight else 0.0)
    }
}

data class BestTablePreferences(
    val bestCount: Int = DEFAULT_BEST_COUNT,
    val newCount: Int = DEFAULT_NEW_COUNT,
    val selectedVersion: String? = null,
) {
    fun normalized(): BestTablePreferences = copy(
        bestCount = bestCount.coerceIn(MIN_COUNT, MAX_COUNT),
        newCount = newCount.coerceIn(MIN_COUNT, MAX_COUNT),
        selectedVersion = selectedVersion?.trim()?.takeIf(String::isNotEmpty),
    )

    companion object {
        const val DEFAULT_BEST_COUNT = 30
        const val DEFAULT_NEW_COUNT = 20
        const val MIN_COUNT = 1
        const val MAX_COUNT = 99
    }
}

interface BestTablePreferencesStore {
    val preferences: kotlinx.coroutines.flow.Flow<BestTablePreferences>
    suspend fun setCapacity(bestCount: Int, newCount: Int)
    suspend fun setVersion(version: String?)
}
