package org.rhythmeta.chunithmd.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CatalogQueryTest {
    private val bundle = CatalogBundle(
        schemaVersion = 1,
        catalog = Catalog(
            versions = listOf(CatalogVersion("CHUNITHM"), CatalogVersion("AIR")),
            songs = listOf(
                song("z", "Zeta", "CHUNITHM", 11.0, "jp"),
                song("a", "Alpha", "AIR", 13.0, "jp"),
                song("offline", "Offline", "AIR", 14.0, "intl"),
            ),
        ),
        aliases = mapOf("a" to listOf("first alias")),
    )

    @Test
    fun searchesTitleArtistAliasAndSongId() {
        assertEquals("a", CatalogQuery.filterAndSort(bundle, search = "alpha").single().songId)
        assertEquals(listOf("z", "a", "offline"), CatalogQuery.filterAndSort(bundle, search = "singer").map { it.songId })
        assertEquals("a", CatalogQuery.filterAndSort(bundle, search = "first alias").single().songId)
        assertEquals("offline", CatalogQuery.filterAndSort(bundle, search = "offline").single().songId)
    }

    @Test
    fun sortsAndFiltersByJpCatalogFields() {
        assertEquals(listOf("a", "offline", "z"), CatalogQuery.filterAndSort(bundle, sort = CatalogSort.Title).map { it.songId })
        assertEquals(listOf("z", "a", "offline"), CatalogQuery.filterAndSort(bundle).map { it.songId })
        assertEquals(listOf("z", "a"), CatalogQuery.filterAndSort(bundle, filters = CatalogFilters(playableOnly = true)).map { it.songId })
        assertEquals("a", CatalogQuery.filterAndSort(bundle, sort = CatalogSort.Difficulty, ascending = false).first().songId)
        assertTrue(CatalogQuery.filterAndSort(bundle, filters = CatalogFilters(difficulties = setOf("master"))).isNotEmpty())
        assertFalse(CatalogQuery.filterAndSort(bundle, filters = CatalogFilters(types = setOf("we"))).isNotEmpty())
    }

    @Test
    fun plusVersionsReuseBasePalette() {
        assertEquals(VersionPalette.forVersion("SUN", false), VersionPalette.forVersion("SUN PLUS", false))
        assertEquals(VersionPalette.forVersion("AIR", false), VersionPalette.forVersion("CHUNITHM AIR PLUS", false))
        assertEquals(VersionPalette.forVersion("VERSE", true), VersionPalette.forVersion("VERSE", true))
        assertTrue(VersionPalette.forVersion("X-VERSE-X", false).lightBackground != VersionPalette.forVersion("X-VERSE", false).lightBackground)
    }

    @Test
    fun rejectsUnsupportedOrIncompleteCatalogs() {
        assertFailsWith<IllegalArgumentException> {
            requireSupportedBundle(bundle.copy(schemaVersion = 2))
        }
        val incomplete = bundle.copy(catalog = bundle.catalog.copy(songs = listOf(song("", "Title", "AIR", 12.0, "jp"))))
        assertFailsWith<IllegalArgumentException> {
            requireSupportedBundle(incomplete)
        }
        assertEquals(bundle, CatalogJson.decodeBundle(CatalogJson.encodeBundle(bundle)))
    }

    private fun song(id: String, title: String, version: String, level: Double, region: String) = CatalogSong(
        songId = id,
        title = title,
        artist = "Singer",
        category = "POPS & ANIME",
        version = version,
        sheets = listOf(CatalogSheet("std", "master", "13", level, mapOf(region to true))),
    )
}
