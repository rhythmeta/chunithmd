package org.rhythmeta.chunithmd.shared.backup

import kotlin.test.*
import kotlinx.coroutines.test.runTest
import org.rhythmeta.chunithmd.shared.account.CloudBackup

class BackupTests {
    private fun snapshot(name:String="Before")=BackupSnapshot(magic="RHYTHMETA_BACKUP",formatVersion=1,game="chunithmd",createdAt=1790852400000,
        profiles=listOf(BackupProfile(id="10000000-0000-4000-8000-000000000001",name=name,server="jp",active=true)))
    private val metadata=CloudBackup("id","chunithmd",1,1,1,"hash","test","1",1,"now","https://example.invalid/backup")
    @Test fun roundTripAndCorruptInput() {
        val source=snapshot().copy(playRecords=listOf(BackupPlayRecord("20000000-0000-4000-8000-000000000001",BackupScore(profileId=snapshot().profiles[0].id,chartKey="test|master",songId="test",score=1009000))),settings=listOf(BackupSetting(key="android.chunithmd.theme.color_mode",kind="int",integerValue=2)))
        val bytes=BackupCodec.encode(source)
        val decoded=BackupCodec.decode(bytes)
        assertEquals(source.profiles[0].name,decoded.profiles[0].name)
        assertEquals(1009000,decoded.playRecords[0].result.score)
        assertEquals(2,decoded.settings[0].integerValue)
        assertFails { BackupCodec.decode(bytes.copyOf(bytes.size-5)) }
        assertFails { BackupCodec.validate(source.copy(game="maimaid")) }
        assertFails { BackupCodec.validate(source.copy(profiles=emptyList())) }
    }
    @Test fun invalidPreferenceCannotReachNativeStorage() {
        val wrongType = BackupSetting(key="android.chunithmd.theme.color_mode", kind="string", stringValue="bad")
        assertFails { BackupCodec.validate(snapshot().copy(settings=listOf(wrongType))) }
        assertFails { BackupSettings.validate(wrongType.copy(kind="int",integerValue=Long.MAX_VALUE)) }
        assertFails { BackupSettings.validate(BackupSetting(key="android.chunithmd.catalog.min_level",kind="float",doubleValue=Double.NaN)) }
    }
    @Test fun successfulRestoreReplacesAndClearsJournal()=runTest {
        val store=MemoryStore(snapshot())
        BackupCoordinator(Remote(snapshot("After")),store).restore(metadata)
        assertEquals("After",store.value.profiles[0].name)
        assertNull(store.journal)
    }
    @Test fun failedReplacementRollsBackAndClearsJournal()=runTest {
        val store=MemoryStore(snapshot(),failNext=true)
        assertFails { BackupCoordinator(Remote(snapshot("After")),store).restore(metadata) }
        assertEquals("Before",store.value.profiles[0].name)
        assertNull(store.journal)
    }
    @Test fun restartRecoversDurableJournalBeforeBackup()=runTest {
        val store=MemoryStore(snapshot("Partially replaced"),journal=BackupCodec.encode(snapshot()))
        val remote=Remote(snapshot("After"))
        BackupCoordinator(remote,store).backup("test")
        assertEquals("Before",remote.uploaded?.profiles?.first()?.name)
        assertNull(store.journal)
    }
    @Test fun failedRollbackKeepsJournalForNextLaunch()=runTest {
        val store=MemoryStore(snapshot(),failAlways=true)
        assertFails { BackupCoordinator(Remote(snapshot("After")),store).restore(metadata) }
        assertNotNull(store.journal)
        store.failAlways=false
        BackupCoordinator(Remote(snapshot("After")),store).recover()
        assertEquals("Before",store.value.profiles[0].name)
        assertNull(store.journal)
    }
    private class Remote(val target:BackupSnapshot):BackupRemote {
        var uploaded:BackupSnapshot?=null
        override suspend fun backup(snapshot:BackupSnapshot,deviceName:String){uploaded=snapshot}
        override suspend fun download(backup:CloudBackup)=target
    }
    private class MemoryStore(var value:BackupSnapshot,var failNext:Boolean=false,var journal:ByteArray?=null,var failAlways:Boolean=false):LocalSnapshotStore {
        override suspend fun exportSnapshot()=value
        override suspend fun replaceSnapshot(snapshot:BackupSnapshot){value=snapshot;if(failNext||failAlways){failNext=false;error("Write interrupted")}}
        override suspend fun readRollback()=journal
        override suspend fun writeRollback(bytes:ByteArray){journal=bytes}
        override suspend fun clearRollback(){journal=null}
    }
}
