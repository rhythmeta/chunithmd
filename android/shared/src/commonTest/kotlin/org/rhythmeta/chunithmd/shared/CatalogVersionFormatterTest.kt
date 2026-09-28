package org.rhythmeta.chunithmd.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogVersionFormatterTest {
    @Test
    fun removesGameNameForNewVersionsAndUppercasesLabels() {
        assertEquals("NEW", CatalogVersionFormatter.badge("CHUNITHM NEW"))
        assertEquals("NEW+", CatalogVersionFormatter.badge("CHUNITHM NEW PLUS"))
        assertEquals("PARADISE LOST", CatalogVersionFormatter.badge("paradise lost"))
    }
}
