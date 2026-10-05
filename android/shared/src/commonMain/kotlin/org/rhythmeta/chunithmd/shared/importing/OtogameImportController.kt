package org.rhythmeta.chunithmd.shared.importing

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.rhythmeta.chunithmd.shared.ProfileServer
import org.rhythmeta.chunithmd.shared.localization.tr

data class OtogameImportState(
    val profileId: String? = null,
    val eligible: Boolean = false,
    val connected: Boolean = false,
    val busy: Boolean = false,
    val page: Int = 0,
    val totalPages: Int = OtogameImportPolicy.PAGE_LIMIT,
    val result: ScoreImportResult? = null,
    val error: String? = null,
)

/** Session headers stay in memory and are cleared when the active profile changes. */
class OtogameImportController(
    private val client: OtogameClient,
    private val scope: CoroutineScope,
    private val apply: suspend (String, OtogamePayload) -> ScoreImportResult,
) {
    private val mutableState = MutableStateFlow(OtogameImportState())
    val state = mutableState.asStateFlow()
    private var authorization: String? = null
    private var operation: Job? = null
    private var generation = 0

    fun selectProfile(id: String?, server: ProfileServer?) {
        val eligible = id != null && OtogameImportPolicy.isEligible(server)
        if (id == state.value.profileId && eligible == state.value.eligible) return
        cancel()
        authorization = null
        mutableState.value = OtogameImportState(profileId = id, eligible = eligible)
    }

    fun captureAuthorization(profileId: String, value: String) {
        val header = value.trim()
        if (profileId != state.value.profileId || !state.value.eligible || state.value.busy || !OtogameClient.isAuthorization(header)) return
        authorization = header
        mutableState.update { it.copy(connected = true, error = null) }
    }

    fun disconnect() {
        cancel()
        authorization = null
        mutableState.update { it.copy(connected = false, error = null, result = null) }
    }

    fun cancel() {
        generation++
        operation?.cancel()
        operation = null
        mutableState.update { it.copy(busy = false) }
    }

    fun importScores() {
        val snapshot = state.value
        val id = snapshot.profileId ?: return
        val header = authorization ?: return
        if (snapshot.busy || !snapshot.eligible) return
        val current = ++generation
        mutableState.update { it.copy(busy = true, page = 0, result = null, error = null) }
        operation = scope.launch {
            try {
                val payload = client.records(header) { page, total ->
                    if (current == generation) mutableState.update { it.copy(page = page, totalPages = total) }
                }
                currentCoroutineContext().ensureActive()
                val result = apply(id, payload)
                currentCoroutineContext().ensureActive()
                if (current == generation) mutableState.update { it.copy(result = result) }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                if (current == generation) {
                    val code = (error as? OtogameException)?.code ?: (error as? DivingFishException)?.code
                    if (code == "unauthorized") authorization = null
                    mutableState.update { it.copy(connected = authorization != null, error = errorMessage(code)) }
                }
            } finally {
                if (current == generation) mutableState.update { it.copy(busy = false) }
            }
        }
    }
}

private fun errorMessage(code: String?) = when (code) {
    "unauthorized" -> tr("Otogame 登录已失效，请重新登录。")
    "invalid_response" -> tr("Otogame 返回的数据格式不正确，请稍后重试。")
    "catalog_empty" -> tr("请先下载静态歌曲数据。")
    "profile_changed" -> tr("当前档案已更改，请重新导入。")
    "profile_ineligible" -> tr("需要启用一个日服档案。")
    else -> tr("Otogame 导入失败，请检查网络后重试。")
}
