package org.rhythmeta.chunithmd.shared.community

import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import org.rhythmeta.chunithmd.shared.*
import org.rhythmeta.chunithmd.shared.account.RhythmetaApiError

@OptIn(ExperimentalCoroutinesApi::class)
class CommunityAliasStoreTest {
    private class MemoryCache(var aliases: Map<String, List<String>> = emptyMap()) : CommunityAliasCache {
        override fun read() = aliases
        override fun write(aliases: Map<String, List<String>>) { this.aliases = aliases }
    }
    private data class Call(val path: String, val method: String, val body: JsonElement?, val authenticated: Boolean)
    private class FakeApi(user: String? = "user-a") : CommunityAliasApi {
        val owner = MutableStateFlow(user)
        override val accountId get() = owner.value
        override val accountChanges = owner
        val calls = mutableListOf<Call>()
        var handler: suspend (Call) -> JsonElement = { call -> when {
            call.path.contains("dailyCount") -> payload("""{"count":0}""")
            call.path.contains("aliases:sync") -> payload("""{"rows":[],"complete":true}""")
            else -> payload("""{"rows":[]}""")
        } }
        override suspend fun request(path: String, method: String, body: JsonElement?, authenticated: Boolean): JsonElement {
            val call = Call(path, method, body, authenticated)
            calls += call
            return handler(call)
        }
    }
    private companion object {
        fun payload(value: String) = Json.parseToJsonElement(value)
        fun row(id: String = "candidate", alias: String = "昵称", status: String = "voting") =
            """{"candidateId":"$id","songIdentifier":"song","aliasText":"$alias","status":"$status","supportCount":3,"opposeCount":1,"myVote":1}"""
    }

    @Test fun submissionUsesGameScopeTrimsDraftAndImmediatelyEnablesSearch() = runTest {
        val api = FakeApi()
        api.handler = { call -> when {
            call.method == "POST" -> payload("""{"status":"created","quotaRemaining":4,"candidate":{"id":"candidate","extra":true}}""")
            call.path.contains("dailyCount") -> payload("""{"count":1}""")
            else -> payload("""{"rows":[${row()}]}""")
        } }
        val store = CommunityAliasStore(api, MemoryCache())
        store.setDraft("song", "  昵称  ")
        store.submit("song")
        val request = api.calls.first()
        assertEquals("chunithmd/v1/community/candidates", request.path)
        assertEquals("POST", request.method)
        assertTrue(request.authenticated)
        assertEquals("昵称", request.body!!.jsonObject["aliasText"]!!.jsonPrimitive.content)
        assertEquals("", store.state.value.song("song").draft)
        assertEquals(1, store.state.value.dailyUsed)
        assertEquals("candidate", store.state.value.song("song").candidates.single().candidateId)
        val catalog = CatalogBundle(1, Catalog(songs = listOf(CatalogSong("song", "Title"))))
        assertEquals("song", CatalogQuery.filterAndSort(catalog.withCommunityAliases(store.state.value), search = "昵称").single().songId)
    }

    @Test fun duplicateAndQuotaResponsesPreserveDraftAndDoNotAddPersonalAlias() = runTest {
        val api = FakeApi()
        val store = CommunityAliasStore(api, MemoryCache())
        api.handler = { payload("""{"status":"rejected_duplicate","duplicateReason":"admin_rejected_locked","similarAliases":["重名"]}""") }
        store.setDraft("song", "重名")
        store.submit("song")
        assertEquals("重名", store.state.value.song("song").draft)
        assertTrue(store.state.value.song("song").error!!.contains("管理员"))
        assertTrue(store.state.value.personalAliases.isEmpty())
        api.handler = { payload("""{"status":"quota_exceeded","quotaRemaining":0}""") }
        store.submit("song")
        assertEquals(5, store.state.value.dailyUsed)
        assertFalse(store.state.value.canSubmit("song"))
        val count = api.calls.size
        store.submit("song")
        assertEquals(count, api.calls.size)
    }

    @Test fun unauthenticatedAndEmptySubmissionsNeverReachNetwork() = runTest {
        val api = FakeApi(null)
        val store = CommunityAliasStore(api, MemoryCache())
        store.setDraft("song", "昵称")
        store.submit("song")
        assertTrue(api.calls.isEmpty())
        api.owner.value = "user"
        store.setDraft("song", "  ")
        store.submit("song")
        assertTrue(api.calls.isEmpty())
        store.setDraft("song", "A".repeat(65))
        assertEquals(64, store.state.value.song("song").draft.length)
    }

    @Test fun voteUsesServerToggleAndUpdatedCounts() = runTest {
        val api = FakeApi()
        api.handler = { call -> if (call.method == "POST") payload("""{"candidateId":"candidate","supportCount":2,"opposeCount":1,"myVote":null}""")
            else payload("""{"rows":[${row()}]}""") }
        val store = CommunityAliasStore(api, MemoryCache())
        store.refreshBoard()
        assertEquals(1, store.state.value.board.single().myVote)
        store.vote("candidate", true)
        assertNull(store.state.value.board.single().myVote)
        assertEquals(2, store.state.value.board.single().supportCount)
        assertEquals("chunithmd/v1/community/candidates/candidate:vote", api.calls.last().path)
        assertEquals(1, api.calls.last().body!!.jsonObject["vote"]!!.jsonPrimitive.int)
        assertNull(store.state.value.votingId)
    }

