package org.rhythmeta.chunithmd.shared

import kotlin.test.*
import org.rhythmeta.chunithmd.shared.backup.*

class PersonalDataBridgeTest {
    @Test fun scannedScoreSavesSelectedWeChartAndRejectsStaleProfileWithoutWriting() {
        val files = Files(); val bridge = PersonalDataBridge(files)
        bridge.snapshotJson()
        val profile = files.snapshot().profiles.single().id
        val bundle = CatalogBundle(1, Catalog(songs = listOf(CatalogSong("we-song", "WE song", sheets = listOf(
            CatalogSheet("we", "狂", "☆☆☆", regions = mapOf("jp" to true)),
            CatalogSheet("we", "止", "☆☆", regions = mapOf("jp" to true)),
        )))))
        bridge.saveScannedScore(bundle, profile, "jp", "we-song:we:止", "１，００７，５００", "clear", "alljustice")
        val result = files.snapshot().playRecords.single().result
        assertEquals(profile, result.profileId)
        assertEquals("we-song:we:止", result.chartKey)
        assertEquals(1_007_500, result.score)
        assertEquals("alljustice", result.fc)
        val other = bridge.saveProfile(null, "Other", "jp", "")
        bridge.activateProfile(other)
        val before = files.data.getValue("personal.pb.gz").copyOf()
        assertFails { bridge.saveScannedScore(bundle, profile, "jp", "we-song:we:止", "1,007,500", "clear", "") }
        assertContentEquals(before, files.data.getValue("personal.pb.gz"))
        files.write("restore-pending.pb.gz", before)
        assertFails { bridge.saveScannedScore(bundle, other, "jp", "we-song:we:止", "1,007,500", "clear", "") }
        assertContentEquals(before, files.data.getValue("personal.pb.gz"))
    }

    @Test fun collectionPickerSavesMembershipAtomicallyAndPreservesOtherCharts() {
        val files = Files(); val bridge = PersonalDataBridge(files)
        bridge.saveCollection(null, "One"); bridge.saveCollection(null, "Two")
        val first = files.snapshot().collections[0].id
        val second = files.snapshot().collections[1].id
        bridge.toggleCollectionSong(first, "song", "std", "expert")
        bridge.setChartCollections("song", "STD", "MASTER", listOf(first, second))
        assertEquals(3, files.snapshot().collectionItems.size)
        bridge.setChartCollections("song", "std", "master", listOf(second))
        val items = files.snapshot().collectionItems
        assertEquals(2, items.size)
        assertTrue(items.any { it.collectionId == first && it.difficulty == "expert" })
        assertTrue(items.any { it.collectionId == second && it.difficulty == "master" })
        val before = files.data.getValue("personal.pb.gz").copyOf()
        files.write("restore-pending.pb.gz", before)
        assertFails { bridge.setChartCollections("song", "std", "master", emptyList()) }
        assertContentEquals(before, files.data.getValue("personal.pb.gz"))
    }

    @Test fun bestTableCapacityRoundTripsThroughPortableSettingsAndPreservesOtherSettings() {
        val files = Files(); val bridge = PersonalDataBridge(files)
        bridge.snapshotJson()
        val state = files.snapshot().copy(settings = listOf(
            BackupSetting(key = "android.chunithmd.theme.color_mode", kind = "int", integerValue = 2),
            BackupSetting(key = "android.chunithmd.best.best_count", kind = "int", integerValue = 7),
            BackupSetting(key = "android.chunithmd.best.new_count", kind = "int", integerValue = 8),
        ))
        files.write("personal.pb.gz", BackupCodec.encode(state))
        val bundle = CatalogBundle(1, Catalog(songs = emptyList()))
        assertEquals(7, bridge.bestTable(bundle, null).preferences.bestCount)
        bridge.setBestTableCapacity(0, 120)
        val reloaded = PersonalDataBridge(files).bestTable(bundle, null)
        assertEquals(1, reloaded.preferences.bestCount)
        assertEquals(99, reloaded.preferences.newCount)
        assertEquals(2, files.snapshot().settings.first { it.key.endsWith("color_mode") }.integerValue)
        assertEquals(3, files.snapshot().settings.size)
        val before = files.data.getValue("personal.pb.gz").copyOf()
        files.write("restore-pending.pb.gz", before)
        assertFails { bridge.setBestTableCapacity(30, 20) }
        assertContentEquals(before, files.data.getValue("personal.pb.gz"))
    }
    @Test fun scoreQueryUsesActiveProfileImportedBestsRegionAndCommunityAliases() {
        val files = Files(); val bridge = PersonalDataBridge(files)
        bridge.snapshotJson()
        val state = files.snapshot()
        val profile = state.profiles.single().id
        val score = BackupScore(profileId = profile, songId = "song", chartKey = "song:std:master",
            score = 1_009_000, rank = "SSS+", achievedAt = 2, fc = "alljustice", fs = "fullchain2")
        files.write("personal.pb.gz", BackupCodec.encode(state.copy(
            profiles = state.profiles.map { it.copy(server = "cn") } + BackupProfile(id = "00000000-0000-4000-8000-000000000002", name = "Other", server = "jp"),
            scores = listOf(score, score.copy(profileId = "00000000-0000-4000-8000-000000000002", score = 1_010_000)),
            playRecords = listOf(BackupPlayRecord("00000000-0000-4000-8000-000000000001", score.copy(score = 950_000, achievedAt = 3))),
        )))
        val bundle = CatalogJson.decodeBundle("""
            {"schemaVersion":1,"aliases":{"song":["Local alias"]},"catalog":{"songs":[
              {"songId":"song","title":"Song","artist":"Artist","sheets":[
                {"type":"std","difficulty":"master","internalLevelValue":13.0,"regions":{"jp":true}}],
                "regionOverrides":{"cn":{"available":true,"charts":{"std:master":{"available":true,"levelValue":13.8}}}}}
            ]}}
        """.trimIndent())
        val result = bridge.scoreQuery(bundle, mapOf("song" to listOf("Community alias")))
        val entry = result.entries.single()
        assertEquals(1_009_000, entry.score)
        assertEquals(13.8, entry.level)
        assertEquals(calculateSingleRating(13.8, entry.score), entry.rating)
        assertEquals(1, result.stats.chartCount)
        assertEquals(1, result.stats.ajCount)
        assertEquals(1, result.stats.fullChainCount)
        for (query in listOf("Local alias", "Community alias", "ARTIST", "song")) {
            assertEquals(listOf(entry), filterAndSortScoreQueryEntries(result.entries, query,
                ScoreQueryFilterSettings(), ScoreQuerySortMode.Rating, false))
        }
        val other = bridge.saveProfile(null, "Other", "jp", "")
        bridge.activateProfile(other)
        assertTrue(bridge.scoreQuery(bundle, emptyMap()).entries.isEmpty())
        assertEquals(0, bridge.scoreQuery(bundle, emptyMap()).stats.chartCount)
    }

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
        assertEquals(0.9f, bridge.chartProgress()["song:std:master"])
        assertEquals(1f, bridge.chartProgress()["song:std:expert"])
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
