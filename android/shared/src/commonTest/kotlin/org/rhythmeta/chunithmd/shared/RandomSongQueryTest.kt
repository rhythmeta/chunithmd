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

    @Test
    fun nativeDrawUsesOwnFiltersAndExplicitRegionAndFavorites() {
        val bundle = bundle(song("chosen"), song("other", category = "POPS"))
        val request = """{"search":"no match","categories":["VARIETY"],"favoritesOnly":true,"playableOnly":true,"server":"cn","favorites":["other"]}"""
        val drawn = CatalogJson.codec.decodeFromString<List<CatalogSong>>(
            NativeCatalogQuery.randomSongs(bundle, request, "jp", listOf("chosen"), 4))
        assertEquals(List(4) { "chosen" }, drawn.map { it.songId })
        assertEquals("[]", NativeCatalogQuery.randomSongs(bundle, request, "jp", emptyList(), 3))
        assertEquals(listOf("std:master"), NativeCatalogQuery.progressSheetIds(bundle, "chosen", "jp"))
        assertEquals(emptyList(), NativeCatalogQuery.progressSheetIds(bundle, "missing", "jp"))
    }

    @Test
    fun nativeDrawFormatsRegionalChartLevels() {
        val regional = song("regional").copy(regionOverrides = mapOf("cn" to RegionOverride(true,
            mapOf("std:master" to RegionChartOverride(true, "14+", 14.8)))))
        val drawn = CatalogJson.codec.decodeFromString<List<CatalogSong>>(
            NativeCatalogQuery.randomSongs(bundle(regional), "{}", "cn", emptyList(), 3))
        assertTrue(drawn.all { it.sheets.single().level == "14+" && it.sheets.single().internalLevelValue == 14.8 })
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
