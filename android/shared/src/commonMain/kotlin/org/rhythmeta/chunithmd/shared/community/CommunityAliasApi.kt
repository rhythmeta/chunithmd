package org.rhythmeta.chunithmd.shared.community

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonElement
import org.rhythmeta.chunithmd.shared.account.RhythmetaClient

data class CommunityAccount(val id: String?, val isAdmin: Boolean = false)

interface CommunityAliasApi {
    val account: CommunityAccount
    val accountChanges: Flow<CommunityAccount>
    suspend fun request(path: String, method: String = "GET", body: JsonElement? = null, authenticated: Boolean = true): JsonElement
}

class RhythmetaCommunityApi(private val client: RhythmetaClient) : CommunityAliasApi {
    override val account get() = client.session.value.let { CommunityAccount(it?.user?.id, it?.user?.isAdmin == true) }
    override val accountChanges get() = client.session.map { CommunityAccount(it?.user?.id, it?.user?.isAdmin == true) }
    override suspend fun request(path: String, method: String, body: JsonElement?, authenticated: Boolean) =
        client.request(path, method, body, authenticated)
}
