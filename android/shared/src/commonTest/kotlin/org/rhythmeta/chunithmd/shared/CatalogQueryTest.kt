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
    fun latestPlayableVersionUsesServerAvailability() {
        val catalog = bundle.copy(
            catalog = bundle.catalog.copy(
                versions = listOf(CatalogVersion("CHUNITHM"), CatalogVersion("AIR"), CatalogVersion("X-VERSE"), CatalogVersion("MATE")),
                songs = listOf(
                    song("jp", "JP", "MATE", 10.0, "jp"),
                    song("cn", "CN", "X-VERSE", 10.0, "cn").copy(
                        regionOverrides = mapOf("cn" to RegionOverride(available = true)),
                    ),
                ),
            ),
        )

        assertEquals("MATE", catalog.latestPlayableVersion(ProfileServer.Jp))
        assertEquals("X-VERSE", catalog.latestPlayableVersion(ProfileServer.Cn))
    }

    @Test
    fun chartAvailabilityUsesChineseChartOverridesWhenPresent() {
        val sheet = CatalogSheet("std", "master", "13", 13.0, mapOf("jp" to true))
        val song = CatalogSong(
            songId = "cn-chart",
            title = "CN chart",
            version = "X-VERSE",
            sheets = listOf(sheet),
            regionOverrides = mapOf(
                "cn" to RegionOverride(
                    available = true,
                    charts = mapOf("std:master" to RegionChartOverride(available = true, levelValue = 13.4)),
                ),
            ),
        )

        assertTrue(song.isPlayableIn("cn"))
        assertTrue(song.isSheetPlayableIn(sheet, "cn"))
        assertEquals(13.4, song.regionOverrides.getValue("cn").charts.getValue("std:master").levelValue)
    }

    @Test
    fun searchesWorldsEndTitleMarkers() {
        val catalog = bundle.copy(
            catalog = bundle.catalog.copy(
                songs = listOf(
                    song("i-wanna", "I Wanna", "AIR", 12.0, "jp", type = "we", level = "☆")
                        .copy(sheets = listOf(CatalogSheet("we", "【招】", "☆", 12.0, mapOf("jp" to true)))),
                ),
            ),
        )

        assertEquals("i-wanna", CatalogQuery.filterAndSort(catalog, search = "i wanna").single().songId)
        assertEquals("i-wanna", CatalogQuery.filterAndSort(catalog, search = "招").single().songId)
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
    fun defaultDescendingReversesCatalogOrderAfterFiltering() {
        assertEquals(
            listOf("offline", "a", "z"),
            CatalogQuery.filterAndSort(bundle, ascending = false).map { it.songId },
        )
        assertEquals(
            listOf("a", "z"),
            CatalogQuery.filterAndSort(bundle, ascending = false, filters = CatalogFilters(playableOnly = true)).map { it.songId },
        )
    }

    @Test
    fun difficultyFilterUsesTheSelectedConstantRange() {
        assertEquals(
            listOf("a"),
            CatalogQuery.filterAndSort(
                bundle,
                filters = CatalogFilters(
                    difficulties = setOf("master"),
                    minLevel = 13.0,
                    maxLevel = 13.5,
                ),
            ).map { it.songId },
        )
        assertEquals(
            listOf("offline"),
            CatalogQuery.filterAndSort(
                bundle,
                filters = CatalogFilters(
                    difficulties = setOf("master"),
                    minLevel = 13.1,
                    maxLevel = 14.0,
                ),
            ).map { it.songId },
        )
    }

    @Test
    fun worldsEndDifficultyIgnoresTheStandardConstantRange() {
        val catalog = bundle.copy(
            catalog = bundle.catalog.copy(
                songs = listOf(
                    song("we", "World's End", "AIR", 12.0, "jp", type = "we", level = "☆", levelValue = 101.0),
                ),
            ),
        )

        assertEquals(
            listOf("we"),
            CatalogQuery.filterAndSort(
                catalog,
                filters = CatalogFilters(
                    difficulties = setOf("world's end"),
                    minLevel = 14.0,
                    maxLevel = 16.0,
                ),
            ).map { it.songId },
        )
    }

    @Test
    fun quickFiltersUseTheSelectedRegionAndHideSongsUnavailableEverywhere() {
        val catalog = bundle.copy(
            catalog = bundle.catalog.copy(
                songs = listOf(
                    song("jp-only", "JP only", "AIR", 12.0, "jp"),
                    song("intl-only", "Intl only", "AIR", 12.0, "intl"),
                    song("cn-only", "CN only", "AIR", 12.0, "jp").copy(
                        sheets = emptyList(),
                        regionOverrides = mapOf("cn" to RegionOverride(available = true)),
                    ),
                    song("deleted", "Deleted", "AIR", 12.0, "jp").copy(sheets = emptyList()),
                ),
            ),
        )

        assertEquals(
            listOf("intl-only"),
            CatalogQuery.filterAndSort(
                catalog,
                filters = CatalogFilters(playableOnly = true),
                playableRegion = "intl",
            ).map { it.songId },
        )
        assertEquals(
            listOf("jp-only", "intl-only", "cn-only"),
            CatalogQuery.filterAndSort(
                catalog,
                filters = CatalogFilters(hideDeleted = true),
            ).map { it.songId },
        )
        assertEquals(
            listOf("intl-only"),
            CatalogQuery.filterAndSort(
                catalog,
                filters = CatalogFilters(playableOnly = true, hideDeleted = true),
                playableRegion = "intl",
            ).map { it.songId },
        )
    }

    @Test
    fun favoritesFilterKeepsOnlyExplicitlyFavoriteSongs() {
        assertEquals(
            listOf("a"),
            CatalogQuery.filterAndSort(
                bundle,
                filters = CatalogFilters(favoritesOnly = true),
                favoriteSongIds = setOf("a"),
            ).map { it.songId },
        )
        assertTrue(
            CatalogQuery.filterAndSort(
                bundle,
                filters = CatalogFilters(favoritesOnly = true),
                favoriteSongIds = emptySet(),
            ).isEmpty(),
        )
    }

    @Test
    fun worldsEndDifficultyMatchesChartTypeInsteadOfInternalDifficultyName() {
        val catalog = bundle.copy(
            catalog = bundle.catalog.copy(
                songs = listOf(song("we", "World's End", "AIR", 12.0, "jp", type = "we", level = "☆")),
            ),
        )

        assertEquals(
            listOf("we"),
            CatalogQuery.filterAndSort(catalog, filters = CatalogFilters(difficulties = setOf("world's end"))).map { it.songId },
        )
    }

    @Test
    fun worldsEndIsNotExposedOrAppliedAsACategory() {
        val catalog = bundle.copy(
            catalog = bundle.catalog.copy(
                songs = listOf(
                    song("we", "World's End", "AIR", 12.0, "jp", type = "we", level = "☆")
                        .copy(category = "WE"),
                    song("std", "Standard", "AIR", 12.0, "jp"),
                ),
            ),
        )

        assertEquals(listOf("POPS & ANIME"), CatalogQuery.availableCategories(catalog))
        assertEquals(
            listOf("we", "std"),
            CatalogQuery.filterAndSort(catalog, filters = CatalogFilters(categories = setOf("WE"))).map { it.songId },
        )
    }

    @Test
    fun unknownVersionsStayOnTheOldSideInBothDirections() {
        val songs = listOf(
            song("new", "New", "AIR", 12.0, "jp"),
            song("unknown", "Unknown", "", 12.0, "jp"),
            song("old", "Old", "CHUNITHM", 12.0, "jp"),
            song("deleted", "Deleted", "AIR", 12.0, "intl"),
        )
        val catalog = bundle.copy(catalog = bundle.catalog.copy(songs = songs))

        assertEquals(
            listOf("unknown", "deleted", "old", "new"),
            CatalogQuery.filterAndSort(catalog, sort = CatalogSort.VersionDate).map { it.songId },
        )
        assertEquals(
            listOf("new", "old", "unknown", "deleted"),
            CatalogQuery.filterAndSort(catalog, sort = CatalogSort.VersionDate, ascending = false).map { it.songId },
        )
    }

    @Test
    fun difficultySortingKeepsDeletedSongsAndSeparatesWorldsEndStars() {
        val songs = listOf(
            song("deleted", "Deleted", "AIR", 20.0, "intl"),
            song("std-high", "Std high", "AIR", 15.0, "jp"),
            song("we-low", "WE low", "AIR", 12.0, "jp", type = "we", level = "☆", levelValue = 101.0),
            song("std-low", "Std low", "AIR", 10.0, "jp"),
            song("we-high", "WE high", "AIR", 12.0, "jp", type = "we", level = "☆☆☆☆☆", levelValue = 105.0),
        )
        val catalog = bundle.copy(catalog = bundle.catalog.copy(songs = songs))

        assertEquals(
            listOf("deleted", "std-low", "we-low", "std-high", "we-high"),
            CatalogQuery.filterAndSort(catalog, sort = CatalogSort.Difficulty).map { it.songId },
        )
        assertEquals(
            listOf("deleted", "std-high", "we-high", "std-low", "we-low"),
            CatalogQuery.filterAndSort(catalog, sort = CatalogSort.Difficulty, ascending = false).map { it.songId },
        )
    }

    @Test
    fun worldsEndWithoutStarsKeepsItsDefaultPosition() {
        val songs = listOf(
            song("we-unknown", "WE unknown", "AIR", 12.0, "jp", type = "we", level = "", levelValue = 105.0),
            song("we-high", "WE high", "AIR", 12.0, "jp", type = "we", level = "☆☆☆☆", levelValue = 104.0),
        )
        val catalog = bundle.copy(catalog = bundle.catalog.copy(songs = songs))

        assertEquals(
            listOf("we-unknown", "we-high"),
            CatalogQuery.filterAndSort(catalog, sort = CatalogSort.Difficulty).map { it.songId },
        )
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

    private fun song(
        id: String,
        title: String,
        version: String,
        constant: Double,
        region: String,
        type: String = "std",
        level: String = constant.toString(),
        levelValue: Double? = constant,
    ) = CatalogSong(
        songId = id,
        title = title,
        artist = "Singer",
        category = "POPS & ANIME",
        version = version,
        sheets = listOf(CatalogSheet(type, "master", level, levelValue, mapOf(region to true))),
    )
}
