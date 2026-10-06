package org.rhythmeta.chunithmd.shared

import org.rhythmeta.chunithmd.shared.localization.tr

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
enum class ProfileServer(val wireValue: String) {
    @SerialName("jp") Jp("jp"),
    @SerialName("intl") Intl("intl"),
    @SerialName("cn") Cn("cn"),
    ;

    companion object {
        fun fromWire(value: String?): ProfileServer = entries.firstOrNull {
            it.wireValue.equals(value?.trim(), ignoreCase = true)
        } ?: Jp
    }
}

@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val server: ProfileServer = ProfileServer.Jp,
    val title: String? = null,
    val avatarPath: String? = null,
    val isActive: Boolean = false,
    val createdAt: Long,
)

@Serializable
data class ProfileDraft(
    val name: String = "",
    val server: ProfileServer = ProfileServer.Jp,
    val title: String = "",
)

/** Stable profile codec: old records may omit newer fields or contain an unknown server. */
object ProfileJson {
    private val codec = Json { ignoreUnknownKeys = true; explicitNulls = false }

    fun encode(profile: UserProfile): String = codec.encodeToString(UserProfile.serializer(), profile)

    fun decode(source: String): UserProfile {
        val objectValue = codec.parseToJsonElement(source).jsonObject
        val server = ProfileServer.fromWire(objectValue["server"]?.jsonPrimitive?.contentOrNull)
        val normalized = JsonObject(objectValue + ("server" to JsonPrimitive(server.wireValue)))
        return codec.decodeFromJsonElement(UserProfile.serializer(), normalized)
    }
}

enum class ProfileValidationError {
    EmptyName,
}

fun ProfileDraft.normalized(): ProfileDraft = copy(
    name = name.trim(),
    title = title.trim(),
)

fun ProfileDraft.validationErrors(): List<ProfileValidationError> = buildList {
    if (normalized().name.isEmpty()) add(ProfileValidationError.EmptyName)
}

fun ProfileDraft.isValid(): Boolean = validationErrors().isEmpty()

fun defaultProfile(id: String, createdAt: Long): UserProfile = UserProfile(
    id = id,
    name = tr("我的档案"),
    server = ProfileServer.Jp,
    title = null,
    avatarPath = null,
    isActive = true,
    createdAt = createdAt,
)

fun List<UserProfile>.sortedForDisplay(): List<UserProfile> = sortedWith(
    compareBy<UserProfile> { it.createdAt }
        .thenBy { it.id },
)

fun List<UserProfile>.activateProfile(id: String): List<UserProfile> =
    if (any { it.id == id }) map { profile -> profile.copy(isActive = profile.id == id) } else this

fun UserProfile.canDelete(): Boolean = !isActive

// Native clients persist profiles through their platform adapters.
