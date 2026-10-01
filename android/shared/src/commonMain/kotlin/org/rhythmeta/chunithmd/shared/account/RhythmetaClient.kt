package org.rhythmeta.chunithmd.shared.account

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.*
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import okio.Buffer
import okio.ByteString.Companion.toByteString
import org.rhythmeta.chunithmd.shared.backup.*
import kotlin.time.Clock

class StoredSecret(val value: String?)
interface RhythmetaSecretStore {
    @Throws(Exception::class) fun read(key: String): StoredSecret
    @Throws(Exception::class) fun write(key: String, value: String?)
}
@Serializable data class RhythmetaUser(val id:String,val email:String,val username:String,val usernameDiscriminator:String,val handle:String,val isAdmin:Boolean=false)
@Serializable data class RhythmetaSession(val user:RhythmetaUser,val accessToken:String,val refreshToken:String,val expiresIn:Int=900)
@Serializable private data class PendingLogin(val state:String,val verifier:String,val createdAt:Long)
@Serializable data class CloudBackup(val id:String,val game:String,val formatVersion:Int,val size:Long,val uncompressedSize:Long,val sha256:String,val deviceName:String,val clientVersion:String,val profileCount:Int,val committedAt:String,val downloadUrl:String)
@Serializable private data class BackupList(val backups:List<CloudBackup>)
@Serializable private data class Upload(val id:String,val uploadUrl:String,val headers:Map<String,String>)
class RhythmetaApiError(val status:Int, message:String):Exception(message)

