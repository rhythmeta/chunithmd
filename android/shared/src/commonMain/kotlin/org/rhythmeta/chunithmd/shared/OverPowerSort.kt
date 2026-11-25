package org.rhythmeta.chunithmd.shared

private val StringCaseInsensitive = Comparator<String> { a, b -> a.compareTo(b, ignoreCase = true) }

internal val overPowerDifficultyOrder: Comparator<OverPowerChart> =
    compareBy(OverPowerChart::constant).thenBy { chartDifficultyOrder(it.difficulty) }

enum class OverPowerSort { Remaining, Difficulty, Rate, Name, Level }

data class OverPowerOrder(val field: OverPowerSort, val ascending: Boolean) {
    fun songs(scope: OverPowerScope, recorded: Boolean): List<OverPowerSong> =
        scope.songs.filter { (it.contribution != null) == recorded }.sortedWith(
            comparator(OverPowerSong::points, compareBy(StringCaseInsensitive, OverPowerSong::title),
                compareBy(overPowerDifficultyOrder, OverPowerSong::displayChart))
                .thenBy { it.songId }
        )

    fun charts(level: OverPowerLevel): List<OverPowerChart> = level.charts.sortedWith(
        comparator(OverPowerChart::points, compareBy(StringCaseInsensitive, OverPowerChart::title), overPowerDifficultyOrder)
            .thenBy { it.sheetKey }
    )

    fun levels(scope: OverPowerScope): List<OverPowerLevel> = scope.levels.sortedWith(
        comparator(OverPowerLevel::points, compareBy { overPowerLevelOrder(it.level) }).thenBy { it.level }
    )

    private fun <T> comparator(
        points: (T) -> OverPowerPoints,
        natural: Comparator<T>,
        difficulty: Comparator<T> = natural,
    ): Comparator<T> {
        val primary: Comparator<T> = when (field) {
            OverPowerSort.Remaining -> compareBy { points(it).remaining }
            OverPowerSort.Difficulty -> difficulty
            OverPowerSort.Rate -> compareBy { points(it).percent }
            OverPowerSort.Name, OverPowerSort.Level -> natural
        }
        return (if (ascending) primary else primary.reversed()).then(natural)
    }
}

internal fun overPowerLevelOrder(level: String): Double = level.replace("+", ".5").toDoubleOrNull() ?: 0.0
