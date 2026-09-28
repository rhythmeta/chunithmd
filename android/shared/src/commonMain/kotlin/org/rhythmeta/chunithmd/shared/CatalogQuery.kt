package org.rhythmeta.chunithmd.shared

enum class CatalogSort(val wireValue: String) {
    Default("default"),
    Title("title"),
    VersionDate("versionDate"),
    Difficulty("difficulty"),
}

data class CatalogFilters(
    val categories: Set<String> = emptySet(),
    val versions: Set<String> = emptySet(),
    val difficulties: Set<String> = emptySet(),
    val types: Set<String> = emptySet(),
    val playableOnly: Boolean = false,
)

object CatalogQuery {
    fun availableCategories(bundle: CatalogBundle): List<String> = bundle.catalog.songs.map { it.category }.distinct().sorted()
    fun availableVersions(bundle: CatalogBundle): List<String> = bundle.catalog.versions.map { it.version }
    fun availableDifficulties(bundle: CatalogBundle): List<String> =
        bundle.catalog.songs.flatMap { song -> song.sheets.map { it.normalizedDifficulty() } }.distinct().sorted()
    fun availableTypes(bundle: CatalogBundle): List<String> =
        bundle.catalog.songs.flatMap { song -> song.sheets.map { it.normalizedType() } }.distinct().sorted()

    fun filterAndSort(
        bundle: CatalogBundle,
        search: String = "",
        sort: CatalogSort = CatalogSort.Default,
        ascending: Boolean = true,
        filters: CatalogFilters = CatalogFilters(),
    ): List<CatalogSong> {
        val query = normalize(search)
        val versionOrder = bundle.catalog.versions.mapIndexed { index, version -> version.version to index }.toMap()
        val filtered = bundle.catalog.songs.filter { song ->
            val searchable = sequenceOf(song.title, song.artist, song.songId)
                .plus(bundle.aliases[song.songId].orEmpty().asSequence())
            val matchesSearch = query.isEmpty() || searchable.any { normalize(it).contains(query) }
            matchesSearch &&
                (filters.categories.isEmpty() || song.category in filters.categories) &&
                (filters.versions.isEmpty() || song.version in filters.versions) &&
                (filters.difficulties.isEmpty() || song.sheets.any { it.normalizedDifficulty() in filters.difficulties }) &&
                (filters.types.isEmpty() || song.sheets.any { it.normalizedType() in filters.types }) &&
                (!filters.playableOnly || song.isPlayableInJp())
        }
        val sorted = when (sort) {
            CatalogSort.Default -> filtered
            CatalogSort.Title -> filtered.sortedWith(compareBy<CatalogSong>({ normalize(it.title) }, { it.songId }))
            CatalogSort.VersionDate -> filtered.sortedWith(
                compareBy<CatalogSong>(
                    { versionOrder[it.version] ?: Int.MAX_VALUE },
                    { it.releaseDate.orEmpty() },
                    { normalize(it.title) },
                ),
            )
            CatalogSort.Difficulty -> filtered.sortedWith(
                compareBy<CatalogSong>({ it.highestJpLevel() }, { normalize(it.title) }),
            )
        }
        return if (ascending || sort == CatalogSort.Default) sorted else sorted.asReversed()
    }

    fun searchAndFilterJson(
        bundleJson: String,
        search: String,
        sort: String,
        ascending: Boolean,
        categories: List<String>,
        versions: List<String>,
        difficulties: List<String>,
        types: List<String>,
        playableOnly: Boolean,
    ): String {
        val bundle = CatalogJson.decodeBundle(bundleJson)
        requireSupportedBundle(bundle)
        val selectedSort = CatalogSort.entries.firstOrNull { it.wireValue == sort } ?: CatalogSort.Default
        val results = filterAndSort(
            bundle = bundle,
            search = search,
            sort = selectedSort,
            ascending = ascending,
            filters = CatalogFilters(
                categories = categories.toSet(),
                versions = versions.toSet(),
                difficulties = difficulties.map(::normalize).toSet(),
                types = types.map(::normalize).toSet(),
                playableOnly = playableOnly,
            ),
        )
        return CatalogJson.codec.encodeToString(results)
    }

    private fun normalize(value: String): String = value.trim().lowercase().replace(Regex("\\s+"), " ")
}

fun requireSupportedBundle(bundle: CatalogBundle) {
    require(bundle.schemaVersion == 1) { "Unsupported catalog schema version ${bundle.schemaVersion}." }
    require(bundle.catalog.songs.all { it.songId.isNotBlank() && it.title.isNotBlank() }) {
        "Catalog contains a song without a stable ID or title."
    }
}
