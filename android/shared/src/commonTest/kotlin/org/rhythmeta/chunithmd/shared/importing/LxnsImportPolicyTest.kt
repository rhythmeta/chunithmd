package org.rhythmeta.chunithmd.shared.importing

import kotlinx.serialization.json.*
import org.rhythmeta.chunithmd.shared.*
import org.rhythmeta.chunithmd.shared.backup.*
import kotlin.test.*

class LxnsImportPolicyTest {
    private val profileId = "10000000-0000-4000-8000-000000000001"
    private val catalog = CatalogBundle(1, Catalog(songs = listOf(
        CatalogSong("local", "Song", sheets = listOf(CatalogSheet("std", "master"), CatalogSheet("std", "ultima"))),
        CatalogSong("we-stop", "Song", sheets = listOf(CatalogSheet("we", "【止】"))),
        CatalogSong("we-mad", "Song", sheets = listOf(CatalogSheet("we", "【狂】"))),
    )))
    private val songs = json("""{"songs":[{"id":3,"title":"Song","difficulties":[]},{"id":8000,"title":"Song","difficulties":[{"difficulty":5,"kanji":"止"}]}]}""")
    private fun json(value: String) = Json.parseToJsonElement(value).jsonObject
    private fun row(score: Int = 1_009_000, clear: String = "hard", combo: String = "alljustice", chain: String = "fullchain", time: String = "2024-01-09T16:00:00Z") =
        """{"id":3,"song_name":"Song","level_index":3,"score":$score,"clear":"$clear","full_combo":"$combo","full_chain":"$chain","play_time":"$time"}"""
    private fun decode(vararg rows: String) = LxnsImportPolicy.decode(json("""{"success":true,"data":[${rows.joinToString()}]}"""), songs)
    private fun plan(payload: LxnsPayload, existing: List<ScoreRecord> = emptyList(), time: Long = 10) =
        LxnsImportPolicy.plan(payload, catalog, profileId, existing, time)

    @Test fun mapsAllLampsTimestampUltimaAndWorldEndUsingProviderCatalog() {
        val payload = decode(row(), row().replace("\"level_index\":3", "\"level_index\":4"),
            row().replace("\"id\":3", "\"id\":8000").replace("\"level_index\":3", "\"level_index\":5"))
        val records = plan(payload).records
        assertEquals(listOf("local:std:master", "local:std:ultima", "we-stop:we:【止】"), records.map { it.sheetKey })
        assertTrue(records.all { it.clear == "hard" && it.fullCombo == "alljustice" && it.fullChain == "fullchain" })
        assertEquals(1_704_816_000_000, records.first().playedAt)
        val snapshot = BackupSnapshot(magic = "RHYTHMETA_BACKUP", formatVersion = 1, game = "chunithmd",
            profiles = listOf(BackupProfile(id = profileId, name = "p", active = true)), playRecords = records.map { it.toBackup() })
        assertEquals(records, BackupCodec.decode(BackupCodec.encode(snapshot)).playRecords.map { it.toRecord() })
    }

    @Test fun datedPlaysKeepHistoryAndDoNotDuplicateAcrossRepeatedImportsOrManualRecords() {
        val payload = decode(row(), row(score = 990_000, time = "2024-01-09T17:00:00Z"), row())
        val first = plan(payload)
        assertEquals(2, first.records.size)
        assertTrue(plan(payload, first.records, 99).records.isEmpty())
        val manual = first.records.first().copy(id = "manual")
        assertEquals(1, plan(payload, listOf(manual)).records.size)
    }

    @Test fun undatedSnapshotsOnlyAddImprovementsAndRespectNullableClear() {
        val unknown = ScoreRecord("old", profileId, "local", "local:std:master", 1_010_000, "SSS+", 1)
        val payload = decode(row(time = ""))
        val first = plan(payload, listOf(unknown))
        assertEquals(1, first.records.size)
        val history = listOf(unknown) + first.records
        assertEquals(1_010_000, history.bestScoreSummary()?.score)
        assertEquals("hard", history.bestScoreSummary()?.clear)
        assertEquals("fullchain", history.bestScoreSummary()?.fullChain)
        assertTrue(plan(payload, history, 100).records.isEmpty())
        val weaker = decode(row(clear = "clear", combo = "fullcombo", chain = "fullchain2", time = ""))
        assertTrue(plan(weaker, history).records.isEmpty())
        val noLamps = decode(row(clear = "", combo = "", chain = "", time = ""))
        assertNull(plan(noLamps).records.single().clear)
    }

    @Test fun invalidRowsAreSkippedAndFailedResponsesNeverProduceAnImport() {
        val payload = decode(row(score = -1), row(score = 1_010_001), row(clear = "bad"), row(time = "not-a-date"),
            row().replace("\"level_index\":3", "\"level_index\":6"), "{}", row())
        assertEquals(ScoreImportResult(7, 1, 6), plan(payload).result)
        assertFailsWith<LxnsException> { LxnsImportPolicy.decode(json("""{"success":false,"data":[]}"""), songs) }
        assertFailsWith<LxnsException> { LxnsImportPolicy.decode(json("""{"success":true,"data":{}}"""), songs) }
    }

    @Test fun anotherProfilesRecordsDoNotSuppressImportsAndMissingChartsAreSkipped() {
        val first = plan(decode(row())).records.single()
        assertEquals(1, plan(decode(row()), listOf(first.copy(id = "other-profile-record", profileId = "other"))).records.size)
        val missing = LxnsPayload(1, listOf(LxnsScore("Missing", 3, 900_000, null, null, null, null)))
        assertEquals(ScoreImportResult(1, 0, 1), plan(missing).result)
    }
}
