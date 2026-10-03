package org.rhythmeta.chunithmd.shared.community

import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import org.rhythmeta.chunithmd.shared.account.RhythmetaApiError
import kotlin.time.Clock

/** Shared requests, account isolation and UI state for native clients. */
class CommunityAliasStore(
    private val api: CommunityAliasApi,
    private val cache: CommunityAliasCache,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val mutableState = MutableStateFlow(CommunityAliasState(
        accountId = api.accountId,
        approvedAliases = runCatching { mergeCommunityAliases(cache.read()) }.getOrDefault(emptyMap()),
    ))
    val state = mutableState.asStateFlow()
    private val syncMutex = Mutex()
    private var lastSync: Long? = null
    private var accountGeneration = 0L
    private var boardOffset = 0
    private var personalRevision = 0L

    private data class AccountKey(val id: String?, val generation: Long)
    private fun account(): AccountKey {
        if (mutableState.value.accountId != api.accountId) {
            accountGeneration++
            personalRevision++
            boardOffset = 0
            mutableState.update { CommunityAliasState(accountId = api.accountId, approvedAliases = it.approvedAliases, syncError = it.syncError) }
        }
        return AccountKey(api.accountId, accountGeneration)
    }
    private fun isCurrent(key: AccountKey): Boolean = key == account()

    suspend fun observeAccount() {
        api.accountChanges.distinctUntilChanged().collectLatest {
            account()
            refreshPersonalAliases()
        }
    }

    suspend fun refreshPersonalAliases() {
        val owner = account()
        if (owner.id == null) return
        val revision = personalRevision
        try {
            val rows = json.decodeFromJsonElement<CommunityRows<CommunityCandidate>>(
                api.request("$Base/candidates:my?limit=200"),
            ).rows
            if (isCurrent(owner) && revision == personalRevision) {
                mutableState.update { it.copy(personalAliases = rows.searchableAliases()) }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* A song-level refresh exposes errors without blocking offline search. */ }
    }

    suspend fun syncApproved(force: Boolean = false) = syncMutex.withLock {
        val previous = lastSync
        if (!force && previous != null && nowMillis() - previous in 0 until 600_000) return@withLock
        try {
            val snapshot = json.decodeFromJsonElement<CommunityAliasSnapshot>(
                api.request("$Base/aliases:sync", authenticated = false),
            )
            check(snapshot.complete) { "社区别名同步不完整，请重试。" }
            val aliases = snapshot.rows.filter { it.status == "approved" }
                .groupBy { it.songIdentifier }.mapValues { (_, rows) -> rows.map { it.aliasText } }
                .let { mergeCommunityAliases(it) }
            cache.write(aliases)
            mutableState.update { it.copy(approvedAliases = aliases, syncError = null) }
            lastSync = nowMillis()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { mutableState.update { it.copy(syncError = errorMessage(failure)) } }
    }

    fun consumeSongFeedback(songId: String) {
        updateSong(songId) { it.copy(message = null, error = null) }
    }

    fun consumeBoardError() {
        mutableState.update { it.copy(boardError = null) }
    }

    fun setDraft(songId: String, value: String) {
        account()
        updateSong(songId) { it.copy(draft = value.take(CommunityAliasMaxLength), message = null, error = null) }
    }

    suspend fun refreshSong(songId: String) {
        val owner = account()
        if (owner.id == null || state.value.song(songId).loading) return
        updateSong(songId) { it.copy(loading = true, error = null) }
        val revision = personalRevision
        try {
            val rows = json.decodeFromJsonElement<CommunityRows<CommunityCandidate>>(
                api.request("$Base/candidates:my?limit=200&songIdentifier=${songId.encodeURLParameter()}"),
            ).rows.filter { it.songIdentifier == songId }
            if (!isCurrent(owner)) return
            // Ignore a read started before a successful submission changed personal search aliases.
            if (revision == personalRevision) {
                personalRevision++
                mutableState.update { it.copy(personalAliases = mergeCommunityAliases(it.personalAliases - songId, rows.searchableAliases())) }
                updateSong(songId) { it.copy(candidates = rows) }
            }
            val count = json.decodeFromJsonElement<CommunityDailyCount>(api.request("$Base/candidates:dailyCount")).count
            if (isCurrent(owner)) mutableState.update { it.copy(dailyUsed = count.coerceIn(0, CommunityAliasDailyQuota)) }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { if (isCurrent(owner)) updateSong(songId) { it.copy(error = errorMessage(failure)) } }
        finally { if (isCurrent(owner)) updateSong(songId) { it.copy(loading = false) } }
    }

    suspend fun submit(songId: String) {
        val owner = account()
        val current = state.value
        if (current.song(songId).submitting || current.song(songId).loading) return
        if (owner.id == null) {
            updateSong(songId) { it.copy(error = "请先登录 Rhythmeta 账户。") }; return
        }
        val draft = current.song(songId).draft.trim()
        if (songId.isBlank() || draft.length !in 1..CommunityAliasMaxLength) {
            updateSong(songId) { it.copy(error = "请输入 1–64 个字符的有效别名。") }; return
        }
        if ((current.dailyUsed ?: 0) >= CommunityAliasDailyQuota) {
            updateSong(songId) { it.copy(error = "今日投稿次数已用完，请明天再试。") }; return
        }
        updateSong(songId) { it.copy(submitting = true, message = null, error = null) }
        try {
            val result = json.decodeFromJsonElement<CommunitySubmitResult>(api.request(
                "$Base/candidates", "POST", buildJsonObject { put("songIdentifier", songId); put("aliasText", draft) },
            ))
            if (!isCurrent(owner)) return
            if (result.status == "created") {
                personalRevision++
                mutableState.update { it.copy(personalAliases = mergeCommunityAliases(it.personalAliases, mapOf(songId to listOf(draft)))) }
            }
            mutableState.update { it.copy(dailyUsed = result.quotaRemaining?.let { remaining ->
                (CommunityAliasDailyQuota - remaining).coerceIn(0, CommunityAliasDailyQuota)
            } ?: if (result.status == "quota_exceeded") CommunityAliasDailyQuota else it.dailyUsed) }
            val detail = result.similarAliases.takeIf { it.isNotEmpty() }?.joinToString("、", prefix = "\n相似别名：").orEmpty()
            updateSong(songId) { it.copy(
                draft = if (result.status == "created") "" else it.draft,
                message = if (result.status == "created") result.displayMessage else null,
                error = if (result.status == "created") null else result.displayMessage + detail,
            ) }
            if (result.status == "created") refreshSong(songId)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { if (isCurrent(owner)) updateSong(songId) { it.copy(error = errorMessage(failure)) } }
        finally { if (isCurrent(owner)) updateSong(songId) { it.copy(submitting = false) } }
    }

    suspend fun refreshBoard(loadMore: Boolean = false) {
        val owner = account()
        if (state.value.boardLoading || state.value.votingId != null || (loadMore && !state.value.boardHasMore)) return
        val offset = if (loadMore) boardOffset else 0
        mutableState.update { it.copy(boardLoading = true, boardError = null) }
        try {
            val rows = json.decodeFromJsonElement<CommunityRows<CommunityCandidate>>(
                api.request("$Base/candidates:votingBoard?limit=$PageSize&offset=$offset", authenticated = owner.id != null),
            ).rows
            if (!isCurrent(owner)) return
            boardOffset = offset + rows.size
            mutableState.update { it.copy(board = ((if (loadMore) it.board else emptyList()) + rows).distinctBy { row -> row.candidateId }, boardHasMore = rows.size == PageSize) }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { if (isCurrent(owner)) mutableState.update { it.copy(boardError = errorMessage(failure)) } }
        finally { if (isCurrent(owner)) mutableState.update { it.copy(boardLoading = false) } }
    }

    suspend fun vote(candidateId: String, support: Boolean) {
        val owner = account()
        if (owner.id == null) {
            mutableState.update { it.copy(boardError = "请先登录 Rhythmeta 账户。") }; return
        }
        if (state.value.votingId != null || state.value.boardLoading) return
        mutableState.update { it.copy(votingId = candidateId, boardError = null) }
        try {
            val result = json.decodeFromJsonElement<CommunityVoteResult>(api.request(
                "$Base/candidates/${candidateId.encodeURLParameter()}:vote", "POST",
                buildJsonObject { put("vote", if (support) 1 else -1) },
            ))
            if (isCurrent(owner)) mutableState.update { state -> state.copy(board = state.board.map {
                if (it.candidateId == result.candidateId) it.copy(supportCount = result.supportCount, opposeCount = result.opposeCount, myVote = result.myVote) else it
            }) }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { if (isCurrent(owner)) mutableState.update { it.copy(boardError = errorMessage(failure)) } }
        finally { if (isCurrent(owner)) mutableState.update { it.copy(votingId = null) } }
    }

    private fun updateSong(id: String, change: (SongAliasState) -> SongAliasState) {
        mutableState.update { it.copy(songs = it.songs + (id to change(it.song(id)))) }
    }

    private fun errorMessage(error: Exception): String = when (error) {
        is RhythmetaApiError -> when (error.status) {
            401 -> "登录已失效，请重新登录 Rhythmeta 账户。"
            404 -> "歌曲或候选别名已不存在，请刷新后重试。"
            429 -> "操作过于频繁，请稍后再试。"
            in 500..599 -> "社区服务暂时不可用，请稍后重试。"
            else -> error.message ?: "操作失败，请刷新后重试。"
        }
        else -> "无法加载社区数据，请检查网络后重试。"
    }

    private companion object {
        const val Base = "chunithmd/v1/community"
        const val PageSize = 100
    }
}
