package org.rhythmeta.chunithmd.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogNoteCountsTest {
    @Test
    fun decodesAirAndFlickCountsFromStaticBundle() {
        val bundle = CatalogJson.decodeBundle(
            """
            {
              "schemaVersion": 1,
              "catalog": {
                "songs": [{
                  "songId": "song",
                  "title": "Song",
                  "sheets": [{
                    "type": "std",
                    "difficulty": "master",
                    "levelValue": 14.0,
                    "internalLevelValue": 14.7,
                    "noteCounts": {
                      "tap": 100,
                      "hold": 20,
                      "slide": 30,
                      "air": 40,
                      "flick": 5,
                      "total": 195
                    }
                  }]
                }]
              }
            }
            """.trimIndent(),
        )

        val sheet = bundle.catalog.songs.single().sheets.single()
        assertEquals(14.7, sheet.internalLevelValue)
        assertEquals(40, sheet.noteCounts?.air)
        assertEquals(5, sheet.noteCounts?.flick)
    }
}
