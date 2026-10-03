package org.rhythmeta.chunithmd.shared.account

import org.rhythmeta.chunithmd.shared.localization.tr

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.rhythmeta.chunithmd.shared.backup.*

@Serializable private data class AccountViewState(val user: RhythmetaUser?=null, val backups:List<CloudBackup> = emptyList(), val busy:Boolean=false, val ready:Boolean=false, val error:String?=null)
/** JSON/callback facade for Swift; state transitions, network contracts and restore rules stay shared. */
class RhythmetaBridge(secrets: RhythmetaSecretStore, files: SnapshotFiles, clientVersion: String) {
    private val client=RhythmetaClient(secrets)
    private val coordinator=BackupCoordinator(client,SnapshotFileStore(files,clientVersion))
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main)
    private var state=AccountViewState(user=client.session.value?.user)
    private var listener: ((String)->Unit)?=null
    private val json=Json { encodeDefaults=true }
    fun observe(callback:(String)->Unit) { listener=callback;emit() }
    private fun emit() { listener?.invoke(json.encodeToString(state)) }
    private fun run(block:suspend ()->Unit) {
        if(state.busy)return
        state=state.copy(busy=true,error=null);emit()
        scope.launch {
            try { block() }
            catch(error:Exception) { state=state.copy(error=error.message ?: tr("操作失败")) }
            finally { state=state.copy(user=client.session.value?.user,busy=false);emit() }
        }
    }
    fun start()=run { coordinator.recover(); state=state.copy(ready=true);if(client.session.value!=null){client.checkSession();reload()} }
    fun loginUrl():String = client.loginUrl()
    fun handleCallback(url:String)=run { client.handleCallback(url);reload() }
    fun refresh()=run { reload() }
    private suspend fun reload() { state=state.copy(backups=client.listBackups()) }
    fun backup(deviceName:String)=run { check(state.ready);coordinator.backup(deviceName);reload() }
    fun restore(id:String)=run { check(state.ready);coordinator.restore(state.backups.first { it.id==id }) }
    fun logout()=run { try { client.logout() } finally { state=state.copy(backups=emptyList()) } }
    fun close() { listener=null;scope.cancel() }
}
