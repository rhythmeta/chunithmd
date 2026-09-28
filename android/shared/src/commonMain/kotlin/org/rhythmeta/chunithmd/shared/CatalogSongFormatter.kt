package org.rhythmeta.chunithmd.shared

object CatalogSongFormatter {
    fun displayTitle(song: CatalogSong): String = displayTitle(song.title, song.sheets)

    fun displayTitle(title: String, sheets: List<CatalogSheet>): String {
        val markers = sheets.asSequence()
            .filter { it.type.equals("we", ignoreCase = true) }
            .map { it.difficulty.trim().removeSurrounding("【", "】").trim() }
            .filter(String::isNotEmpty)
            .distinct()
            .toList()
        if (markers.isEmpty()) return title

        val prefix = markers.joinToString(separator = "][", prefix = "[", postfix = "]")
        return if (title.startsWith(prefix)) title else "$prefix $title"
    }
}
