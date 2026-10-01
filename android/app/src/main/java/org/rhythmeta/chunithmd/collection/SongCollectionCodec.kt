package org.rhythmeta.chunithmd.collection

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater
import kotlinx.serialization.Serializable
import org.rhythmeta.chunithmd.sharing.SongCollectionShare
import org.rhythmeta.chunithmd.sharing.SongCollectionEntry

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

/** An offline snapshot, deliberately separate from maimaid's song IDs and cloud links. */
object SongCollectionCodec {
    private const val Prefix = "CHMD1."
    private const val MaxTextSize = 200_000
    private const val MaxRawSize = 1_000_000

    fun encode(collection: SongCollection): String {
        val source = CollectionExport(collection.name, collection.entries).validated()
        val bytes = SongCollectionShare.newBuilder()
            .setName(source.name)
            .addAllEntries(source.entries.map { entry ->
                SongCollectionEntry.newBuilder()
                    .setSongId(entry.songId)
                    .setChartType(entry.chartType)
                    .setDifficulty(entry.difficulty)
                    .build()
            })
            .build().toByteArray()
        require(bytes.size <= MaxRawSize) { "收藏夹过大，无法生成分享码" }
        val result = Prefix + Base64.getUrlEncoder().withoutPadding().encodeToString(compress(bytes))
        require(result.length <= MaxTextSize) { "收藏夹过大，无法生成分享码" }
        return result
    }

    fun decode(text: String): CollectionExport {
        require(text.length <= MaxTextSize) { "分享码过长" }
        val code = text.trim()
        require(code.startsWith(Prefix)) { "请粘贴 chunithmd 离线收藏夹分享码" }
        val compressed = Base64.getUrlDecoder().decode(code.removePrefix(Prefix))
        val message = SongCollectionShare.parseFrom(decompress(compressed))
        return CollectionExport(message.name, message.entriesList.map {
            CollectionEntry(it.songId, it.chartType, it.difficulty)
        }).validated()
    }

    private fun compress(bytes: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        return try {
            deflater.setInput(bytes)
            deflater.finish()
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (!deflater.finished()) output.write(buffer, 0, deflater.deflate(buffer))
            output.toByteArray()
        } finally {
            deflater.end()
        }
    }

    private fun decompress(bytes: ByteArray): ByteArray {
        val inflater = Inflater(true)
        return try {
            inflater.setInput(bytes)
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                require(count > 0 || inflater.finished()) { "分享码不完整或已损坏" }
                require(output.size() + count <= MaxRawSize) { "收藏夹数据过大" }
                output.write(buffer, 0, count)
            }
            require(inflater.remaining == 0) { "分享码含有多余数据" }
            output.toByteArray()
        } finally {
            inflater.end()
        }
    }
}
