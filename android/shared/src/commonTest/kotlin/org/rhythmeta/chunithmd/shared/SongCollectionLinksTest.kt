package org.rhythmeta.chunithmd.shared

import kotlin.test.*
import org.rhythmeta.chunithmd.collection.*

class SongCollectionLinksTest {
    private val code = "CHMD1.ABC_def-123"

    @Test fun acceptsShareCodesAndBothLinkForms() {
        for (value in listOf(
            code, "  $code\n", "https://dash.rhythmeta.org/collection/$code",
            "chunithmd://collection/$code", "https://dash.rhythmeta.org/collection/$code?from=share#preview",
        )) assertEquals(code, SongCollectionLinks.extractCode(value))
        assertEquals("https://dash.rhythmeta.org/collection/$code", SongCollectionLinks.webUrl(code))
    }

    @Test fun rejectsForeignGamesHostsAndMalformedPaths() {
        for (value in listOf(
            "", "CHMD1.", "CHMD1.a/b", "CHMD1.a b", "MMD2.ABC", "CHMD2.ABC",
            "http://dash.rhythmeta.org/collection/$code",
            "https://dash.rhythmeta.org.evil.example/collection/$code",
            "https://dash.rhythmeta.org@evil.example/collection/$code",
            "https://evil.example/collection/$code",
            "https://dash.rhythmeta.org/collection/$code/extra",
            "https://dash.rhythmeta.org/collection/MMD2.ABC",
            "maimaid://collection/$code", "chunithmd://auth/$code",
            "CHMD1." + "A".repeat(200_000),
            "https://dash.rhythmeta.org/collection/$code?" + "A".repeat(203_000),
        )) assertFailsWith<IllegalArgumentException>(value) { SongCollectionLinks.extractCode(value) }
    }

    @Test fun normalizesSharedModelsWithoutDroppingUnknownSongs() {
        val entry = CollectionEntry("unknown", "STD", "ULTIMA")
        val normalized = CollectionExport("  练习  ", listOf(entry, entry.normalized())).validated()
        assertEquals("练习", normalized.name)
        assertEquals(listOf(entry.normalized()), normalized.entries)
        assertFailsWith<IllegalArgumentException> { CollectionExport(" ", emptyList()).validated() }
        assertFailsWith<IllegalArgumentException> { CollectionExport("练习", listOf(entry.copy(songId = " "))).validated() }
    }
}
