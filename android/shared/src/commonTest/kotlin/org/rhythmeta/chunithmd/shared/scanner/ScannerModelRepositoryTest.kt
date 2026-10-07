package org.rhythmeta.chunithmd.shared.scanner

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.ByteString.Companion.encodeUtf8
import okio.Path.Companion.toPath
import kotlin.random.Random
import kotlin.test.*

class ScannerModelRepositoryTest {
    private class Fixture {
        val root = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "scanner-model-test-${Random.nextLong()}"
        var network = true
        var corrupt: String? = null
        var cancelAt: String? = null
        val requests = mutableListOf<String>()
        val bodies = mutableMapOf<String, String>()
        var manifest = manifest("v1")
        fun manifest(version: String): ScannerModelManifest {
            val entries = listOf("ScoreDetector.onnx", "PaddleOCRv6Small.onnx", "PaddleOCRv6SmallVocab.json").map { name ->
                val bytes = "$name-$version".encodeUtf8()
                bodies[bytes.sha256().hex()] = bytes.utf8()
                ScannerModelEntry(name, bytes.sha256().hex(), bytes.size.toLong())
            }
            return ScannerModelManifest(1, entries)
        }
        fun repository(): ScannerModelRepository = ScannerModelRepository(root.toString(), "android", HttpClient(MockEngine { request ->
            check(network) { "Offline" }
            val path = request.url.encodedPath
            requests += path
            if (path.endsWith("android.json")) respond(scannerModelJson.encodeToString(manifest))
            else {
                val digest = path.substringAfterLast('/')
                if (digest == cancelAt) throw CancellationException("Cancelled transfer")
                val body = bodies[digest] ?: "missing"
                respond(if (digest == corrupt) "x".repeat(body.length) else body, HttpStatusCode.OK)
            }
        }), "https://models.test")
        fun clean() = FileSystem.SYSTEM.deleteRecursively(root, mustExist = false)
    }

    @Test fun downloadVerifiesAndReopensOffline() = runTest {
        val f = Fixture()
        val repo = f.repository()
        try {
            assertNull(repo.loadCached())
            val manifest = repo.fetchManifest()
            val progress = mutableListOf<Long>()
            val snapshot = repo.download(manifest) { bytes, _ -> progress += bytes }
            assertEquals(manifest.entries.sumOf { it.size }, progress.last())
            assertEquals(snapshot, repo.loadCached())
            f.network = false
            val reopened = f.repository()
            try { assertEquals(snapshot, reopened.loadCached()) } finally { reopened.close() }
        } finally { repo.close(); f.clean() }
    }

    @Test fun failedAndCancelledUpdatesPreserveOldRevisionAndReuseVerifiedFiles() = runTest {
        val f = Fixture(); val repo = f.repository()
        try {
            val previous = repo.download(repo.fetchManifest()) { _, _ -> }
            f.manifest = f.manifest("v2")
            val next = repo.fetchManifest()
            // Sorted order: OCR, vocabulary, detector. The completed OCR is reusable.
            f.corrupt = next.entries[1].sha256
            assertFailsWith<IllegalStateException> { repo.download(next) { _, _ -> } }
            assertEquals(previous, repo.loadCached())
            f.corrupt = null
            f.cancelAt = next.entries.last().sha256
            assertFailsWith<CancellationException> { repo.download(next) { _, _ -> } }
            assertEquals(previous, repo.loadCached())
            f.cancelAt = null
            val ocrRequests = f.requests.count { it.endsWith(next.entries.first().sha256) }
            val current = repo.download(next) { _, _ -> }
            assertNotEquals(previous.revision, current.revision)
            assertEquals(current, repo.loadCached())
            assertEquals(ocrRequests, f.requests.count { it.endsWith(next.entries.first().sha256) })
            assertFalse(FileSystem.SYSTEM.list(f.root / "objects").any { it.name.endsWith(".download") })
        } finally { repo.close(); f.clean() }
    }

    @Test fun tamperedCacheCannotEnableScannerAndCanBeRepaired() = runTest {
        val f = Fixture(); val repo = f.repository()
        try {
            val manifest = repo.fetchManifest()
            val saved = repo.download(manifest) { _, _ -> }
            val file = saved.file("ScoreDetector.onnx").toPath()
            FileSystem.SYSTEM.write(file) { writeUtf8("damaged") }
            assertNull(repo.loadCached())
            repo.download(manifest) { _, _ -> }
            assertEquals(saved, repo.loadCached())
        } finally { repo.close(); f.clean() }
    }

    @Test fun invalidManifestsNeverBecomePathsOrDownloads() = runTest {
        val f = Fixture(); val repo = f.repository()
        try {
            val entry = f.manifest.entries.first()
            for (invalid in listOf(
                f.manifest.copy(schemaVersion = 2),
                f.manifest.copy(entries = f.manifest.entries.dropLast(1)),
                f.manifest.copy(entries = listOf(entry, entry, entry)),
                f.manifest.copy(entries = f.manifest.entries.map { it.copy(filename = "../${it.filename}") }),
                f.manifest.copy(entries = f.manifest.entries.map { it.copy(size = 0) }),
                f.manifest.copy(entries = f.manifest.entries.map { it.copy(sha256 = "../../invalid") }),
            )) {
                f.manifest = invalid
                assertFailsWith<IllegalArgumentException> { repo.fetchManifest() }
            }
            assertTrue(f.requests.all { it.endsWith("android.json") })
        } finally { repo.close(); f.clean() }
    }

    @Test fun offlineManagerEnablesOnlyVerifiedCachedModels() = runTest {
        val f = Fixture(); val repo = f.repository()
        try {
            repo.download(repo.fetchManifest()) { _, _ -> }
            f.network = false
            val manager = ScannerModelManager(repo)
            try {
                manager.check()
                val state = withContext(Dispatchers.Default) { withTimeout(5000) { manager.state.first { it.stage != "checking" } } }
                assertEquals("ready", state.stage)
                assertTrue(state.usable && state.offline)
                assertNotNull(manager.snapshot)
            } finally { manager.close() }
        } finally { repo.close(); f.clean() }
    }

    @Test fun offlineEmptyCacheIsRetryable() = runTest {
        val f = Fixture(); f.network = false
        val manager = ScannerModelManager(f.repository())
        try {
            manager.check()
            val state = withContext(Dispatchers.Default) { withTimeout(5000) { manager.state.first { it.stage != "checking" } } }
            assertEquals("failed", state.stage)
            assertFalse(state.usable)
            assertNull(manager.snapshot)
        } finally { manager.close(); f.clean() }
    }
}
