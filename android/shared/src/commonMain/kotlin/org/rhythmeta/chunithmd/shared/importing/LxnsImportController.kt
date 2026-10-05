package org.rhythmeta.chunithmd.shared.importing

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.rhythmeta.chunithmd.shared.localization.tr

data class LxnsImportState(
    val profileId: String? = null,
    val connected: Boolean = false,
    val busy: Boolean = false,
    val authorizationUrl: String? = null,
    val code: String = "",
    val result: ScoreImportResult? = null,
    val error: String? = null,
)

class LxnsImportController(
    private val client: LxnsClient,
    private val scope: CoroutineScope,
    private val apply: suspend (String, LxnsPayload) -> ScoreImportResult,
) {
    private val mutableState = MutableStateFlow(LxnsImportState())
    val state = mutableState.asStateFlow()
    private var operation: Job? = null
    private var generation = 0

    fun selectProfile(id: String?) {
        if (id == state.value.profileId) return
        cancel()
        mutableState.value = LxnsImportState(profileId = id, connected = id?.let(client::isConnected) == true,
            authorizationUrl = id?.let(client::authorizationUrl))
    }
    fun setCode(value: String) { if (!state.value.busy) mutableState.update { it.copy(code = value.take(4096)) } }
    fun beginAuthorization() = perform { id ->
        val url = client.beginAuthorization(id)
        mutableState.update { it.copy(authorizationUrl = url, code = "") }
    }
    fun exchangeAndImport() {
        val code = state.value.code
        perform { id ->
            client.exchange(id, code)
            mutableState.update { it.copy(code = "", authorizationUrl = null) }
            import(id)
        }
    }
    fun importScores() = perform { import(it) }
    fun disconnect() = perform { id ->
        client.disconnect(id)
        mutableState.update { it.copy(code = "", authorizationUrl = null) }
    }
    fun cancel() {
        generation++
        operation?.cancel()
        operation = null
        mutableState.update { it.copy(busy = false, code = "") }
    }
    private suspend fun import(id: String) {
        currentCoroutineContext().ensureActive()
        val response = client.records(id)
        val songs = client.songs()
        val payload = LxnsImportPolicy.decode(response, songs)
        currentCoroutineContext().ensureActive()
        val result = apply(id, payload)
        currentCoroutineContext().ensureActive()
        mutableState.update { it.copy(result = result) }
    }
    private fun perform(work: suspend (String) -> Unit) {
        val id = state.value.profileId ?: return
        if (state.value.busy) return
        val current = ++generation
        mutableState.update { it.copy(busy = true, error = null, result = null) }
        operation = scope.launch {
            try { work(id) }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                if (current == generation) mutableState.update { it.copy(error = lxnsErrorMessage(error)) }
            } finally {
                if (state.value.profileId == id) mutableState.update { it.copy(connected = client.isConnected(id), authorizationUrl = client.authorizationUrl(id)) }
                if (current == generation) mutableState.update { it.copy(busy = false) }
            }
        }
    }
}

private fun lxnsErrorMessage(error: Exception): String = when ((error as? LxnsException)?.code ?: (error as? DivingFishException)?.code) {
    "invalid_code" -> tr("请输入有效的授权码。")
    "missing_pkce", "invalid_grant", "unauthorized" -> tr("落雪授权已失效，请重新授权。")
    "invalid_client", "invalid_scope" -> tr("落雪应用配置有误，请检查应用信息和读取权限。")
    "forbidden" -> tr("请在落雪授权页面授予玩家数据读取权限。")
    "player_missing" -> tr("落雪账号尚未绑定中二节奏玩家。")
    "rate_limited" -> tr("落雪请求过于频繁，请稍后重试。")
    "catalog_empty" -> tr("请先下载静态歌曲数据。")
    "profile_changed" -> tr("当前档案已更改，请重新导入。")
    "invalid_response" -> tr("落雪返回的数据格式不正确，请稍后重试。")
    else -> tr("落雪导入失败，请检查网络后重试。")
}
