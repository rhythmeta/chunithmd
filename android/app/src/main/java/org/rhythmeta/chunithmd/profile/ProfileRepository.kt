package org.rhythmeta.chunithmd.profile

import org.rhythmeta.chunithmd.shared.localization.tr

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.rhythmeta.chunithmd.shared.ProfileDraft
import org.rhythmeta.chunithmd.shared.UserProfile
import org.rhythmeta.chunithmd.shared.activateProfile
import org.rhythmeta.chunithmd.shared.defaultProfile
import org.rhythmeta.chunithmd.shared.isValid
import org.rhythmeta.chunithmd.shared.normalized
import org.rhythmeta.chunithmd.shared.sortedForDisplay

class ProfileRepository(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        ProfileDatabase::class.java,
        "profiles.db",
    ).build()
    private val dao = database.profiles()
    private val mutex = Mutex()

    suspend fun exportProfiles(): List<UserProfile> = dao.observeAllOnce().map(UserProfileEntity::toShared)

    suspend fun replaceProfiles(profiles: List<UserProfile>) = mutex.withLock {
        database.withTransaction { dao.deleteAll(); profiles.forEach { dao.upsert(it.toEntity()) } }
    }

    val profiles: Flow<List<UserProfile>> = dao.observeAll().map { entities -> entities.map(UserProfileEntity::toShared).sortedForDisplay() }
    val activeProfile: Flow<UserProfile?> = dao.observeActive().map { it?.toShared() }

    suspend fun ensureDefaultProfile(): UserProfile = mutex.withLock {
        dao.active()?.toShared()
            ?: dao.first()?.let { first ->
                val profile = first.toShared().copy(isActive = true)
                database.withTransaction {
                    dao.clearActive()
                    dao.upsert(profile.toEntity())
                }
                profile
            }
            ?: defaultProfile(UUID.randomUUID().toString(), System.currentTimeMillis()).also { profile ->
                dao.upsert(profile.toEntity())
            }
    }

    suspend fun create(draft: ProfileDraft): UserProfile {
        val normalized = draft.normalized()
        require(normalized.isValid()) { tr("Profile name cannot be empty.") }
        val profile = UserProfile(
            id = UUID.randomUUID().toString(),
            name = normalized.name,
            server = normalized.server,
            title = normalized.title.ifEmpty { null },
            createdAt = System.currentTimeMillis(),
            isActive = dao.active() == null,
        )
        dao.upsert(profile.toEntity())
        return profile
    }

    suspend fun update(profile: UserProfile, draft: ProfileDraft): UserProfile {
        val normalized = draft.normalized()
        require(normalized.isValid()) { tr("Profile name cannot be empty.") }
        val updated = profile.copy(
            name = normalized.name,
            server = normalized.server,
            title = normalized.title.ifEmpty { null },
        )
        dao.upsert(updated.toEntity())
        return updated
    }

    suspend fun activate(profile: UserProfile) = mutex.withLock {
        val all = dao.observeAllOnce().map(UserProfileEntity::toShared)
        database.withTransaction {
            all.activateProfile(profile.id).forEach { dao.upsert(it.toEntity()) }
        }
    }

    suspend fun delete(profile: UserProfile): Boolean {
        // Re-read the active id so a stale list item cannot delete the current profile.
        if (dao.active()?.id != profile.id) {
            dao.delete(profile.toEntity())
            return true
        }
        return false
    }

    suspend fun saveAvatar(profile: UserProfile, path: String?): UserProfile {
        val updated = profile.copy(avatarPath = path)
        dao.upsert(updated.toEntity())
        return updated
    }

    private suspend fun UserProfileDao.observeAllOnce(): List<UserProfileEntity> = observeAll().first()
}
