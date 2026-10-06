package org.rhythmeta.chunithmd.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.rhythmeta.chunithmd.shared.community.mergeCommunityAliases

@Serializable
private data class NativeCatalogRequest(
    val search: String = "", val sort: String = "default", val ascending: Boolean = true,
    val categories: List<String> = emptyList(), val versions: List<String> = emptyList(),
    val difficulties: List<String> = emptyList(), val types: List<String> = emptyList(),
    val playableOnly: Boolean = false, val hideDeleted: Boolean = false,
    val favoritesOnly: Boolean = false, val favorites: List<String> = emptyList(),
    val minLevel: Double = 1.0, val maxLevel: Double = 16.0, val server: String = "jp",
)

/** Keep the decoded bundle in Swift's store; typing never reparses the full catalog. */
object NativeCatalogQuery {
    @Throws(Exception::class)
    fun search(bundle: CatalogBundle, requestJson: String, aliasesJson: String): String {
        val request = CatalogJson.codec.decodeFromString<NativeCatalogRequest>(requestJson)
        val aliases = CatalogJson.codec.decodeFromString<Map<String, List<String>>>(aliasesJson)
        val results = CatalogQuery.filterAndSort(bundle.copy(aliases = mergeCommunityAliases(bundle.aliases, aliases)),
            request.search, CatalogSort.entries.firstOrNull { it.wireValue == request.sort } ?: CatalogSort.Default,
            request.ascending, CatalogFilters(categories = request.categories.toSet(), versions = request.versions.toSet(),
                difficulties = request.difficulties.toSet(), types = request.types.toSet(), playableOnly = request.playableOnly,
                hideDeleted = request.hideDeleted, favoritesOnly = request.favoritesOnly, minLevel = request.minLevel, maxLevel = request.maxLevel),
            request.server, request.favorites.toSet())
        return encodeRegional(results, request.server)
    }
    fun songs(bundle: CatalogBundle, server: String): String = encodeRegional(bundle.catalog.songs, server)
    fun availableRegions(song: CatalogSong): List<String> = listOf("jp", "intl", "cn").filter(song::isPlayableIn)
    private fun encodeRegional(songs: List<CatalogSong>, server: String): String = CatalogJson.codec.encodeToString(songs.map { song ->
        song.copy(sheets = song.sheets.map { song.sheetForServer(it, ProfileServer.fromWire(server)) })
    })
}
