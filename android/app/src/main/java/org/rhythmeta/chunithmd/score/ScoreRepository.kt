package org.rhythmeta.chunithmd.score

import org.rhythmeta.chunithmd.shared.localization.tr

import androidx.room.Room
import androidx.room.withTransaction
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.rhythmeta.chunithmd.profile.ProfileRepository
import org.rhythmeta.chunithmd.shared.ChunithmScoreRules
import org.rhythmeta.chunithmd.shared.ClearType
import org.rhythmeta.chunithmd.shared.FullChainType
import org.rhythmeta.chunithmd.shared.FullComboType
import org.rhythmeta.chunithmd.shared.ScoreRecord
import org.rhythmeta.chunithmd.shared.ScoreStore

@OptIn(ExperimentalCoroutinesApi::class)
class ScoreRepository(
    context: android.content.Context,
    private val profileRepository: ProfileRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) : ScoreStore {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        ScoreDatabase::class.java,
        "score-records.db",
    ).addMigrations(ScoreDatabase.MIGRATION_1_2, ScoreDatabase.MIGRATION_2_3).build()
    private val dao = database.records()

    suspend fun importDivingFish(
        profileId: String,
        payload: org.rhythmeta.chunithmd.shared.importing.DivingFishPayload,
        catalog: org.rhythmeta.chunithmd.shared.CatalogBundle,
    ): org.rhythmeta.chunithmd.shared.importing.ScoreImportResult =
        profileRepository.withActiveProfile(profileId) {
            database.withTransaction {
                val plan = org.rhythmeta.chunithmd.shared.importing.DivingFishImportPolicy.plan(
                    payload, catalog, profileId, dao.forProfile(profileId).map { it.toDomain() }, clock(),
                )
                plan.records.forEach { dao.insert(ScoreRecordEntity.fromDomain(it)) }
                plan.result
            }
        }

    suspend fun importLxns(
        profileId: String,
        payload: org.rhythmeta.chunithmd.shared.importing.LxnsPayload,
        catalog: org.rhythmeta.chunithmd.shared.CatalogBundle,
    ): org.rhythmeta.chunithmd.shared.importing.ScoreImportResult =
        profileRepository.withActiveProfile(profileId) {
            database.withTransaction {
                val plan = org.rhythmeta.chunithmd.shared.importing.LxnsImportPolicy.plan(
                    payload, catalog, profileId, dao.forProfile(profileId).map { it.toDomain() }, clock(),
                )
                plan.records.forEach { dao.insert(ScoreRecordEntity.fromDomain(it)) }
                plan.result
            }
        }

    suspend fun exportRecords(): List<ScoreRecord> = dao.all().map { it.toDomain() }

    suspend fun replaceRecords(records: List<ScoreRecord>) = database.withTransaction {
        dao.deleteAll()
        records.forEach { dao.insert(ScoreRecordEntity.fromDomain(it)) }
    }

    suspend fun save(songId: String, sheetKey: String, score: Int): ScoreRecord = save(
        songId = songId,
        sheetKey = sheetKey,
        score = score,
        clear = null,
        fullCombo = null,
        fullChain = null,
    )

    override fun observeSongRecords(songId: String): Flow<List<ScoreRecord>> =
        profileRepository.activeProfile.flatMapLatest { profile ->
            profile?.let { dao.observeForSong(it.id, songId).map { records -> records.map(ScoreRecordEntity::toDomain) } }
                ?: flowOf(emptyList())
        }

    override fun observeCurrentProfileRecords(): Flow<List<ScoreRecord>> =
        profileRepository.activeProfile.flatMapLatest { profile ->
            profile?.let { dao.observeForProfile(it.id).map { records -> records.map(ScoreRecordEntity::toDomain) } }
                ?: flowOf(emptyList())
        }

    override suspend fun save(
        songId: String,
        sheetKey: String,
        score: Int,
        clear: ClearType?,
        fullCombo: FullComboType?,
        fullChain: FullChainType?,
    ): ScoreRecord {
        require(ChunithmScoreRules.isValid(score)) { tr("Score is out of range.") }
        val profile = requireNotNull(profileRepository.activeProfile.first()) { tr("No active profile.") }
        val now = clock()
        val record = ScoreRecord(
            id = idFactory(),
            profileId = profile.id,
            songId = songId,
            sheetKey = sheetKey,
            score = score,
            rank = ChunithmScoreRules.rank(score),
            playedAt = now,
            clear = clear?.wireValue,
            fullCombo = fullCombo?.wireValue,
            fullChain = fullChain?.wireValue,
        )
        dao.insert(ScoreRecordEntity.fromDomain(record))
        return record
    }

    override suspend fun delete(id: String) {
        val profile = profileRepository.activeProfile.first() ?: return
        val record = dao.find(id) ?: return
        if (record.profileId == profile.id) dao.delete(record)
    }
}
