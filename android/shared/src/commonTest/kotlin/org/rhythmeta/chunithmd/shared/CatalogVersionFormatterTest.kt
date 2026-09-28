package org.rhythmeta.chunithmd.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogVersionFormatterTest {
    @Test
    fun formatsPlusVersionsWithPlusSign() {
        assertEquals("AIR+", CatalogVersionFormatter.badge("CHUNITHM AIR PLUS"))
        assertEquals("SUN+", CatalogVersionFormatter.badge("sun plus"))
    }

    @Test
    fun preservesRenamedVersionNamesAtFullLength() {
        assertEquals("PARADISE LOST", CatalogVersionFormatter.badge("CHUNITHM PARADISE LOST"))
        assertEquals("PARADISE LOST+", CatalogVersionFormatter.badge("PARADISE LOST PLUS"))
    }

    @Test
    fun uppercasesAndDoesNotTruncateVersionNames() {
        val version = "chunithm luminous international edition"
        assertEquals("LUMINOUS INTERNATIONAL EDITION", CatalogVersionFormatter.badge(version))
    }

    @Test
    fun fallsBackToBaseGameNameForMissingVersion() {
        assertEquals("CHUNITHM", CatalogVersionFormatter.badge(null))
        assertEquals("CHUNITHM", CatalogVersionFormatter.badge("  "))
        assertEquals("CHUNITHM", CatalogVersionFormatter.badge("CHUNITHM"))
    }
}
