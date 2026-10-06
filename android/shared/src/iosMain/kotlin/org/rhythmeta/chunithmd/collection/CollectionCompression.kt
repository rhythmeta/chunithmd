@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.rhythmeta.chunithmd.collection
import kotlinx.cinterop.*
import platform.zlib.*
import okio.Buffer

internal actual fun deflateCollection(bytes: ByteArray, compress: Boolean): ByteArray = memScoped {
    require(bytes.isNotEmpty())
    val stream = alloc<z_stream>()
    platform.posix.memset(stream.ptr, 0, sizeOf<z_stream>().convert())
    val initialized = if (compress) deflateInit2_(stream.ptr, Z_BEST_COMPRESSION, Z_DEFLATED, -15, 8, Z_DEFAULT_STRATEGY, zlibVersion(), sizeOf<z_stream>().toInt())
        else inflateInit2_(stream.ptr, -15, zlibVersion(), sizeOf<z_stream>().toInt())
    check(initialized == Z_OK)
    try {
        bytes.usePinned { source ->
            stream.next_in = source.addressOf(0).reinterpret(); stream.avail_in = bytes.size.toUInt()
            val output = Buffer(); val chunk = ByteArray(65536)
            while (true) {
                val status = chunk.usePinned { target ->
                    stream.next_out = target.addressOf(0).reinterpret(); stream.avail_out = chunk.size.toUInt()
                    if (compress) deflate(stream.ptr, Z_FINISH) else inflate(stream.ptr, Z_NO_FLUSH)
                }
                output.write(chunk, 0, chunk.size - stream.avail_out.toInt())
                require(output.size <= 1_000_000)
                if (status == Z_STREAM_END) { require(stream.avail_in == 0u); break }
                require(status == Z_OK && (stream.avail_in > 0u || stream.avail_out == 0u)) { "Invalid collection data." }
            }
            output.readByteArray()
        }
    } finally { if (compress) deflateEnd(stream.ptr) else inflateEnd(stream.ptr) }
}
