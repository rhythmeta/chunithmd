package org.rhythmeta.chunithmd.shared.account

import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.rhythmeta.chunithmd.shared.backup.SnapshotFile
import org.rhythmeta.chunithmd.shared.backup.SnapshotFiles
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RhythmetaBridgeTest {
    private class Secrets : RhythmetaSecretStore {
        val values = mutableMapOf<String, String?>()
        override fun read(key: String) = StoredSecret(values[key])
        override fun write(key: String, value: String?) { values[key] = value }
    }
    private class Files : SnapshotFiles {
        private val values = mutableMapOf<String, ByteArray>()
        override fun read(name: String) = SnapshotFile(values[name])
        override fun write(name: String, bytes: ByteArray) { values[name] = bytes }
        override fun delete(name: String) { values.remove(name) }
    }

    @Test fun authModesUseSharedPkceAndInvalidModesDoNotReplacePendingLogin() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val secrets = Secrets()
        val bridge = RhythmetaBridge(secrets, Files(), "test")
        try {
            var previousState: String? = null
            for (mode in listOf("login", "register", "forgot")) {
                val url = Url(bridge.loginUrl(mode))
                assertEquals("dash.rhythmeta.org", url.host)
                assertEquals(mode, url.parameters["authMode"])
                assertEquals("chunithmd://auth/callback", url.parameters["redirect_uri"])
                assertEquals("S256", url.parameters["code_challenge_method"])
                assertNotNull(url.parameters["code_challenge"])
                val pending = Json.parseToJsonElement(secrets.values.getValue("pending")!!).jsonObject
                assertEquals(pending.getValue("state").jsonPrimitive.content, url.parameters["state"])
                assertNotEquals(previousState, url.parameters["state"])
                previousState = url.parameters["state"]
            }
            val pending = secrets.values["pending"]
            assertFailsWith<IllegalArgumentException> { bridge.loginUrl("unsupported") }
            assertEquals(pending, secrets.values["pending"])
        } finally { bridge.close(); Dispatchers.resetMain() }
    }

    @Test fun failedRestoreNeverReportsSuccess() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val bridge = RhythmetaBridge(Secrets(), Files(), "test")
        val states = mutableListOf<String>()
        try {
            bridge.observe { states += it }
            bridge.restore("missing")
            runCurrent()
            val result = Json.parseToJsonElement(states.last()).jsonObject
            assertNotEquals(JsonNull, result["error"])
            assertEquals(JsonNull, result["message"])
            assertEquals("false", result.getValue("busy").jsonPrimitive.content)
        } finally { bridge.close(); Dispatchers.resetMain() }
    }
}
