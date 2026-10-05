package org.rhythmeta.chunithmd.shared.importing

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.rhythmeta.chunithmd.shared.localization.tr

enum class DivingFishImportPhase { Idle, Authorizing, Waiting, Importing }

data class DivingFishImportState(
    val profileId: String? = null,
    val connected: Boolean = false,
    val phase: DivingFishImportPhase = DivingFishImportPhase.Idle,
    val userCode: String? = null,
    val authorizationUrl: String? = null,
    val result: ScoreImportResult? = null,
    val error: String? = null,
) {
    val busy: Boolean get() = phase != DivingFishImportPhase.Idle
}

/** UI platforms supply only persistence adapters and the screen's lifetime scope. */
class DivingFishImportController(
    private val client: DivingFishClient,
    private val scope: CoroutineScope,
    private val apply: suspend (String, DivingFishPayload) -> ScoreImportResult,
) {
    private val mutableState = MutableStateFlow(DivingFishImportState())
    val state: StateFlow<DivingFishImportState> = mutableState.asStateFlow()
    private var operation: Job? = null
    private var generation = 0

    fun selectProfile(id: String?) {
        if (state.value.profileId == id) return
        cancel()
        mutableState.value = DivingFishImportState(profileId = id, connected = id?.let(client::isConnected) == true)
    }

    fun authorizeAndImport() = perform { id ->
        mutableState.update { it.copy(phase = DivingFishImportPhase.Authorizing) }
        val device = client.authorize()
        mutableState.update { it.copy(phase = DivingFishImportPhase.Waiting, userCode = device.userCode, authorizationUrl = device.url) }
        client.awaitAuthorization(device, id)
        import(id)
    }

    fun importScores() = perform { import(it) }

    fun disconnect() = perform { id -> client.disconnect(id) }

    fun cancel() {
        generation++
        operation?.cancel()
        operation = null
        mutableState.update { it.copy(phase = DivingFishImportPhase.Idle, userCode = null, authorizationUrl = null) }
    }

    private suspend fun import(id: String) {
        currentCoroutineContext().ensureActive()
        mutableState.update { it.copy(phase = DivingFishImportPhase.Importing, userCode = null, authorizationUrl = null) }
        val payload = DivingFishImportPolicy.decode(client.records(id))
        currentCoroutineContext().ensureActive()
        val result = apply(id, payload)
        currentCoroutineContext().ensureActive()
        mutableState.update { it.copy(result = result) }
    }

    private fun perform(work: suspend (String) -> Unit) {
        val id = state.value.profileId ?: return
        if (state.value.busy) return
        val current = ++generation
        mutableState.update { it.copy(phase = DivingFishImportPhase.Importing, result = null, error = null) }
        operation = scope.launch {
            try { work(id) }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                if (generation == current) mutableState.update { it.copy(error = divingFishErrorMessage(error)) }
            } finally {
                // An in-flight token exchange may finish after cancel; reflect its saved binding.
                if (state.value.profileId == id) mutableState.update { it.copy(connected = client.isConnected(id)) }
                if (generation == current) mutableState.update { it.copy(
                    phase = DivingFishImportPhase.Idle, userCode = null, authorizationUrl = null,
                    connected = client.isConnected(id),
                ) }
            }
        }
    }
}

fun divingFishErrorMessage(error: Exception): String = when ((error as? DivingFishException)?.code) {
    "expired_token" -> tr("授权码已过期，请重新授权。")
    "access_denied" -> tr("已拒绝水鱼授权。")
    "invalid_grant", "unauthorized" -> tr("水鱼授权已失效，请重新授权。")
    "invalid_client", "unauthorized_client", "invalid_scope" -> tr("水鱼应用配置有误，请检查客户端类型和成绩读取权限。")
    "forbidden" -> tr("无法读取成绩，请确认已同意水鱼用户协议并授予成绩读取权限。")
    "rate_limited", "slow_down" -> tr("水鱼请求过于频繁，请稍后重试。")
    "catalog_empty" -> tr("请先下载静态歌曲数据。")
    "profile_changed" -> tr("当前档案已更改，请重新导入。")
    "invalid_response" -> tr("水鱼返回的数据格式不正确，请稍后重试。")
    else -> tr("水鱼导入失败，请检查网络后重试。")
}
