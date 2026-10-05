package org.rhythmeta.chunithmd.shared.importing

import org.rhythmeta.chunithmd.shared.*

/** Local IDs are titles, so provider IDs must be resolved before matching. */
internal class ImportChartMatcher(catalog: CatalogBundle) {
    private val charts = catalog.catalog.songs.flatMap { song -> song.sheets.map { sheet -> song to sheet } }
    private val exact = charts.groupBy { (song, sheet) -> titleKey(song.title) to difficulty(sheet) }
    private val folded = charts.groupBy { (song, sheet) -> titleKey(song.title).lowercase() to difficulty(sheet) }

    fun match(title: String, difficulty: Int): Pair<CatalogSong, CatalogSheet>? {
        val worldEnd = if (difficulty == 5) worldEndTitle.matchEntire(title.trim()) else null
        val key = titleKey(worldEnd?.groupValues?.get(3) ?: title)
        val candidates = exact[key to difficulty] ?: folded[key.lowercase() to difficulty].orEmpty()
        val tag = worldEnd?.let { it.groupValues[1].ifEmpty { it.groupValues[2] } }
        return candidates.filter { (_, chart) ->
            tag == null || chart.difficulty.trim().removeSurrounding("【", "】").removeSurrounding("[", "]") == tag
        }.singleOrNull()
    }

    private fun difficulty(sheet: CatalogSheet): Int = if (sheet.type.equals("we", true)) 5
        else if (sheet.type.lowercase() in listOf("std", "standard"))
            listOf("basic", "advanced", "expert", "master", "ultima").indexOf(sheet.difficulty.lowercase()) else -1
    private fun titleKey(value: String) = value.trim().replace(Regex("\\s+"), " ")
    private val worldEndTitle = Regex("^(?:\\[([^]]+)\\]|【([^】]+)】)\\s*(.+)$")
}

data class ScoreImportResult(val fetched: Int, val updated: Int, val skipped: Int)
