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
    @Test fun chartHistoryUsesActiveProfileAndSharedSortAndBestRules() {
        val files = Files(); val bridge = PersonalDataBridge(files)
        bridge.snapshotJson()
        val state = files.snapshot()
        val profile = state.profiles.single().id
        fun record(id: String, score: Int, time: Long, owner: String = profile, chart: String = "song:std:master") =
            BackupPlayRecord(id, BackupScore(profileId = owner, songId = "song", chartKey = chart,
                score = score, rank = ChunithmScoreRules.rank(score), achievedAt = time))
        files.write("personal.pb.gz", BackupCodec.encode(state.copy(profiles = state.profiles + BackupProfile(id = "00000000-0000-4000-8000-000000000006", name = "Other", server = "jp"), playRecords = listOf(
            record("00000000-0000-4000-8000-000000000001", 1_009_000, 1), record("00000000-0000-4000-8000-000000000002", 950_000, 3), record("00000000-0000-4000-8000-000000000003", 1_009_000, 2),
            record("00000000-0000-4000-8000-000000000004", 1_010_000, 4, owner = "00000000-0000-4000-8000-000000000006"),
            record("00000000-0000-4000-8000-000000000005", 1_010_000, 5, chart = "song:std:expert"),
        ))))
        val records = bridge.playHistory()
        assertFalse(records.any { it.id == "00000000-0000-4000-8000-000000000004" })
        val byTime = bridge.chartHistory(records, "song", "std:master", ScoreHistorySort.Time)
        assertEquals(listOf("00000000-0000-4000-8000-000000000002", "00000000-0000-4000-8000-000000000003", "00000000-0000-4000-8000-000000000001"), byTime.map { it.id })
        assertEquals(listOf("00000000-0000-4000-8000-000000000003", "00000000-0000-4000-8000-000000000001", "00000000-0000-4000-8000-000000000002"),
            bridge.chartHistory(records, "song", "std:master", ScoreHistorySort.Score).map { it.id })
        assertEquals("00000000-0000-4000-8000-000000000003", bridge.bestHistoryRecordId(byTime))
        bridge.deleteRecord("00000000-0000-4000-8000-000000000003")
        assertEquals("00000000-0000-4000-8000-000000000001", bridge.bestHistoryRecordId(bridge.chartHistory(bridge.playHistory(), "song", "std:master", ScoreHistorySort.Time)))
    }

}
