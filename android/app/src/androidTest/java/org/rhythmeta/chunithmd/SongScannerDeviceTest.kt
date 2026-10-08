package org.rhythmeta.chunithmd

import android.net.Uri
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.rhythmeta.chunithmd.shared.*
import org.rhythmeta.chunithmd.shared.scanner.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SongScannerDeviceTest {
    @Test fun downloadsAndScansTitleThenSwitchesBackToScore() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val manager = ScannerModelManager(File(context.noBackupFilesDir, "scanner-models").path, "android")
        try {
            manager.prepare()
            withTimeout(120_000) { manager.state.first { it.stage in listOf("required", "ready", "update", "failed") } }
            if (manager.state.value.stage in listOf("required", "update")) {
                delay(100); manager.download()
                withTimeout(180_000) { manager.state.first { it.stage in listOf("ready", "failed") } }
            }
            assertTrue("Downloaded model set: ${manager.state.value}", manager.state.value.usable)
            val recognizer = AndroidScoreRecognizer(context, manager)
            for ((name, title) in listOf("song-ascii.jpg" to "Candyland Symphony", "song-jp.jpg" to "最愛テトラグラマトン", "cn.jpg" to "宿星審判")) {
                val songMode = name != "cn.jpg"
                val file = File.createTempFile("song-regression-", ".jpg", context.cacheDir)
                try {
                    instrumentation.context.assets.open("scanner/$name").use { input -> file.outputStream().use(input::copyTo) }
                    val start = System.nanoTime()
                    val observations = recognizer.recognize(Uri.fromFile(file), songMode)
                    val fields = ScoreScanner.fields(observations)
                    Log.i("ScannerRegression", "$name title=${fields.title} durationMs=${(System.nanoTime()-start)/1_000_000}")
                    assertEquals(title, fields.title)
                    if (songMode) {
                        assertEquals(listOf("title"), observations.map { it.field })
                        val catalog = CatalogBundle(1, Catalog(songs = listOf(CatalogSong("song", title, sheets = listOf(
                            CatalogSheet("std", "master", regions = mapOf("jp" to true)))))))
                        assertEquals("song", SongScanner.match(catalog, fields.title, "jp")?.songId)
                        val stabilizer = LiveSongStabilizer(); val match = SongScanner.match(catalog, fields.title, "jp")
                        assertFalse(stabilizer.accept(match)); assertTrue(stabilizer.accept(match))
                    } else assertEquals(1_007_080, ScoreScanner.parseScore(fields.score))
                } finally { file.delete() }
            }
        } finally { manager.close() }
    }
}
