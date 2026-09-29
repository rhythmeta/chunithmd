package org.rhythmeta.chunithmd.score

enum class ClearType(
    val wireValue: String,
    val displayName: String,
) {
    Catastrophy("catastrophy", "CATASTROPHY"),
    Absolute("absolute", "ABSOLUTE"),
    Brave("brave", "BRAVE"),
    Hard("hard", "HARD"),
    Clear("clear", "CLEAR"),
    Failed("failed", "FAILED"),
    ;

    companion object {
        fun fromWire(value: String?): ClearType =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) } ?: Clear

        fun displayName(value: String?): String = entries
            .firstOrNull { it.wireValue.equals(value, ignoreCase = true) }
            ?.displayName
            ?: value?.trim()?.takeIf(String::isNotEmpty)?.uppercase()
            ?: Clear.displayName
    }
}

enum class FullComboType(
    val wireValue: String,
    val displayName: String,
) {
    AllJusticeCritical("alljusticecritical", "AJC"),
    AllJustice("alljustice", "ALL JUSTICE"),
    FullCombo("fullcombo", "FULL COMBO"),
    ;

    companion object {
        fun fromWire(value: String?): FullComboType? =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) }

        fun displayName(value: String?): String? = entries
            .firstOrNull { it.wireValue.equals(value, ignoreCase = true) }
            ?.displayName
            ?: value?.trim()?.takeIf(String::isNotEmpty)?.uppercase()
    }
}

enum class FullChainType(
    val wireValue: String,
    val displayName: String,
) {
    FullChain("fullchain", "铂 FULL CHAIN"),
    FullChain2("fullchain2", "金 FULL CHAIN"),
    ;

    companion object {
        fun fromWire(value: String?): FullChainType? =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) }

        fun displayName(value: String?): String? = entries
            .firstOrNull { it.wireValue.equals(value, ignoreCase = true) }
            ?.displayName
            ?: value?.trim()?.takeIf(String::isNotEmpty)?.uppercase()
    }
}
