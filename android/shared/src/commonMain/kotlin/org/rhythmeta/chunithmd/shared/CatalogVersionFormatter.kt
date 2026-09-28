package org.rhythmeta.chunithmd.shared

object CatalogVersionFormatter {
    private const val GameName = "CHUNITHM"

    fun badge(version: String?): String {
        val normalized = version.orEmpty().trim()
        if (normalized.isEmpty()) return GameName

        val name = if (
            normalized.startsWith(GameName, ignoreCase = true) &&
            (normalized.length == GameName.length || normalized[GameName.length].isWhitespace())
        ) {
            normalized.substring(GameName.length).trimStart()
        } else {
            normalized
        }

        val hasPlusSuffix = name.endsWith("PLUS", ignoreCase = true) &&
            name.getOrNull(name.length - "PLUS".length - 1)?.isWhitespace() == true
        val baseName = if (hasPlusSuffix) name.dropLast("PLUS".length).trimEnd() else name
        val formattedName = baseName.ifBlank { GameName }.uppercase()
        return if (hasPlusSuffix && baseName.isNotBlank()) "$formattedName+" else formattedName
    }
}
