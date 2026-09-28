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
    fun availableCategories(bundle: CatalogBundle): List<String> = bundle.catalog.songs
        .map { it.category }
        .filterNot(::isWorldsEndCategory)
        .distinct()
        .sorted()
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
        val selectedCategories = filters.categories.filterNot(::isWorldsEndCategory)
        val filtered = bundle.catalog.songs.filter { song ->
            val searchable = sequenceOf(song.title, song.artist, song.songId)
                .plus(bundle.aliases[song.songId].orEmpty().asSequence())
            val matchesSearch = query.isEmpty() || searchable.any { normalize(it).contains(query) }
            matchesSearch &&
                (selectedCategories.isEmpty() || song.category in selectedCategories) &&
                (filters.versions.isEmpty() || song.version in filters.versions) &&
                (filters.difficulties.isEmpty() || matchesDifficultyFilter(song, filters.difficulties)) &&
                (filters.types.isEmpty() || song.sheets.any { it.normalizedType() in filters.types }) &&
                (!filters.playableOnly || song.isPlayableInJp())
        }
        return when (sort) {
            CatalogSort.Default -> filtered
            CatalogSort.Title -> filtered.sortedWith(
                compareBy<CatalogSong>({ normalize(it.title) }, { it.songId }),
            ).directed(ascending)
            CatalogSort.VersionDate -> sortByVersionDate(filtered, versionOrder, ascending)
            CatalogSort.Difficulty -> sortByDifficulty(filtered, ascending)
        }
    }

    private fun sortByVersionDate(
        songs: List<CatalogSong>,
        versionOrder: Map<String, Int>,
        ascending: Boolean,
    ): List<CatalogSong> {
        val known = songs.filter { !it.isDeletedInJp() && versionOrder[it.version] != null }
        val unknown = songs.filter { it.isDeletedInJp() || versionOrder[it.version] == null }
        val comparator = compareBy<CatalogSong>(
            { versionOrder[it.version] ?: 0 },
            { it.releaseDate.orEmpty() },
            { normalize(it.title) },
        )
        val orderedKnown = known.sortedWith(comparator).directed(ascending)

        // Missing-version and deleted songs have no reliable release position.
        // Keep that bucket on the old side regardless of the selected direction.
        return if (ascending) unknown + orderedKnown else orderedKnown + unknown
    }

    private fun sortByDifficulty(songs: List<CatalogSong>, ascending: Boolean): List<CatalogSong> {
        val result = songs.toMutableList()
        val positionsByType = songs.indices
            .filter { songs[it].difficultySortValue() != null }
            .groupBy { songs[it].isWorldsEnd() }

        positionsByType.forEach { (_, positions) ->
            val ordered = positions
                .map { index -> index to songs[index] }
                .sortedWith { first, second ->
                    val firstValue = first.second.difficultySortValue() ?: return@sortedWith 0
                    val secondValue = second.second.difficultySortValue() ?: return@sortedWith 0
                    val valueComparison = firstValue.compareTo(secondValue)
                    if (valueComparison != 0) {
                        if (ascending) valueComparison else -valueComparison
                    } else {
                        first.first.compareTo(second.first)
                    }
                }
            positions.forEachIndexed { index, position -> result[position] = ordered[index].second }
        }
        return result
    }

    private fun CatalogSong.difficultySortValue(): Double? {
        if (isDeletedInJp()) return null
        return if (isWorldsEnd()) {
            highestJpWorldsEndStars()?.toDouble()
        } else {
            highestJpStandardLevel()
        }
    }

    private fun matchesDifficultyFilter(song: CatalogSong, selected: Set<String>): Boolean {
        return selected.any { difficulty ->
            if (difficulty.equals("world's end", ignoreCase = true)) {
                song.isWorldsEnd()
            } else {
                song.sheets.any { it.normalizedDifficulty() == difficulty }
            }
        }
    }

    private fun <T> List<T>.directed(ascending: Boolean): List<T> = if (ascending) this else asReversed()

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

fun isWorldsEndCategory(value: String): Boolean = when (value.trim().lowercase()) {
    "we", "world's end", "worlds end" -> true
    else -> false
}

fun requireSupportedBundle(bundle: CatalogBundle) {
    require(bundle.schemaVersion == 1) { "Unsupported catalog schema version ${bundle.schemaVersion}." }
    require(bundle.catalog.songs.all { it.songId.isNotBlank() && it.title.isNotBlank() }) {
        "Catalog contains a song without a stable ID or title."
    }
}
