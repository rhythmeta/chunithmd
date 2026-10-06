package org.rhythmeta.chunithmd.shared.importing

import org.rhythmeta.chunithmd.shared.localization.tr
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.rhythmeta.chunithmd.shared.*
import org.rhythmeta.chunithmd.shared.account.RhythmetaSecretStore
import org.rhythmeta.chunithmd.shared.backup.SnapshotFiles

/** Owns provider controllers for the lifetime of an iOS import screen. */
class NativeImportBridge(secrets: RhythmetaSecretStore, files: SnapshotFiles, private val bundle: CatalogBundle) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val personal = PersonalDataBridge(files)
    private val fish = DivingFishImportController(DivingFishClient(secrets), scope) { id, payload -> personal.importDivingFish(bundle, id, payload) }
    private val lxns = LxnsImportController(LxnsClient(secrets), scope) { id, payload -> personal.importLxns(bundle, id, payload) }
    private val otogame = OtogameImportController(OtogameClient(), scope) { id, payload -> personal.importOtogame(bundle, id, payload) }
    private var listener: ((String) -> Unit)? = null
    fun observe(callback: (String) -> Unit) {
        listener = callback
        scope.launch { fish.state.collect { emit() } }
        scope.launch { lxns.state.collect { emit() } }
        scope.launch { otogame.state.collect { emit() } }
    }
    fun selectProfile(id: String, server: String) {
        fish.selectProfile(id); lxns.selectProfile(id); otogame.selectProfile(id, ProfileServer.fromWire(server))
    }
    fun authorize(provider: String) { when(provider) { "fish" -> fish.authorizeAndImport(); "lxns" -> lxns.beginAuthorization() } }
    fun exchange(code: String) { lxns.setCode(code); lxns.exchangeAndImport() }
    fun captureOtogame(profileId: String, header: String) { otogame.captureAuthorization(profileId, header) }
    fun importScores(provider: String) { when(provider) { "fish" -> fish.importScores(); "lxns" -> lxns.importScores(); "otogame" -> otogame.importScores() } }
    fun disconnect(provider: String) { when(provider) { "fish" -> fish.disconnect(); "lxns" -> lxns.disconnect(); "otogame" -> otogame.disconnect() } }
    fun cancel() { fish.cancel(); lxns.cancel(); otogame.cancel() }
    fun close() { listener = null; scope.cancel() }
    private fun emit() {
        listener?.invoke(buildJsonObject {
            val f = fish.state.value; val l = lxns.state.value; val o = otogame.state.value
            put("fish", state(f.connected, f.busy, f.authorizationUrl, f.userCode, f.error, f.result, phase = f.phase.name))
            put("lxns", state(l.connected, l.busy, l.authorizationUrl, null, l.error, l.result))
            put("otogame", state(o.connected, o.busy, null, null, o.error, o.result, eligible = o.eligible, page = o.page, totalPages = o.totalPages))
        }.toString())
    }
    private fun state(
        connected: Boolean, busy: Boolean, url: String?, code: String?, error: String?, result: ScoreImportResult?,
        phase: String? = null, eligible: Boolean = true, page: Int = 0, totalPages: Int = 0,
    ) = buildJsonObject {
        put("connected", connected); put("busy", busy); put("url", url); put("code", code); put("error", error)
        put("phase", phase); put("eligible", eligible); put("page", page); put("totalPages", totalPages)
        put("result", result?.let { tr("读取 {0} 条，新增 {1} 条，跳过 {2} 条", it.fetched, it.updated, it.skipped) })
    }
}
