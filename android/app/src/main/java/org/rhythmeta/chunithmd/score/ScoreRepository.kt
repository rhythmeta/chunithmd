package org.rhythmeta.chunithmd.score

import androidx.room.Room
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.rhythmeta.chunithmd.profile.ProfileRepository
import org.rhythmeta.chunithmd.shared.ChunithmScoreRules

@OptIn(ExperimentalCoroutinesApi::class)
class ScoreRepository(
    context: android.content.Context,
    private val profileRepository: ProfileRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        ScoreDatabase::class.java,
        "score-records.db",
    ).addMigrations(ScoreDatabase.MIGRATION_1_2).build()
    private val dao = database.records()

    fun observeSongRecords(songId: String): Flow<List<ScoreRecordEntity>> =
        profileRepository.activeProfile.flatMapLatest { profile ->
            profile?.let { dao.observeForSong(it.id, songId) } ?: flowOf(emptyList())
        }

    fun observeCurrentProfileRecords(): Flow<List<ScoreRecordEntity>> =
        profileRepository.activeProfile.flatMapLatest { profile ->
            profile?.let { dao.observeForProfile(it.id) } ?: flowOf(emptyList())
        }

    suspend fun save(
        songId: String,
        sheetKey: String,
        score: Int,
        clear: ClearType = ClearType.Clear,
        fullCombo: FullComboType? = null,
        fullChain: FullChainType? = null,
    ): ScoreRecordEntity {
        require(ChunithmScoreRules.isValid(score)) { "Score is out of range." }
        val profile = requireNotNull(profileRepository.activeProfile.first()) { "No active profile." }
        val now = clock()
        val record = ScoreRecordEntity(
            id = idFactory(),
            profileId = profile.id,
            songId = songId,
            sheetKey = sheetKey,
            score = score,
            rank = ChunithmScoreRules.rank(score),
            playedAt = now,
            clear = clear.wireValue,
            fullCombo = fullCombo?.wireValue,
            fullChain = fullChain?.wireValue,
        )
        dao.insert(record)
        return record
    }

    suspend fun delete(id: String) {
        val profile = profileRepository.activeProfile.first() ?: return
        val record = dao.find(id) ?: return
        if (record.profileId == profile.id) dao.delete(record)
    }
}
