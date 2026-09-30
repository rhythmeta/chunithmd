package org.rhythmeta.chunithmd.shared

import kotlin.random.Random

/** The catalog rules used by the random-song picker. */
object RandomSongQuery {
    fun filter(
        bundle: CatalogBundle,
        filters: CatalogFilters = CatalogFilters(),
        playableRegion: String = "jp",
        favoriteSongIds: Set<String> = emptySet(),
    ): List<CatalogSong> = CatalogQuery.filterAndSort(
        bundle = bundle,
        search = "",
        sort = CatalogSort.Default,
        ascending = true,
        filters = filters,
        playableRegion = playableRegion,
        favoriteSongIds = favoriteSongIds,
    )

    fun draw(
        pool: List<CatalogSong>,
        count: Int,
        random: Random = Random.Default,
    ): List<CatalogSong> {
        if (pool.isEmpty()) return emptyList()
        return List(count.coerceIn(3, 4)) { pool[random.nextInt(pool.size)] }
    }
}
