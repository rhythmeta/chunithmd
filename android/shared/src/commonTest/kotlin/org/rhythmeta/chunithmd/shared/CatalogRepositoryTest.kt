package org.rhythmeta.chunithmd.shared

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.encodeUtf8
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val BundleJson =
    """{"schemaVersion":1,"catalog":{"songs":[{"songId":"a","title":"A","imageName":"a.png"}]}}"""
private val JacketBytes = "jacket-a".encodeUtf8()
private val BundleSize = BundleJson.encodeUtf8().size.toLong()
private val TotalSize = BundleSize + JacketBytes.size

private fun manifestJson(includeSizes: Boolean = true): String {
    val sizes = if (includeSizes) {
        ""","sizes":{"bundleBytes":$BundleSize,"imageBytes":${JacketBytes.size},"totalBytes":$TotalSize}"""
    } else {
        ""
    }
    return """{"schemaVersion":1,"product":"chunithmd","version":"bundle-test","sha256":"${BundleJson.encodeUtf8().sha256().hex()}","bundle":"/bundles/test.json","createdAt":"2026-01-01T00:00:00Z","assets":{"jacketBaseUrl":"https://assets.test/jackets/"}""" +
        sizes + "}"
}

class CatalogRepositoryTest {
    private class Fixture(val manifest: String = manifestJson()) {
        val root = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "catalog-repository-test-${Random.nextLong()}"
        val requests = mutableListOf<String>()

        fun repository() = CatalogRepository(
            cacheDirectory = root.toString(),
            fileSystem = FileSystem.SYSTEM,
            client = HttpClient(MockEngine { request ->
                val path = request.url.encodedPath
                requests += path
                when {
                    path.endsWith("manifest.json") -> respond(
                        manifest,
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                    path.startsWith("/bundles/") -> respond(
                        BundleJson,
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                    path.startsWith("/jackets/") -> respond(
                        JacketBytes.toByteArray(),
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "image/png"),
                    )
                    else -> respond("missing", HttpStatusCode.NotFound)
                }
            }),
            manifestUrl = "https://assets.test/manifest.json",
        )

        fun clean() = FileSystem.SYSTEM.deleteRecursively(root, mustExist = false)
    }

    @Test fun catalogDownloadDoesNotWaitForJackets() = runTest {
        val f = Fixture()
        try {
            val repository = f.repository()
            val states = mutableListOf<CatalogSyncState>()
            val snapshot = repository.downloadAndApply { states += it }

            assertEquals("bundle-test", snapshot.manifest.version)
            // The catalog is on disk and usable while every jacket is still missing.
            assertNotNull(repository.loadLocal())
            assertNull(repository.localJacketPath("a.png"))
            assertTrue(repository.isImageSyncPending())
            assertTrue(f.requests.none { it.startsWith("/jackets/") })
            assertEquals(CatalogSyncStage.Ready, states.last().stage)
            // The bundle stage reports the manifest's bundle size instead of guessing from responses.
            assertTrue(states.any { it.totalBytes == BundleSize })
        } finally {
            f.clean()
        }
    }

    @Test fun jacketPassFillsTheCacheAndClearsThePendingMarker() = runTest {
        val f = Fixture()
        try {
            val repository = f.repository()
            repository.downloadAndApply()

            val states = mutableListOf<CatalogSyncState>()
            assertTrue(repository.downloadMissingJackets { states += it })

            assertNotNull(repository.localJacketPath("a.png"))
            assertFalse(repository.isImageSyncPending())
            assertTrue(f.requests.any { it == "/jackets/a.png" })
            // Downloaded bytes keep climbing across the two passes and never exceed the manifest total.
            assertEquals(TotalSize, states.last().totalBytes)
            assertEquals(TotalSize, states.last().downloadedBytes)
        } finally {
            f.clean()
        }
    }

    @Test fun secondRunSkipsJacketsThatAreAlreadyCached() = runTest {
        val f = Fixture()
        try {
            val repository = f.repository()
            repository.downloadAndApply()
            repository.downloadMissingJackets()
            f.requests.clear()

            repository.downloadMissingJackets()

            assertTrue(f.requests.none { it.startsWith("/jackets/") })
        } finally {
            f.clean()
        }
    }

    @Test fun manifestWithoutSizesStillDownloadsWithoutATotal() = runTest {
        val f = Fixture(manifestJson(includeSizes = false))
        try {
            val repository = f.repository()
            val states = mutableListOf<CatalogSyncState>()
            repository.downloadAndApply { states += it }

            assertTrue(states.none { it.totalBytes != null })
            assertTrue(repository.isImageSyncPending())
        } finally {
            f.clean()
        }
    }
}
