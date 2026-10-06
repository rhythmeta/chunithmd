package org.rhythmeta.chunithmd.shared

import kotlin.test.*
import org.rhythmeta.chunithmd.collection.*

class CollectionCardsTest {
    @Test fun difficultySortUsesTheCollectedChartAndRetainsMissingSongs() {
        val bundle = CatalogBundle(1, Catalog(songs = listOf(
            CatalogSong("a", "A", sheets = listOf(CatalogSheet("std", "expert", internalLevelValue = 10.0), CatalogSheet("std", "master", internalLevelValue = 15.0))),
            CatalogSong("b", "B", sheets = listOf(CatalogSheet("std", "master", internalLevelValue = 13.0))),
        )))
        val collection = SongCollection("c", "C", listOf(CollectionEntry("a", "std", "master"), CollectionEntry("a", "std", "expert"), CollectionEntry("b", "std", "master"), CollectionEntry("missing", "std", "master")))
        val cards = collectionCards(collection, bundle, CatalogSort.Difficulty, false, ProfileServer.Jp)
        assertEquals(listOf("a:std:master", "b:std:master", "a:std:expert", "missing:std:master"), cards.map { it.entry.key })
        assertNull(cards.last().song)
        assertEquals(4, cards.size)
    }
}
