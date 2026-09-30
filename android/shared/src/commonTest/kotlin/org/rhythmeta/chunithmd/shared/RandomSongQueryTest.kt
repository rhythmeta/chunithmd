package org.rhythmeta.chunithmd.shared

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RandomSongQueryTest {
    @Test
    fun drawClampsCountAndAllowsRepeatedSongs() {
        val songs = listOf(song("a"), song("b"))

        val result = RandomSongQuery.draw(songs, count = 1, random = Random(7))

        assertEquals(3, result.size)
        assertTrue(result.all { it in songs })
    }

    @Test
    fun filterUsesCatalogFiltersAndPlayableRegion() {
        val bundle = bundle(
            song("jp", category = "VARIETY", playable = true),
            song("cn", category = "POPS", playable = false),
        )

        val result = RandomSongQuery.filter(
            bundle = bundle,
            filters = CatalogFilters(categories = setOf("VARIETY"), playableOnly = true),
            playableRegion = "jp",
        )

        assertEquals(listOf("jp"), result.map(CatalogSong::songId))
    }

    private fun song(id: String, category: String = "VARIETY", playable: Boolean = true) = CatalogSong(
        songId = id,
        title = id,
        category = category,
        sheets = listOf(
            CatalogSheet(
                type = "std",
                difficulty = "master",
                regions = mapOf("jp" to playable),
            ),
        ),
    )

    private fun bundle(vararg songs: CatalogSong) = CatalogBundle(
        schemaVersion = 1,
        catalog = Catalog(songs = songs.toList()),
    )
}
