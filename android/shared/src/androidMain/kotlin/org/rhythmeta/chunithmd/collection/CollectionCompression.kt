package org.rhythmeta.chunithmd.collection

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

internal actual fun deflateCollection(bytes: ByteArray, compress: Boolean): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    if (compress) {
        val codec = Deflater(Deflater.BEST_COMPRESSION, true)
        try {
            codec.setInput(bytes); codec.finish()
            while (!codec.finished()) output.write(buffer, 0, codec.deflate(buffer))
        } finally { codec.end() }
    } else {
        val codec = Inflater(true)
        try {
            codec.setInput(bytes)
            while (!codec.finished()) {
                val count = codec.inflate(buffer)
                require(count > 0 || codec.finished()) { "分享码不完整或已损坏" }
                require(output.size() + count <= 1_000_000) { "收藏夹过大" }
                output.write(buffer, 0, count)
            }
            require(codec.remaining == 0) { "分享码含有多余数据" }
        } finally { codec.end() }
    }
    return output.toByteArray()
}
