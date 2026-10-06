@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
package org.rhythmeta.chunithmd.collection

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.io.encoding.Base64

@Serializable private data class SharedCollectionMessage(
    @ProtoNumber(1) val name: String = "",
    @ProtoNumber(2) val entries: List<SharedCollectionEntry> = emptyList(),
)
@Serializable private data class SharedCollectionEntry(
    @ProtoNumber(1) val songId: String = "",
    @ProtoNumber(2) val chartType: String = "",
    @ProtoNumber(3) val difficulty: String = "",
)

/** Same protobuf and raw DEFLATE wire format as Android and the dashboard. */
object PortableCollectionCodec {
    @Throws(Exception::class)
    fun encode(collection: CollectionExport): String {
        val value = collection.validated()
        val bytes = ProtoBuf.encodeToByteArray(SharedCollectionMessage.serializer(), SharedCollectionMessage(value.name,
            value.entries.map { SharedCollectionEntry(it.songId, it.chartType, it.difficulty) }))
        require(bytes.size <= 1_000_000) { "收藏夹过大" }
        val code = SongCollectionLinks.Prefix + Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(deflateCollection(bytes, true))
        require(code.length <= SongCollectionLinks.MaxCodeSize) { "收藏夹过大" }
        return SongCollectionLinks.webUrl(code)
    }
    @Throws(Exception::class)
    fun decode(text: String): CollectionExport {
        val code = SongCollectionLinks.extractCode(text).removePrefix(SongCollectionLinks.Prefix)
        val bytes = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).decode(code)
        val value = ProtoBuf.decodeFromByteArray(SharedCollectionMessage.serializer(), deflateCollection(bytes, false))
        return CollectionExport(value.name, value.entries.map { CollectionEntry(it.songId, it.chartType, it.difficulty) }).validated()
    }
}
internal expect fun deflateCollection(bytes: ByteArray, compress: Boolean): ByteArray
