package org.rhythmeta.chunithmd.shared.community

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import okio.FileSystem
import okio.Path.Companion.toPath

interface CommunityAliasCache {
    fun read(): Map<String, List<String>>
    fun write(aliases: Map<String, List<String>>)
}

/** Public data only. Account-specific candidates stay in memory. */
class FileCommunityAliasCache(directory: String, private val files: FileSystem = FileSystem.SYSTEM) : CommunityAliasCache {
    private val path = directory.toPath() / "community-aliases.json"
    override fun read(): Map<String, List<String>> = if (files.exists(path)) {
        Json.decodeFromString(files.read(path) { readUtf8() })
    } else emptyMap()
    override fun write(aliases: Map<String, List<String>>) {
        files.createDirectories(path.parent!!)
        val temporary = "$path.tmp".toPath()
        files.write(temporary) { writeUtf8(Json.encodeToString(aliases)) }
        files.atomicMove(temporary, path)
    }
}
