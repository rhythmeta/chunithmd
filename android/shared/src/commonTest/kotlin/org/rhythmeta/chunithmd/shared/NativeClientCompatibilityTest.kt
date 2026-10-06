package org.rhythmeta.chunithmd.shared

import kotlin.test.*
import org.rhythmeta.chunithmd.collection.*

class NativeClientCompatibilityTest {
    @Test fun androidCollectionGoldenCanBeReadAndRoundTripped() {
        val golden = "CHMD1.4-J9vrv5yc4Fz2ZvUng0Z4WQKBdbcX5eum6iEHNxSYoUW25icUlqEVw4CSpcmlOSmZsoJM_Fk5tZXJwJlAFJCzGVp0pxl-cX5aSoFyuk5qUAAA"
        val decoded = PortableCollectionCodec.decode(golden)
        assertTrue(decoded.entries.isNotEmpty())
        assertEquals(decoded, PortableCollectionCodec.decode(PortableCollectionCodec.encode(decoded)))
        assertFails { PortableCollectionCodec.decode("CHMD1.invalid") }
    }
    @Test fun nativeSearchUsesCommunityAliasesAndRegionalConstants() {
        val bundle = CatalogBundle(1, Catalog(songs = listOf(CatalogSong("song", "Title",
            sheets = listOf(CatalogSheet("std", "master", "14", 14.0, regions = mapOf("jp" to true))),
            regionOverrides = mapOf("cn" to RegionOverride(true, mapOf("std:master" to RegionChartOverride(true, "14+", 14.8))))))))
        val result = NativeCatalogQuery.search(bundle, """{"search":"昵称","server":"cn","playableOnly":true}""", """{"song":["昵称"]}""")
        val songs = CatalogJson.codec.decodeFromString<List<CatalogSong>>(result)
        assertEquals(14.8, songs.single().sheets.single().levelValue)
        assertEquals("14+", songs.single().sheets.single().level)
    }
}
