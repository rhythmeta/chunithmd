package org.rhythmeta.chunithmd.shared.backup

import org.rhythmeta.chunithmd.shared.*
import org.rhythmeta.chunithmd.shared.importing.normalizeDivingFishRecordId

fun UserProfile.toBackup(avatar: ByteArray) = BackupProfile(id=id, name=name, server=server.wireValue, title=title.orEmpty(), avatar=avatar, active=isActive, createdAt=createdAt)
fun BackupProfile.toProfile(avatarPath: String?) = UserProfile(id=id, name=name, server=ProfileServer.fromWire(server), title=title.ifEmpty { null }, avatarPath=avatarPath, isActive=active, createdAt=createdAt)
fun ScoreRecord.toBackup() = BackupPlayRecord(normalizeDivingFishRecordId(id), BackupScore(profileId=profileId, chartKey=sheetKey, songId=songId, score=score, rank=rank, achievedAt=playedAt, clear=clear.orEmpty(), fc=fullCombo.orEmpty(), fs=fullChain.orEmpty()))
fun BackupPlayRecord.toRecord() = result.let { ScoreRecord(id=id, profileId=it.profileId, songId=it.songId, sheetKey=it.chartKey, score=it.score, rank=it.rank, playedAt=it.achievedAt, clear=it.clear.takeIf(String::isNotBlank), fullCombo=it.fc.ifEmpty { null }, fullChain=it.fs.ifEmpty { null }) }

/** Export only: separate native stores can retain scores belonging to deleted profiles. */
fun BackupSnapshot.withScoresForExistingProfiles(): BackupSnapshot {
    val profileIds = profiles.mapTo(mutableSetOf()) { it.id }
    return copy(
        scores = scores.filter { it.profileId in profileIds },
        playRecords = playRecords.filter { it.result.profileId in profileIds },
    )
}
