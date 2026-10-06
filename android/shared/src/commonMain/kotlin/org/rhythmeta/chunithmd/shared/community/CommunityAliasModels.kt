package org.rhythmeta.chunithmd.shared.community

import org.rhythmeta.chunithmd.shared.localization.tr

import kotlinx.serialization.Serializable
import org.rhythmeta.chunithmd.shared.CatalogBundle

const val CommunityAliasDailyQuota = 5
const val CommunityAliasMaxLength = 64

@Serializable
data class CommunityCandidate(
    val candidateId: String,
    val songIdentifier: String,
    val aliasText: String,
    val status: String = "voting",
    val submitterHandle: String? = null,
    val voteCloseAt: String? = null,
    val supportCount: Int = 0,
    val opposeCount: Int = 0,
    val myVote: Int? = null,
)

@Serializable
data class CommunitySubmitResult(
    val status: String = "error",
    val duplicateReason: String? = null,
    val similarAliases: List<String> = emptyList(),
    val quotaRemaining: Int? = null,
) {
    val displayMessage: String get() = when (status) {
        "created" -> tr("投稿成功，已进入社区投票；你现在可以用这个别名搜索歌曲。")
        "quota_exceeded" -> tr("今日投稿次数已用完，请明天再试。")
        "unauthenticated" -> tr("请先登录 Rhythmeta 账户。")
        "invalid_request" -> tr("请输入 1–64 个字符的有效别名。")
        "rejected_duplicate" -> when (duplicateReason) {
            "lxns_existing" -> tr("曲库中已存在这个别名。")
            "admin_rejected_locked" -> tr("这个别名已被管理员驳回，暂时不能再次投稿。")
            else -> tr("社区中已存在这个别名或相同投稿。")
        }
        else -> tr("投稿失败，请稍后重试。")
    }
}

@Serializable
internal data class CommunityRows<T>(val rows: List<T>)
@Serializable
internal data class CommunityDailyCount(val count: Int)
@Serializable
internal data class CommunityVoteResult(val candidateId: String, val supportCount: Int, val opposeCount: Int, val myVote: Int? = null)
@Serializable
internal data class CommunityApprovedAlias(val songIdentifier: String, val aliasText: String, val status: String = "approved")
@Serializable
internal data class CommunityAliasSnapshot(val rows: List<CommunityApprovedAlias>, val complete: Boolean = false)

@Serializable
data class SongAliasState(
    val draft: String = "",
    val loading: Boolean = false,
    val submitting: Boolean = false,
    val candidates: List<CommunityCandidate> = emptyList(),
    val message: String? = null,
    val error: String? = null,
)

@Serializable
data class CommunityAliasState(
    val accountId: String? = null,
    val approvedAliases: Map<String, List<String>> = emptyMap(),
    val personalAliases: Map<String, List<String>> = emptyMap(),
    val songs: Map<String, SongAliasState> = emptyMap(),
    val dailyUsed: Int? = null,
    val board: List<CommunityCandidate> = emptyList(),
    val boardLoading: Boolean = false,
    val boardHasMore: Boolean = true,
    val votingId: String? = null,
    val boardError: String? = null,
    val syncError: String? = null,
) {
    val authenticated: Boolean get() = accountId != null
    fun song(id: String): SongAliasState = songs[id] ?: SongAliasState()
    fun canSubmit(id: String): Boolean = authenticated && !song(id).submitting && !song(id).loading &&
        song(id).draft.trim().length in 1..CommunityAliasMaxLength &&
        (dailyUsed ?: 0) < CommunityAliasDailyQuota
}

/** Keep static, approved and personal aliases separate so revocations never delete static aliases. */
fun mergeCommunityAliases(vararg sources: Map<String, List<String>>): Map<String, List<String>> =
    sources.flatMap { it.keys }.distinct().associateWith { id ->
        sources.flatMap { it[id].orEmpty() }.map(String::trim).filter(String::isNotEmpty).distinctBy(String::lowercase)
    }.filterValues { it.isNotEmpty() }

fun CatalogBundle.withCommunityAliases(state: CommunityAliasState): CatalogBundle =
    copy(aliases = mergeCommunityAliases(aliases, state.approvedAliases, state.personalAliases))

internal fun List<CommunityCandidate>.searchableAliases(): Map<String, List<String>> =
    filter { it.status in setOf("pool_private", "voting", "approved") }
        .groupBy(CommunityCandidate::songIdentifier)
        .mapValues { (_, rows) -> rows.map(CommunityCandidate::aliasText) }
        .let { mergeCommunityAliases(it) }
