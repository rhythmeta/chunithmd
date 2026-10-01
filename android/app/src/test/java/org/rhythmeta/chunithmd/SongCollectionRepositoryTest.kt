package org.rhythmeta.chunithmd

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.rhythmeta.chunithmd.collection.*
import org.rhythmeta.chunithmd.sharing.SongCollectionShare

class SongCollectionRepositoryTest {
    @get:Rule val temporaryFolder = TemporaryFolder()
    private val file get() = File(temporaryFolder.root, "collections.preferences_pb")
    private val master = CollectionEntry("song-a", "std", "master")

    @Test
    fun collectionsAndChartMembershipSurviveReopening() = runBlocking {
        var id = ""
        withRepository { repository ->
            id = repository.create("目标谱面")
            repository.setMembership(id, master, true)
            repository.setMembership(id, master.copy(difficulty = "ultima"), true)
        }
        withRepository { repository ->
            val collection = repository.collections.first().single()
            assertEquals(id, collection.id)
            assertEquals("目标谱面", collection.name)
            assertEquals(listOf("master", "ultima"), collection.entries.map { it.difficulty })
        }
    }

    @Test
    fun membershipIsCaseInsensitiveAndSpecificToEachCollectionAndChart() = runBlocking {
        withRepository { repository ->
            val first = repository.create("一")
            val second = repository.create("二")
            repository.setMembership(first, master, true)
            repository.setMembership(first, master.copy(chartType = "STD", difficulty = "MASTER"), true)
            repository.setMembership(first, master.copy(chartType = "we"), true)
            repository.setMembership(second, master, true)
            assertEquals(2, repository.collections.first().first().entries.size)
            repository.setMembership(first, master, false)
            val collections = repository.collections.first()
            assertEquals("we", collections.first().entries.single().chartType)
            assertEquals(listOf(master), collections.last().entries)
            repository.delete(first)
            assertEquals(listOf(second), repository.collections.first().map { it.id })
            assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.setMembership(first, master, true) } }
        }
    }

    @Test
    fun createRenameAndImportUseUniqueBoundedNames() = runBlocking {
        withRepository { repository ->
            val name = "A".repeat(40)
            val first = repository.create(name)
            repository.create(name)
            repository.importCollection(CollectionExport(name, listOf(master, master)))
            val collections = repository.collections.first()
            assertEquals(3, collections.map { it.name }.distinct().size)
            assertTrue(collections.all { it.name.length <= 40 })
            assertEquals(1, collections.last().entries.size)
            repository.rename(first, " 新名字 ")
            assertEquals("新名字", repository.collections.first().first().name)
            assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.create("  ") } }
        }
    }

    @Test
    fun concurrentAddsDoNotLoseEntries() = runBlocking {
        withRepository { repository ->
            val id = repository.create("并发写入")
            coroutineScope {
                repeat(20) { index -> launch { repository.setMembership(id, master.copy(songId = "song-$index"), true) } }
            }
            assertEquals(20, repository.collections.first().single().entries.size)
        }
    }

    @Test
    fun offlineShareCodeRoundTripsUnicodeAndUnknownCatalogEntries() {
        val original = SongCollection("local-id", "练习曲 ✨", listOf(master, CollectionEntry("missing-song", "we", "world's end")))
        val decoded = SongCollectionCodec.decode(SongCollectionCodec.encode(original))
        assertEquals(original.name, decoded.name)
        assertEquals(original.entries, decoded.entries)
        val compressed = Base64.getUrlDecoder().decode(SongCollectionCodec.encode(original).removePrefix("CHMD1."))
        val inflater = Inflater(true)
        try {
            val bytes = InflaterInputStream(ByteArrayInputStream(compressed), inflater).use { it.readBytes() }
            val proto = SongCollectionShare.parseFrom(bytes)
            assertEquals(original.name, proto.name)
            assertEquals("song-a", proto.getEntries(0).songId)
            assertEquals("master", proto.getEntries(0).difficulty)
        } finally {
            inflater.end()
        }
    }

    @Test
    fun oversizedInflatedDataAndTruncatedCodesAreRejected() {
        val output = ByteArrayOutputStream()
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        try {
            DeflaterOutputStream(output, deflater).use { it.write(ByteArray(1_000_001)) }
        } finally {
            deflater.end()
        }
        val hugeCode = "CHMD1." + Base64.getUrlEncoder().withoutPadding().encodeToString(output.toByteArray())
        assertThrows(IllegalArgumentException::class.java) { SongCollectionCodec.decode(hugeCode) }
        val code = SongCollectionCodec.encode(SongCollection("id", "test", listOf(master)))
        assertThrows(Exception::class.java) { SongCollectionCodec.decode(code.dropLast(5)) }
    }

    @Test
    fun invalidImportsLeaveExistingCollectionsUntouched() = runBlocking {
        withRepository { repository ->
            repository.create("保留")
            for (code in listOf("", "MMD2.not-a-chunithm-code", "CHMD1.invalid", "CHMD1." + "A".repeat(200_000))) {
                assertThrows(Exception::class.java) { SongCollectionCodec.decode(code) }
            }
            assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.importCollection(CollectionExport("无效", listOf(master.copy(songId = "")))) } }
            assertEquals("保留", repository.collections.first().single().name)
        }
    }

    private suspend fun withRepository(block: suspend (SongCollectionRepository) -> Unit) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            block(SongCollectionRepository(PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })))
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin()
        }
    }
}
