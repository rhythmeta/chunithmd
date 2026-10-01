package org.rhythmeta.chunithmd.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class ConstantTableCalculatorTest {
    @Test
    fun baseLevelsAndSectionsMatchMaimaidBuckets() {
        val entries = listOf(
            entry("a", "A", 16.0),
            entry("b", "B", 15.9),
            entry("c", "C", 13.9),
        )

        assertEquals(listOf(15, 13), ConstantTableResponse(entries).availableBaseLevels)
        assertEquals(listOf("16.0", "15.9"), constantTableSections(entries, 15).map { it.constantLabel })
        assertEquals("15~16", constantTableBaseLevelLabel(15))
    }

    @Test
    fun filtersFavoritesCategoriesAndVersions() {
        val entries = listOf(
            entry("favorite", "Favorite", 13.0, category = "maimai", version = "current", favorite = true),
            entry("other", "Other", 13.0, category = "maimai", version = "old"),
        )

        val result = filterConstantTableEntries(
            entries,
            CatalogFilters(
                categories = setOf("maimai"),
                versions = setOf("current"),
                favoritesOnly = true,
            ),
        )
        assertEquals(listOf("favorite"), result.map { it.sheetKey })
    }

    @Test
    fun entriesSortByTitleThenDifficulty() {
        val entries = listOf(
            entry("master", "Same", 14.5, difficulty = "master"),
            entry("expert", "Same", 14.5, difficulty = "expert"),
            entry("alpha", "Alpha", 14.5, difficulty = "expert"),
        )
        assertEquals(listOf("alpha", "master", "expert"), constantTableSections(entries, 14).single().entries.map { it.sheetKey })
    }

    private fun entry(
        key: String,
        title: String,
        constant: Double,
        difficulty: String = "master",
        category: String? = null,
        version: String? = null,
        favorite: Boolean = false,
    ) = ConstantTableEntry(
        sheetKey = key,
        songId = key,
        title = title,
        imageName = "$key.png",
        difficulty = difficulty,
        type = "std",
        constant = constant,
        category = category,
        version = version,
        isFavorite = favorite,
    )
}
