package org.rhythmeta.chunithmd.shared

data class VersionColor(
    val lightBackground: Long,
    val lightForeground: Long,
    val darkBackground: Long,
    val darkForeground: Long,
)

object VersionPalette {
    private val colors = mapOf(
        "CHUNITHM" to color(0xFFF2D32F, 0xFF332A00, 0xFFB39B13, 0xFFFFFFFF),
        "AIR" to color(0xFF4DC9D0, 0xFF00363A, 0xFF147B83, 0xFFFFFFFF),
        "STAR" to color(0xFFE5B74B, 0xFF342400, 0xFF9C6B16, 0xFFFFFFFF),
        "AMAZON" to color(0xFF72C987, 0xFF073516, 0xFF26733D, 0xFFFFFFFF),
        "CRYSTAL" to color(0xFFB8A8E6, 0xFF281C48, 0xFF705BAA, 0xFFFFFFFF),
        "SUPERSTAR" to color(0xFFF0D878, 0xFF382600, 0xFFA98522, 0xFFFFFFFF),
        "PARADISE" to color(0xFFE887AD, 0xFF451129, 0xFFA6406D, 0xFFFFFFFF),
        "NEW" to color(0xFF55CBB1, 0xFF00372F, 0xFF19846F, 0xFFFFFFFF),
        "SUN" to color(0xFFE9A044, 0xFF3D2100, 0xFF9C5B16, 0xFFFFFFFF),
        "LUMINOUS" to color(0xFFE990C6, 0xFF471033, 0xFFA94D89, 0xFFFFFFFF),
        "VERSE" to color(0xFFB587E8, 0xFF2E1450, 0xFF7842A9, 0xFFFFFFFF),
        "X-VERSE" to color(0xFF77CBE7, 0xFF063246, 0xFF287996, 0xFFFFFFFF),
        "X-VERSE-X" to color(0xFF90D8BF, 0xFF103B31, 0xFF378B75, 0xFFFFFFFF),
        "MATE" to color(0xFFF1B565, 0xFF482600, 0xFF9B5C19, 0xFFFFFFFF),
    )

    fun forVersion(version: String?, dark: Boolean): VersionColor {
        val key = version.orEmpty().trim().uppercase().removePrefix("CHUNITHM ")
        val base = when {
            key == "PARADISE LOST" || key.startsWith("PARADISE") -> "PARADISE"
            key.startsWith("NEW") -> "NEW"
            key == "SUN PLUS" || key == "SUN" -> "SUN"
            key.startsWith("LUMINOUS") -> "LUMINOUS"
            key == "X-VERSE" -> "X-VERSE"
            key == "X-VERSE-X" -> "X-VERSE-X"
            key == "VERSE" -> "VERSE"
            key == "MATE" -> "MATE"
            else -> key.substringBefore(" PLUS").substringBefore('+')
        }
        val resolved = colors[base] ?: color(0xFF718096, 0xFFFFFFFF, 0xFF4A5568, 0xFFFFFFFF)
        return resolved.copy(
            lightBackground = resolved.lightBackground,
            lightForeground = resolved.lightForeground,
            darkBackground = resolved.darkBackground,
            darkForeground = resolved.darkForeground,
        ).let { if (dark) it else it }
    }

    private fun color(lightBg: Long, lightFg: Long, darkBg: Long, darkFg: Long) =
        VersionColor(lightBg, lightFg, darkBg, darkFg)
}
