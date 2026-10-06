package org.rhythmeta.chunithmd.shared

/** Presentation data for native Best tables, using the shared selection and Rating rules. */
data class BestTableResponse(
    val preferences: BestTablePreferences,
    val serverVersion: String?,
    val effectiveVersion: String?,
    val versions: List<String>,
    val summary: PlayerRatingSummary,
    val bestEntries: List<BestTableEntry>,
    val newEntries: List<BestTableEntry>,
) {
    val totalCapacity: Int get() = preferences.bestCount + preferences.newCount
}

fun buildBestTableResponse(
    bundle: CatalogBundle,
    records: Iterable<ScoreRecord>,
    server: ProfileServer,
    preferences: BestTablePreferences,
): BestTableResponse {
    val versions = bundle.catalog.versions.map { it.version }.distinct()
    val normalized = preferences.normalized().let {
        it.copy(selectedVersion = it.selectedVersion?.takeIf(versions::contains))
    }
    val serverVersion = bundle.latestPlayableVersion(server)
    val entries = buildBestTableEntries(bundle, records, server, normalized.selectedVersion)
    val summary = calculatePlayerRating(
        entries.map { RatingChartEntry(it.chartId, it.songId, it.rating, it.isNew) },
        normalized.bestCount, normalized.newCount,
    )
    val byId = entries.associateBy { it.chartId }
    return BestTableResponse(
        normalized, serverVersion, normalized.selectedVersion ?: serverVersion, versions, summary,
        summary.best30.mapNotNull { byId[it.chartId] }, summary.new20.mapNotNull { byId[it.chartId] },
    )
}
