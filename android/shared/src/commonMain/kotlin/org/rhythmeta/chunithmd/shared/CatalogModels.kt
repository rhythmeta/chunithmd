package org.rhythmeta.chunithmd.shared

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
data class StaticManifest(
    val schemaVersion: Int,
    val product: String,
    val version: String,
    val sha256: String,
    val bundle: String,
    val createdAt: String,
    val assets: StaticAssets,
)

@Serializable
data class StaticAssets(
    val jacketBaseUrl: String,
)

@Serializable
data class CatalogBundle(
    val schemaVersion: Int,
    val catalog: Catalog,
    val aliases: Map<String, List<String>> = emptyMap(),
)

@Serializable
data class Catalog(
    val regions: List<String> = emptyList(),
    val categories: List<JsonElement> = emptyList(),
    val difficulties: List<JsonElement> = emptyList(),
    val types: List<JsonElement> = emptyList(),
    val versions: List<CatalogVersion> = emptyList(),
    val songs: List<CatalogSong>,
)

@Serializable
data class CatalogVersion(
    val version: String,
    val abbr: String = version,
    val releaseDate: String? = null,
)

@Serializable
data class CatalogSong(
    val songId: String,
    val title: String,
    val artist: String = "",
    val category: String = "",
    val bpm: Double? = null,
    val imageName: String = "",
    val version: String? = null,
    val releaseDate: String? = null,
    val sheets: List<CatalogSheet> = emptyList(),
    val regionOverrides: Map<String, RegionOverride> = emptyMap(),
)

@Serializable
data class CatalogSheet(
    val type: String,
    val difficulty: String,
    val level: String = "",
    val levelValue: Double? = null,
    val regions: Map<String, Boolean> = emptyMap(),
    val noteDesigner: String? = null,
    val noteCounts: CatalogNoteCounts? = null,
)

@Serializable
data class CatalogNoteCounts(
    val tap: Int? = null,
    val hold: Int? = null,
    val slide: Int? = null,
    val touch: Int? = null,
    @SerialName("break") val breakCount: Int? = null,
    val total: Int? = null,
)

@Serializable
data class RegionOverride(
    val available: Boolean,
    val charts: Map<String, RegionChartOverride> = emptyMap(),
)

@Serializable
data class RegionChartOverride(
    val available: Boolean,
    val level: String? = null,
    val levelValue: Double? = null,
)

@Serializable
data class CachedSnapshot(
    val manifest: StaticManifest,
    val bundleJson: String,
)

object CatalogJson {
    val codec = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun decodeManifest(source: String): StaticManifest = codec.decodeFromString(source)
    fun decodeBundle(source: String): CatalogBundle = codec.decodeFromString(source)
    fun decodeSnapshot(source: String): CachedSnapshot = codec.decodeFromString(source)
    fun encodeSnapshot(snapshot: CachedSnapshot): String = codec.encodeToString(snapshot)
    fun encodeBundle(bundle: CatalogBundle): String = codec.encodeToString(bundle)
}

fun CatalogSong.isPlayableInJp(): Boolean = sheets.any { it.regions["jp"] == true }

fun CatalogSong.isPlayableIn(region: String): Boolean = when {
    region.equals("cn", ignoreCase = true) -> regionOverrides["cn"]?.available == true
    else -> sheets.any { it.regions[region.lowercase()] == true }
}

fun CatalogBundle.latestPlayableVersion(server: ProfileServer): String? {
    val region = server.wireValue
    return catalog.versions.asReversed().firstOrNull { version ->
        catalog.songs.any { song ->
            song.version.equals(version.version, ignoreCase = true) && song.isPlayableIn(region)
        }
    }?.version
}

fun CatalogSong.isDeletedInJp(): Boolean = !isPlayableInJp()

fun CatalogSong.isWorldsEnd(): Boolean = sheets.any { it.type.equals("we", ignoreCase = true) }

fun CatalogSong.highestJpLevel(): Double = sheets
    .asSequence()
    .filter { it.regions["jp"] == true }
    .mapNotNull(CatalogSheet::levelValue)
    .maxOrNull() ?: 0.0

fun CatalogSong.highestJpStandardLevel(): Double? = sheets
    .asSequence()
    .filter { it.regions["jp"] == true && !it.type.equals("we", ignoreCase = true) }
    .mapNotNull(CatalogSheet::levelValue)
    .maxOrNull()

fun CatalogSong.highestJpWorldsEndStars(): Int? = sheets
    .asSequence()
    .filter { it.regions["jp"] == true && it.type.equals("we", ignoreCase = true) }
    .mapNotNull(CatalogSheet::worldsEndStars)
    .maxOrNull()

fun CatalogSheet.worldsEndStars(): Int? {
    val stars = level.count { it == '☆' }
    return stars.takeIf { it > 0 }
}

fun CatalogSheet.normalizedDifficulty(): String = difficulty.lowercase()

fun CatalogSheet.normalizedType(): String = type.lowercase()
