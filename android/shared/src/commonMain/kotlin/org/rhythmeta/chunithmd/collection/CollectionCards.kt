package org.rhythmeta.chunithmd.collection

import org.rhythmeta.chunithmd.shared.*

data class CollectionCard(val entry: CollectionEntry, val song: CatalogSong?, val sheet: CatalogSheet?)

fun collectionCards(collection: SongCollection, bundle: CatalogBundle?, sort: CatalogSort, ascending: Boolean, server: ProfileServer): List<CollectionCard> {
    val songs = bundle?.catalog?.songs.orEmpty()
    val byId = songs.associateBy { it.songId }
    val order = songs.mapIndexed { index, song -> song.songId to index }.toMap()
    val versions = bundle?.catalog?.versions.orEmpty().mapIndexed { index, version -> version.version to index }.toMap()
    val cards = collection.entries.map { entry ->
        val song = byId[entry.songId]
        val sheet = song?.sheets?.firstOrNull { it.type.equals(entry.chartType, true) && it.difficulty.equals(entry.difficulty, true) }
        val regionLevel = if (server == ProfileServer.Cn) song?.regionOverrides?.get("cn")?.charts?.get("${entry.chartType}:${entry.difficulty}") else null
        CollectionCard(entry, song, sheet?.let { if (regionLevel == null) it else it.copy(level = regionLevel.level ?: it.level, internalLevelValue = regionLevel.levelValue ?: it.internalLevelValue) })
    }
    val comparator = when (sort) {
        CatalogSort.Default -> compareBy<CollectionCard> { order[it.entry.songId] ?: Int.MAX_VALUE }
        CatalogSort.Title -> compareBy { it.song?.let(CatalogSongFormatter::displayTitle)?.lowercase() ?: it.entry.songId }
        CatalogSort.VersionDate -> compareBy<CollectionCard> { versions[it.song?.version] ?: Int.MAX_VALUE }.thenBy { it.song?.releaseDate.orEmpty() }
        CatalogSort.Difficulty -> compareBy { it.sheet?.internalLevelValue ?: it.sheet?.levelValue ?: 0.0 }
    }
    return cards.sortedWith(if (ascending) comparator else comparator.reversed())
}
