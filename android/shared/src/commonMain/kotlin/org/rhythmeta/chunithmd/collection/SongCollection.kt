package org.rhythmeta.chunithmd.collection

import kotlinx.serialization.Serializable

@Serializable
data class CollectionEntry(val songId: String, val chartType: String, val difficulty: String) {
    val key: String get() = "$songId:$chartType:$difficulty"
    fun normalized() = copy(chartType = chartType.lowercase(), difficulty = difficulty.lowercase())
}

@Serializable
data class SongCollection(
    val id: String,
    val name: String,
    val entries: List<CollectionEntry> = emptyList(),
)

@Serializable
data class CollectionExport(val name: String, val entries: List<CollectionEntry>, val version: Int = 1) {
    fun validated(): CollectionExport {
        require(version == 1) { "不支持的收藏夹版本" }
        require(name.trim().isNotEmpty() && name.length <= 200) { "收藏夹名称无效" }
        require(entries.size <= 10_000) { "收藏夹谱面数量过多" }
        entries.forEach {
            require(it.songId.isNotBlank() && it.songId.length <= 200) { "歌曲编号无效" }
            require(it.chartType.isNotBlank() && it.chartType.length <= 32) { "谱面类型无效" }
            require(it.difficulty.isNotBlank() && it.difficulty.length <= 64) { "谱面难度无效" }
        }
        return copy(name = name.trim().take(40), entries = entries.map { it.normalized() }.distinctBy { it.key })
    }
}

