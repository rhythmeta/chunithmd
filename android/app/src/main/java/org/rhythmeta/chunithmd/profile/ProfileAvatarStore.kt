package org.rhythmeta.chunithmd.profile

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileAvatarStore(context: Context) {
    private val avatarDirectory = File(context.applicationContext.filesDir, "profile-avatars").apply { mkdirs() }
    private val stagingDirectory = File(context.applicationContext.cacheDir, "profile-avatar-staging").apply { mkdirs() }

    // TODO: support preset and remote avatar catalogs when an authenticated resource exists.

    suspend fun stage(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        val target = File(stagingDirectory, "${UUID.randomUUID()}.png")
        runCatching {
            target.outputStream().use { output -> check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) }
            target.takeIf { it.isFile && it.length() > 0L }?.absolutePath
        }.getOrElse {
            target.delete()
            null
        }
    }

    suspend fun commit(stagedPath: String, profileId: String): String? = withContext(Dispatchers.IO) {
        val staged = File(stagedPath)
        if (!staged.isFile || staged.parentFile != stagingDirectory) return@withContext null
        val target = File(avatarDirectory, "$profileId-${UUID.randomUUID()}.png")
        runCatching {
            staged.inputStream().use { input -> target.outputStream().use { output -> input.copyTo(output) } }
            staged.delete()
            target.takeIf { it.isFile && it.length() > 0L }?.absolutePath
        }.getOrElse {
            target.delete()
            null
        }
    }

    fun discard(path: String?) {
        path?.let(::File)?.takeIf { it.parentFile == stagingDirectory }?.delete()
    }

    fun deleteStored(path: String?) {
        path?.let(::File)?.takeIf { it.parentFile == avatarDirectory }?.delete()
    }
}
