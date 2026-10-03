package org.rhythmeta.chunithmd.collection

import org.rhythmeta.chunithmd.shared.localization.tr

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater
import org.rhythmeta.chunithmd.sharing.SongCollectionShare
import org.rhythmeta.chunithmd.sharing.SongCollectionEntry

/** Offline protobuf snapshot shared by the app and dashboard. */
object SongCollectionCodec {
    private const val Prefix = SongCollectionLinks.Prefix
    private const val MaxTextSize = SongCollectionLinks.MaxCodeSize
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
        require(bytes.size <= MaxRawSize) { tr("收藏夹过大，无法生成分享码") }
        val result = Prefix + Base64.getUrlEncoder().withoutPadding().encodeToString(compress(bytes))
        require(result.length <= MaxTextSize) { tr("收藏夹过大，无法生成分享码") }
        return result
    }

    fun webUrl(collection: SongCollection): String = SongCollectionLinks.webUrl(encode(collection))

    fun decode(text: String): CollectionExport {
        val code = SongCollectionLinks.extractCode(text)
        val message = try {
            val compressed = Base64.getUrlDecoder().decode(code.removePrefix(Prefix))
            SongCollectionShare.parseFrom(decompress(compressed))
        } catch (error: Exception) {
            throw IllegalArgumentException(tr("分享码不完整或已损坏"), error)
        }
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
                require(count > 0 || inflater.finished()) { tr("分享码不完整或已损坏") }
                require(output.size() + count <= MaxRawSize) { tr("收藏夹数据过大") }
                output.write(buffer, 0, count)
            }
            require(inflater.remaining == 0) { tr("分享码含有多余数据") }
            output.toByteArray()
        } finally {
            inflater.end()
        }
    }
}
