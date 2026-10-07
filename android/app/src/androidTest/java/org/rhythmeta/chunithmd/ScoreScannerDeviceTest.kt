package org.rhythmeta.chunithmd

import android.net.Uri
import android.graphics.ImageDecoder
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import org.rhythmeta.chunithmd.shared.scanner.ScannerModelManager
import org.rhythmeta.chunithmd.shared.scanner.ScannerModelRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.rhythmeta.chunithmd.shared.scanner.AndroidScoreRecognizer
import org.rhythmeta.chunithmd.shared.scanner.ScoreDetection
import org.rhythmeta.chunithmd.shared.scanner.ScoreScanner
import org.rhythmeta.chunithmd.shared.scanner.LiveScanStabilizer
import org.rhythmeta.chunithmd.shared.CatalogBundle
import org.rhythmeta.chunithmd.shared.Catalog
import org.rhythmeta.chunithmd.shared.CatalogSong
import org.rhythmeta.chunithmd.shared.CatalogSheet
import kotlin.math.max
import kotlin.math.roundToInt

/** Runs the packaged native runtime and OCR on hardware; JVM tests cannot catch ARM SIGILL. */
@RunWith(AndroidJUnit4::class)
class ScoreScannerDeviceTest {
    @Test
    fun scansJapaneseChineseAndWorldsEndRepeatedly() = runBlocking {
        // Keep the process foreground: device background freezing otherwise distorts inference timing.
        val activity = ActivityScenario.launch(MainActivity::class.java)
        try {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val context = instrumentation.targetContext
            val modelDirectory = File(context.noBackupFilesDir, "scanner-models").path
            var models = ScannerModelManager(modelDirectory, "android")
            models.check()
            withTimeout(120_000) { models.state.first { it.stage != "checking" } }
            if (!models.state.value.usable) {
                assertEquals("required", models.state.value.stage)
                // Let check's coroutine finish before starting the next operation.
                kotlinx.coroutines.delay(50)
                models.download()
                withTimeout(180_000) { models.state.first { it.stage == "ready" || it.stage == "failed" } }
            }
            assertTrue("Remote models must be available: ${models.state.value}", models.state.value.usable)
            models.close()
            // Refused loopback endpoint simulates an unreachable model host without changing phone networking.
            models = ScannerModelManager(ScannerModelRepository(modelDirectory, "android", baseUrl = "https://127.0.0.1:9"))
            models.check()
            withTimeout(30_000) { models.state.first { it.stage != "checking" } }
            assertTrue("Cached models must work offline", models.state.value.usable && models.state.value.offline)
            val recognizer = AndroidScoreRecognizer(context, models)
            val cases = listOf(
                Triple("jp.jpg", "蜘蛛の糸", 1_002_011),
                Triple("we.jpg", "ナイト・オブ・ナイツ", 969_524),
                Triple("cn.jpg", "宿星審判", 1_007_080),
            )
            try { repeat(2) { pass ->
                for ((name, expectedTitle, expectedScore) in cases) {
                    val file = File.createTempFile("scanner-regression-", ".jpg", context.cacheDir)
                    try {
                        instrumentation.context.assets.open("scanner/$name").use { input ->
                            file.outputStream().use(input::copyTo)
                        }
                        val started = System.nanoTime()
                        val observations = if (pass == 0) recognizer.recognize(Uri.fromFile(file)) else {
                            // Exercise the live-camera bitmap entry with a 1080p-class recorded frame.
                            val frame = ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
                                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                                val scale = 1920.0 / max(info.size.width, info.size.height)
                                decoder.setTargetSize((info.size.width * scale).roundToInt(), (info.size.height * scale).roundToInt())
                            }
                            try { recognizer.recognize(frame) } finally { frame.recycle() }
                        }
                        assertEquals("$name detection fields", ScoreDetection.fields.toSet(), observations.map { it.field }.toSet())
                        val fields = ScoreScanner.fields(observations)
                        Log.i("ScannerRegression", "pass=$pass fixture=$name recognized=$fields")
                        assertEquals("$name score OCR: ${fields.score}", expectedScore, ScoreScanner.parseScore(fields.score))
                        assertEquals("$name CJK title OCR", expectedTitle, fields.title)
                        assertEquals(if (name == "we.jpg") "we" else "master", ScoreScanner.difficulty(fields.difficulty))
                        if (name == "we.jpg") assertEquals("時", ScoreScanner.attribute(fields.level))
                        val catalog = CatalogBundle(1, Catalog(songs = listOf(CatalogSong("fixture", expectedTitle,
                            sheets = listOf(CatalogSheet(if (name == "we.jpg") "we" else "std",
                                if (name == "we.jpg") "時" else "master", fields.level, regions = mapOf("jp" to true)))))))
                        assertTrue("First valid frame must display a card", LiveScanStabilizer().accept(ScoreScanner.review(catalog, fields, "jp")))
                        for (observation in observations) {
                            val box = observation.box
                            assertTrue(box.x.isFinite() && box.y.isFinite() && box.width > 0f && box.height > 0f)
                            assertTrue(box.x >= 0f && box.y >= 0f && box.x + box.width <= 1.0001f && box.y + box.height <= 1.0001f)
                        }
                        Log.i("ScannerRegression", "pass=$pass fixture=$name score=$expectedScore durationMs=${(System.nanoTime() - started) / 1_000_000}")
                    } finally {
                        file.delete()
                    }
                }
            }
            } finally { models.close() }
        } finally { activity.close() }
    }
}
