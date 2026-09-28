package org.rhythmeta.chunithmd.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogSongFormatterTest {
    @Test
    fun prefixesWorldsEndMarkerAndKeepsSongTitle() {
        val sheets = listOf(CatalogSheet(type = "we", difficulty = "【蔵】"))
        assertEquals("[蔵] sølips", CatalogSongFormatter.displayTitle("sølips", sheets))
    }

    @Test
    fun combinesMultipleWorldsEndMarkersWithoutChangingNormalTitles() {
        val sheets = listOf(
            CatalogSheet(type = "we", difficulty = "【両】"),
            CatalogSheet(type = "we", difficulty = "【狂】"),
        )
        assertEquals("[両][狂] Imperishable Night", CatalogSongFormatter.displayTitle("Imperishable Night", sheets))
        assertEquals("Normal song", CatalogSongFormatter.displayTitle("Normal song", listOf(CatalogSheet(type = "std", difficulty = "master"))))
    }
}
