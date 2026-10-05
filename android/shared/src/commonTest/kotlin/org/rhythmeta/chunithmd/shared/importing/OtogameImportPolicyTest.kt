package org.rhythmeta.chunithmd.shared.importing

import kotlinx.serialization.json.*
import org.rhythmeta.chunithmd.shared.*
import org.rhythmeta.chunithmd.shared.backup.*
import kotlin.test.*

internal fun otogameRow(extra: String = "", title: String = "Aleph-0", difficulty: Int = 4, score: Int = 1002435,
    time: Long = 1790420884, track: Int = 3): String =
    """{"music":{"music_id":"8a3c946a414ffbd2bc53061bf1ccab9a","name":"$title"},"difficulty":$difficulty,"score":$score,"play_date":$time,"track":$track$extra}"""
internal fun otogamePage(page: Int = 1, total: Int = 12, rows: String = otogameRow()): String =
    """{"code":"ok","message":"","data":{"data":[$rows],"pagination":{"page":$page,"per_page":30,"total_page":$total}},"timestamp":1791198890}"""

class OtogameImportPolicyTest {
    private val profileId = "10000000-0000-4000-8000-000000000001"
    private val catalog = CatalogBundle(1, Catalog(songs = listOf(
        CatalogSong("aleph", "Aleph-0", sheets = listOf(CatalogSheet("std", "master"), CatalogSheet("std", "ultima"))),
        CatalogSong("we-stop", "Aleph-0", sheets = listOf(CatalogSheet("we", "【止】"))),
        CatalogSong("we-mad", "Aleph-0", sheets = listOf(CatalogSheet("we", "【狂】"))),
    )))
    private fun decode(vararg rows: String) = OtogameImportPolicy.decode(Json.parseToJsonElement(otogamePage(rows = rows.joinToString())).jsonObject)
    private fun plan(payload: OtogamePayload, existing: List<ScoreRecord> = emptyList()) =
        OtogameImportPolicy.plan(payload, catalog, profileId, existing)

    @Test fun sampleFieldsPreserveTimeUltimaAndSkillBasedHardClear() {
        val row = otogameRow(",\"is_clear\":true,\"skill_id\":104004,\"is_full_combo\":false,\"is_all_justice\":false,\"full_chain_kind\":0")
        val page = decode(row)
        assertEquals(4, page.totalPages)
        val record = plan(page.payload).records.single()
        assertEquals("aleph:std:ultima", record.sheetKey)
        assertEquals(1_790_420_884_000, record.playedAt)
        assertEquals(1002435, record.score)
        assertEquals("SS", record.rank)
        assertEquals("hard", record.clear)
        assertNull(record.fullCombo)
        assertNull(record.fullChain)
        val snapshot = BackupSnapshot(magic = "RHYTHMETA_BACKUP", formatVersion = 1, game = "chunithmd",
            profiles = listOf(BackupProfile(id = profileId, name = "p", active = true)), playRecords = listOf(record.toBackup()))
        assertEquals(record, BackupCodec.decode(BackupCodec.encode(snapshot)).playRecords.single().toRecord())
    }

    @Test fun mapsClearComboAndChainWithoutInventingMissingClear() {
        val records = plan(decode(
            otogameRow(",\"is_clear\":false,\"skill_id\":104007,\"is_full_combo\":true,\"full_chain_kind\":1"),
            otogameRow(",\"is_clear\":true,\"skill_id\":104005,\"is_all_justice\":true,\"full_chain_kind\":2", time = 1790420885),
            otogameRow(",\"is_clear\":true,\"skill_id\":104006", time = 1790420886),
            otogameRow(",\"is_clear\":true,\"skill_id\":104007", time = 1790420887),
            otogameRow(score = 1010000, time = 1790420888),
            otogameRow(",\"is_clear\":true,\"skill_id\":999999", time = 1790420889),
        ).payload).records
        assertEquals(listOf("failed", "brave", "absolute", "catastrophy", null, "clear"), records.map { it.clear })
        assertEquals(listOf("fullcombo", "alljustice", null, null, "alljusticecritical", null), records.map { it.fullCombo })
        assertEquals(listOf("fullchain2", "fullchain", null, null, null, null), records.map { it.fullChain })
    }

    @Test fun repeatedPagesAndImportsDoNotDuplicateButLowerHistoricalPlaysRemain() {
        val high = otogameRow()
        val low = otogameRow(score = 900000, time = 1790420800)
        val payload = decode(high, low, high).payload
        val first = plan(payload)
        assertEquals(ScoreImportResult(3, 2, 1), first.result)
        assertEquals(ScoreImportResult(3, 0, 3), plan(payload, first.records).result)
        assertEquals(1, plan(payload, listOf(first.records.first().copy(id = "manual"))).records.size)
        assertEquals(2, plan(payload, first.records.map { it.copy(id = "other-${it.id}", profileId = "other") }).records.size)
        assertEquals(1002435, first.records.bestScoreSummary()?.score)
    }

    @Test fun ambiguousWorldsEndIsSkippedAndExplicitTagsMatch() {
        val result = plan(decode(otogameRow(difficulty = 5), otogameRow(title = "[止]Aleph-0", difficulty = 5)).payload)
        assertEquals("we-stop:we:【止】", result.records.single().sheetKey)
        assertEquals(1, result.result.skipped)
    }

    @Test fun invalidRowsDoNotBecomeZeroScoresOrImportedTimestamps() {
        val payload = decode("{}", "null", otogameRow(time = 0), otogameRow(time = Long.MAX_VALUE),
            otogameRow(score = -1), otogameRow(score = 1010001), otogameRow(difficulty = 6), otogameRow(track = 0),
            otogameRow(title = "Missing"), otogameRow()).payload
        assertEquals(ScoreImportResult(10, 1, 9), plan(payload).result)
    }

    @Test fun malformedEnvelopesFailAndRegionEligibilityMatchesMaimaid() {
        for (json in listOf("{}", """{"code":"error","data":{}}""", otogamePage(page = 0), otogamePage(total = -1))) {
            assertFailsWith<OtogameException> { OtogameImportPolicy.decode(Json.parseToJsonElement(json).jsonObject) }
        }
        assertTrue(OtogameImportPolicy.isEligible(ProfileServer.Jp))
        assertFalse(OtogameImportPolicy.isEligible(ProfileServer.Cn))
        assertFalse(OtogameImportPolicy.isEligible(ProfileServer.Intl))
        assertFalse(OtogameImportPolicy.isEligible(null))
    }
}
