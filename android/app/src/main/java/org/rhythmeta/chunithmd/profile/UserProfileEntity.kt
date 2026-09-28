package org.rhythmeta.chunithmd.profile

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.rhythmeta.chunithmd.shared.ProfileServer
import org.rhythmeta.chunithmd.shared.UserProfile

@Entity(tableName = "user_profiles", indices = [Index("isActive")])
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val server: String,
    val title: String?,
    val avatarPath: String?,
    val isActive: Boolean,
    val createdAt: Long,
)

fun UserProfileEntity.toShared(): UserProfile = UserProfile(
    id = id,
    name = name,
    server = ProfileServer.fromWire(server),
    title = title,
    avatarPath = avatarPath,
    isActive = isActive,
    createdAt = createdAt,
)

fun UserProfile.toEntity(): UserProfileEntity = UserProfileEntity(
    id = id,
    name = name,
    server = server.wireValue,
    title = title,
    avatarPath = avatarPath,
    isActive = isActive,
    createdAt = createdAt,
)