class RhythmetaClient(private val secrets:RhythmetaSecretStore, private val client:HttpClient=HttpClient { followRedirects=false }, private val base:String="https://api.rhythmeta.org") : BackupRemote {
    private val json=Json { ignoreUnknownKeys=true; encodeDefaults=true }
    private val mutableSession=MutableStateFlow(secrets.read("session").value?.let { runCatching { json.decodeFromString<RhythmetaSession>(it) }.getOrNull() })
    val session:StateFlow<RhythmetaSession?> = mutableSession
    private val refreshMutex=Mutex()
    private val callback="chunithmd://auth/callback"
    private fun apply(value:RhythmetaSession?) { secrets.write("session",value?.let { json.encodeToString(it) });mutableSession.value=value }
    fun loginUrl(mode:String="login"):String {
        require(mode in setOf("login","register","forgot"))
        fun token()=secureRandomBytes(32).toByteString().base64Url().trimEnd('=')
        val pending=PendingLogin(token(),token(),Clock.System.now().toEpochMilliseconds())
        secrets.write("pending",json.encodeToString(pending))
        val challenge=pending.verifier.encodeToByteArray().toByteString().sha256().base64Url().trimEnd('=')
        return "https://dash.rhythmeta.org/?"+mapOf("client_id" to "chunithmd","redirect_uri" to callback,"authMode" to mode,"code_challenge_method" to "S256","code_challenge" to challenge,"state" to pending.state).entries.joinToString("&") { "${it.key}=${it.value.encodeURLParameter()}" }
    }
    suspend fun handleCallback(value:String) {
        val url=Url(value)
        require(url.protocol.name=="chunithmd"&&url.host=="auth"&&url.encodedPath=="/callback"&&url.fragment.isEmpty())
        val pending=json.decodeFromString<PendingLogin>(secrets.read("pending").value?:error("Login request expired."))
        val age=Clock.System.now().toEpochMilliseconds()-pending.createdAt
        require(age in 0..1_800_000&&url.parameters.getAll("state")==listOf(pending.state)&&url.parameters["result"]=="success") { "Login request does not match." }
        secrets.write("pending",null)
        val payload=request("auth/v1/session:exchange","POST",buildJsonObject {
            put("sessionCode",url.parameters["sessionCode"]?:error("Missing login code."));put("clientId","chunithmd");put("redirectUri",callback);put("codeVerifier",pending.verifier)
        },authenticated=false)
        apply(json.decodeFromJsonElement(payload))
    }
    suspend fun checkSession() {
        if(session.value==null)return
        try { val user=json.decodeFromJsonElement<RhythmetaUser>(request("auth/v1/me"));session.value?.let { apply(it.copy(user=user)) } }
        catch(error:RhythmetaApiError) { if(error.status==401)apply(null);throw error }
    }
    suspend fun logout() {
        val token=session.value?.refreshToken
        try { if(token!=null)request("auth/v1/logout","POST",buildJsonObject { put("refreshToken",token) },false) }
        finally { apply(null);secrets.write("pending",null) }
    }
    suspend fun request(path:String,method:String="GET",body:JsonElement?=null,authenticated:Boolean=true):JsonElement {
        val current=session.value
        if(authenticated&&current==null)throw RhythmetaApiError(401,"Sign in with your Rhythmeta account.")
        try { return send(path,method,body,if(authenticated)current?.accessToken else null) }
        catch(error:RhythmetaApiError) {
            if(!authenticated||error.status!=401||current==null)throw error
            refreshMutex.withLock {
                if(session.value?.refreshToken==current.refreshToken) {
                    try { apply(json.decodeFromJsonElement(send("auth/v1/refresh","POST",buildJsonObject { put("refreshToken",current.refreshToken) },null))) }
                    catch(refreshError:RhythmetaApiError) { if(refreshError.status in 400..499)apply(null);throw refreshError }
                }
            }
            return send(path,method,body,session.value?.accessToken?:throw error)
        }
    }
    private suspend fun send(path:String,verb:String,body:JsonElement?,token:String?):JsonElement {
        val response=client.request("${base.trimEnd('/')}/${path.trimStart('/')}") {
            method=HttpMethod.parse(verb);header("X-Rhythmeta-Client","app")
            if(token!=null)bearerAuth(token)
            if(body!=null){contentType(ContentType.Application.Json);setBody(body.toString())}
        }
        val text=response.bodyAsText()
        val payload=runCatching { json.parseToJsonElement(text) }.getOrElse { JsonObject(emptyMap()) }
        if(!response.status.isSuccess())throw RhythmetaApiError(response.status.value,(payload as? JsonObject)?.get("message")?.jsonPrimitive?.content?:"Request failed (${response.status.value}).")
        return payload
    }
    suspend fun listBackups():List<CloudBackup> = json.decodeFromJsonElement<BackupList>(request("chunithmd/v1/backups")).backups
    override suspend fun backup(snapshot:BackupSnapshot,deviceName:String) {
        val raw=withContext(Dispatchers.Default) { BackupCodec.raw(snapshot) }
        val bytes=withContext(Dispatchers.Default) { gzipBackup(raw,true).also { require(it.size<=BackupCodec.MAX_COMPRESSED) } }
        val upload=json.decodeFromJsonElement<Upload>(request("chunithmd/v1/backups","POST",buildJsonObject {
            put("formatVersion",1);put("size",bytes.size);put("uncompressedSize",raw.size);put("sha256",BackupCodec.sha256(bytes));put("deviceName",deviceName.take(128));put("clientVersion",snapshot.clientVersion);put("profileCount",snapshot.profiles.size)
        }))
        require(Url(upload.uploadUrl).protocol==URLProtocol.HTTPS)
        val response=client.put(upload.uploadUrl) { upload.headers.forEach { (key,value)->header(key,value) };setBody(bytes) }
        if(!response.status.isSuccess())throw RhythmetaApiError(response.status.value,"Backup upload failed.")
        request("chunithmd/v1/backups/${upload.id}/commit","POST")
    }
    override suspend fun download(backup:CloudBackup):BackupSnapshot {
        require(backup.game=="chunithmd"&&backup.formatVersion==1&&backup.size in 1..BackupCodec.MAX_COMPRESSED.toLong()&&Url(backup.downloadUrl).protocol==URLProtocol.HTTPS)
        val response=client.get(backup.downloadUrl){header(HttpHeaders.AcceptEncoding,"identity")}
        if(response.status!=HttpStatusCode.OK)throw RhythmetaApiError(response.status.value,"Backup download failed.")
        val channel=response.bodyAsChannel();val output=Buffer();val buffer=ByteArray(65536)
        while(true) {
            val count=channel.readAvailable(buffer);if(count<0)break
            if(output.size+count>backup.size){channel.cancel(null);error("Backup exceeds expected size.")}
            output.write(buffer,0,count)
        }
        val bytes=output.readByteArray()
        require(bytes.size.toLong()==backup.size&&BackupCodec.sha256(bytes)==backup.sha256){"Backup checksum does not match."}
        return withContext(Dispatchers.Default){BackupCodec.decode(bytes,backup.uncompressedSize)}
    }
}
