package org.rhythmeta.chunithmd.shared.community

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonElement
import org.rhythmeta.chunithmd.shared.account.RhythmetaClient

interface CommunityAliasApi {
    val accountId: String?
    val accountChanges: Flow<String?>
    suspend fun request(path: String, method: String = "GET", body: JsonElement? = null, authenticated: Boolean = true): JsonElement
}

class RhythmetaCommunityApi(private val client: RhythmetaClient) : CommunityAliasApi {
    override val accountId get() = client.session.value?.user?.id
    override val accountChanges get() = client.session.map { it?.user?.id }
    override suspend fun request(path: String, method: String, body: JsonElement?, authenticated: Boolean) =
        client.request(path, method, body, authenticated)
}