    @Test fun publicBoardLoadsMoreWithoutDuplicatesAndEscapesSongIdentifiers() = runTest {
        val api = FakeApi(null)
        api.handler = { call -> if (call.path.endsWith("offset=0")) payload("""{"rows":[${(0..99).joinToString(",") { row("id-$it") }}]}""")
            else payload("""{"rows":[${row("id-99")},${row("id-100")}]}""") }
        val store = CommunityAliasStore(api, MemoryCache())
        store.refreshBoard()
        assertTrue(store.state.value.boardHasMore)
        store.refreshBoard(loadMore = true)
        assertEquals(101, store.state.value.board.size)
        assertFalse(store.state.value.boardHasMore)
        assertTrue(api.calls.last().path.endsWith("offset=100"))
        assertTrue(api.calls.none { it.authenticated })
        api.owner.value = "user"
        api.handler = { if (it.path.contains("dailyCount")) payload("""{"count":0}""") else payload("""{"rows":[]}""") }
        store.refreshSong("song & other")
        assertTrue(api.calls.any { it.path.contains("songIdentifier=song%20%26%20other") })
    }

    @Test fun completeSyncReplacesCacheButFailedOrIncompleteSyncPreservesIt() = runTest {
        val cache = MemoryCache(mapOf("song" to listOf("旧别名")))
        val api = FakeApi(null)
        val store = CommunityAliasStore(api, cache)
        api.handler = { payload("""{"rows":[{"songIdentifier":"song","aliasText":"新别名","status":"approved"},{"songIdentifier":"song","aliasText":"已撤销","status":"rejected"}],"complete":true}""") }
        store.syncApproved()
        assertEquals(listOf("新别名"), cache.aliases["song"])
        api.handler = { payload("""{"rows":[],"complete":false}""") }
        store.syncApproved(force = true)
        assertEquals(listOf("新别名"), store.state.value.approvedAliases["song"])
        assertNotNull(store.state.value.syncError)
        api.handler = { throw RhythmetaApiError(503, "Unavailable") }
        store.syncApproved(force = true)
        assertEquals(listOf("新别名"), cache.aliases["song"])
        api.handler = { payload("""{"rows":[],"complete":true}""") }
        store.syncApproved(force = true)
        assertTrue(cache.aliases.isEmpty())
        val catalog = CatalogBundle(1, Catalog(emptyList(), songs = emptyList()), aliases = mapOf("song" to listOf("静态别名")))
        assertEquals(listOf("静态别名"), catalog.withCommunityAliases(store.state.value).aliases["song"])
    }

    @Test fun accountSwitchClearsPrivateAliasesAndDiscardsInflightSubmission() = runTest {
        val api = FakeApi()
        val response = CompletableDeferred<JsonElement>()
        api.handler = { if (it.method == "POST") response.await() else payload("""{"rows":[${row()}]}""") }
        val store = CommunityAliasStore(api, MemoryCache(mapOf("song" to listOf("公开别名"))))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { store.observeAccount() }
        runCurrent()
        assertEquals(listOf("昵称"), store.state.value.personalAliases["song"])
        store.setDraft("song", "在途投稿")
        val request = launch { store.submit("song") }
        runCurrent()
        api.owner.value = null
        runCurrent()
        assertTrue(store.state.value.personalAliases.isEmpty())
        response.complete(payload("""{"status":"created","quotaRemaining":4}"""))
        request.join()
        assertTrue(store.state.value.personalAliases.isEmpty())
        assertTrue(store.state.value.songs.isEmpty())
        assertEquals(listOf("公开别名"), store.state.value.approvedAliases["song"])
    }

    @Test fun cancelledRequestsReleaseLoadingAndVotingState() = runTest {
        val api = FakeApi()
        api.handler = { payload("""{"rows":[${row()}]}""") }
        val store = CommunityAliasStore(api, MemoryCache())
        store.refreshBoard()
        api.handler = { awaitCancellation() }
        val voting = launch { store.vote("candidate", false) }
        runCurrent()
        assertEquals("candidate", store.state.value.votingId)
        voting.cancelAndJoin()
        assertNull(store.state.value.votingId)
        val board = launch { store.refreshBoard() }
        runCurrent()
        assertTrue(store.state.value.boardLoading)
        board.cancelAndJoin()
        assertFalse(store.state.value.boardLoading)
        val song = launch { store.refreshSong("song") }
        runCurrent()
        song.cancelAndJoin()
        assertFalse(store.state.value.song("song").loading)
    }

    @Test fun onlySearchesActivePersonalCandidatesAndDeduplicatesAliases() = runTest {
        val api = FakeApi()
        api.handler = { payload("""{"rows":[${row("one", "alias", "approved")},${row("two", "Alias", "voting")},${row("three", "rejected alias", "rejected")}]}""") }
        val store = CommunityAliasStore(api, MemoryCache())
        store.refreshPersonalAliases()
        assertEquals(listOf("alias"), store.state.value.personalAliases["song"])
        assertEquals(listOf("Alias", "other"), mergeCommunityAliases(mapOf("song" to listOf("Alias")), mapOf("song" to listOf(" alias ", "other")))["song"])
    }
}
