package org.rhythmeta.chunithmd.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileModelsTest {
    @Test
    fun defaultProfileUsesJapaneseServerAndActiveState() {
        val profile = defaultProfile("profile-1", 42L)

        assertEquals("我的档案", profile.name)
        assertEquals(ProfileServer.Jp, profile.server)
        assertTrue(profile.isActive)
    }

    @Test
    fun draftNormalizationAndValidationAreShared() {
        val draft = ProfileDraft(name = "  Player  ", title = "  Title  ")

        assertEquals(ProfileDraft("Player", ProfileServer.Jp, "Title"), draft.normalized())
        assertTrue(draft.isValid())
        assertEquals(listOf(ProfileValidationError.EmptyName), ProfileDraft(name = "  ").validationErrors())
    }

    @Test
    fun serverParsingFallsBackToJapanese() {
        assertEquals(ProfileServer.Cn, ProfileServer.fromWire(" CN "))
        assertEquals(ProfileServer.Jp, ProfileServer.fromWire("unknown"))
    }

    @Test
    fun activationAndDeletionRulesAreShared() {
        val profiles = listOf(
            UserProfile("a", "A", isActive = true, createdAt = 1),
            UserProfile("b", "B", createdAt = 2),
        )

        val activated = profiles.activateProfile("b")
        assertFalse(activated.first { it.id == "a" }.isActive)
        assertTrue(activated.first { it.id == "b" }.isActive)
        assertFalse(activated.first { it.id == "b" }.canDelete())
        assertTrue(activated.first { it.id == "a" }.canDelete())
    }

    @Test
    fun displaySortKeepsCreationOrderRegardlessOfActiveProfile() {
        val profiles = listOf(
            UserProfile("b", "B", createdAt = 2),
            UserProfile("a", "A", isActive = true, createdAt = 3),
            UserProfile("c", "C", createdAt = 1),
        )

        assertEquals(listOf("c", "b", "a"), profiles.sortedForDisplay().map(UserProfile::id))
    }

    @Test
    fun profileSerializationUsesStableServerWireValue() {
        val source = UserProfile("a", "A", ProfileServer.Cn, "Title", createdAt = 1)

        val encoded = ProfileJson.encode(source)
        val decoded = ProfileJson.decode(encoded)

        assertTrue(encoded.contains("\"server\":\"cn\""))
        assertEquals(source, decoded)
    }

    @Test
    fun profileSerializationDefaultsMissingFieldsAndUnknownServer() {
        val decoded = ProfileJson.decode("""{"id":"a","name":"A","server":"legacy","createdAt":1,"future":true}""")

        assertEquals(ProfileServer.Jp, decoded.server)
        assertEquals(null, decoded.title)
        assertEquals(null, decoded.avatarPath)
    }
}
