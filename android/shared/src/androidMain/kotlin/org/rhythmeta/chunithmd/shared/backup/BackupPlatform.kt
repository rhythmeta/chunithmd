package org.rhythmeta.chunithmd.shared.backup

import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

actual fun secureRandomBytes(count: Int): ByteArray = ByteArray(count).also(SecureRandom()::nextBytes)
actual fun gzipBackup(bytes: ByteArray, compress: Boolean): ByteArray {
    val output = ByteArrayOutputStream()
    if (compress) GZIPOutputStream(output).use { it.write(bytes) }
    else GZIPInputStream(bytes.inputStream()).use { input ->
        val buffer = ByteArray(65536)
        while (true) {
            val count = input.read(buffer); if (count < 0) break
            require(output.size().toLong() + count <= BackupCodec.MAX_RAW) { "Expanded backup is too large." }
            output.write(buffer, 0, count)
        }
    }
    return output.toByteArray()
}
