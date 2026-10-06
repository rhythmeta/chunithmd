package org.rhythmeta.chunithmd.shared

import kotlin.test.*
import org.rhythmeta.chunithmd.shared.backup.*

class PersonalDataBridgeTest {
    private class Files : SnapshotFiles {
        val data = mutableMapOf<String, ByteArray>()
        override fun read(name: String) = SnapshotFile(data[name])
        override fun write(name: String, bytes: ByteArray) { data[name] = bytes }
        override fun delete(name: String) { data.remove(name) }
        fun snapshot() = BackupCodec.decode(data.getValue("personal.pb.gz"))
    }
    @Test fun editsPreserveRestoredFieldsAndUseLatestSnapshot() {
        val files = Files(); val bridge = PersonalDataBridge(files)
        bridge.snapshotJson()
        val before = files.snapshot()
        val restored = before.copy(profiles = before.profiles.map { it.copy(name = "Restored", title = "Title", avatar = byteArrayOf(1, 2, 3)) },
            settings = listOf(BackupSetting(key = "android.chunithmd.theme.color_mode", kind = "int", integerValue = 2)))
        files.write("personal.pb.gz", BackupCodec.encode(restored))
        bridge.toggleFavorite("song")
        val after = files.snapshot()
        assertEquals("Restored", after.profiles.single().name)
        assertContentEquals(byteArrayOf(1, 2, 3), after.profiles.single().avatar)
        assertEquals(restored.settings.map { it.key to it.integerValue }, after.settings.map { it.key to it.integerValue })
        assertEquals(listOf("song"), after.favoriteSongIds)
    }
    @Test fun invalidScoreAndPendingRestoreNeverMutateData() {
        val files = Files(); val bridge = PersonalDataBridge(files)
        bridge.snapshotJson()
        val before = files.data.getValue("personal.pb.gz").copyOf()
        assertFails { bridge.saveScore("song", "std", "master", 1_010_001, "", "", "") }
        assertContentEquals(before, files.data.getValue("personal.pb.gz"))
        files.write("restore-pending.pb.gz", before)
        assertFails { bridge.toggleFavorite("song") }
        assertContentEquals(before, files.data.getValue("personal.pb.gz"))
    }
    @Test fun profilesIsolateScoresAndCollectionDeletionCascades() {
        val files = Files(); val bridge = PersonalDataBridge(files)
        bridge.snapshotJson()
        val first = files.snapshot().profiles.single().id
        bridge.saveScore("song", "std", "master", 1_009_000, "clear", "alljustice", "")
        bridge.saveProfile(null, "Second", "cn", "")
        val second = files.snapshot().profiles.last().id
        bridge.activateProfile(second)
        bridge.saveScore("song", "std", "master", 1_000_000, "", "", "")
        assertEquals(listOf(first, second), files.snapshot().playRecords.map { it.result.profileId })
        assertFails { bridge.deleteProfile(second) }
        bridge.deleteProfile(first)
        assertEquals(listOf(second), files.snapshot().playRecords.map { it.result.profileId })
        bridge.saveCollection(null, "Practice")
        val folder = files.snapshot().collections.single().id
        bridge.toggleCollectionSong(folder, "song", "std", "master")
        assertEquals("song", files.snapshot().collectionItems.single().songId)
        bridge.deleteCollection(folder)
        assertTrue(files.snapshot().collectionItems.isEmpty())
    }
}
