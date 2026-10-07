package org.rhythmeta.chunithmd.scanner

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import org.rhythmeta.chunithmd.BuildConfig
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.rhythmeta.chunithmd.profile.ProfileRepository
import org.rhythmeta.chunithmd.score.ScoreRepository
import org.rhythmeta.chunithmd.shared.*
import org.rhythmeta.chunithmd.shared.localization.tr
import org.rhythmeta.chunithmd.shared.scanner.*

data class ScannerUiState(
    val image: Uri? = null, val busy: Boolean = false, val saving: Boolean = false,
    val fields: ScanFields? = null, val match: ScanChartCandidate? = null,
    val clear: String = "", val combo: String = "",
    val error: String? = null, val saved: Boolean = false,
    val reviewVisible: Boolean = false,
    val observations: List<ScanObservation> = emptyList(),
    val imageWidth: Int = 0, val imageHeight: Int = 0, val previewRotationDegrees: Int = 0,
)

class ScannerViewModel(application: Application) : AndroidViewModel(application) {
    val models = ScannerModelManager(java.io.File(application.noBackupFilesDir, "scanner-models").path, "android")
    private val recognizer = AndroidScoreRecognizer(application, models)
    init { models.check() }
    override fun onCleared() { models.close(); super.onCleared() }
    private val mutable = MutableStateFlow(ScannerUiState())
    val state = mutable.asStateFlow()
    private var catalog: CatalogBundle? = null
    private var profile: UserProfile? = null
    private var job: Job? = null
    private var liveJob: Job? = null
    private val inferenceMutex = Mutex()
    private val stabilizer = LiveScanStabilizer()
    private var active = false
    private val frameInFlight = AtomicBoolean(false)
    val isProcessingFrame: Boolean get() = frameInFlight.get()

    fun setActive(enabled: Boolean) {
        active = enabled
        if (!enabled) {
            liveJob?.cancel()
            stabilizer.reset()
        }
    }

    /** Called on the main thread; frames are dropped while one inference is in flight. */
    fun analyzeLiveFrame(bitmap: Bitmap, previewRotationDegrees: Int) {
        val bundle = catalog
        val region = profile?.server?.wireValue
        val state = mutable.value
        if (models.snapshot == null || !active || bundle == null || region == null || state.image != null || state.busy ||
            state.saving || state.reviewVisible || !frameInFlight.compareAndSet(false, true)) {
            bitmap.recycle()
            return
        }
        liveJob = viewModelScope.launch {
            val started = SystemClock.elapsedRealtime()
            try {
                val observations = inferenceMutex.withLock { recognizer.recognize(bitmap) }
                ensureActive()
                if (!active || mutable.value.reviewVisible || mutable.value.image != null) return@launch
                val fields = ScoreScanner.fields(observations)
                mutable.value = mutable.value.copy(observations = observations,
                    imageWidth = bitmap.width, imageHeight = bitmap.height, previewRotationDegrees = previewRotationDegrees, error = null)
                val review = withContext(Dispatchers.Default) { ScoreScanner.review(bundle, fields, region) }
                ensureActive()
                if (!active || mutable.value.reviewVisible || mutable.value.image != null) return@launch
                if (BuildConfig.DEBUG) Log.d("ScannerLive", "frame=${bitmap.width}x${bitmap.height} fields=${observations.size} matched=${ScoreScanner.automaticMatch(review)?.key} durationMs=${SystemClock.elapsedRealtime() - started}")
                if (stabilizer.accept(review)) {
                    mutable.value = mutable.value.copy(fields = review.fields, match = ScoreScanner.automaticMatch(review),
                        clear = review.clear, combo = "", saved = false)
                } else if (stabilizer.shouldClear) {
                    mutable.value = mutable.value.copy(fields = null, match = null)
                }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                ensureActive()
                stabilizer.reset()
                mutable.value = mutable.value.copy(fields = null, match = null,
                    error = error.localizedMessage ?: tr("识别失败"))
            }
        }.also { task -> task.invokeOnCompletion { bitmap.recycle(); frameInFlight.set(false) } }
    }

    fun openReview() {
        if (mutable.value.match == null || mutable.value.saving) return
        liveJob?.cancel()
        mutable.value = mutable.value.copy(reviewVisible = true, saved = false, error = null)
    }
    fun dismissReview() {
        if (!mutable.value.saving) { stabilizer.reset(); mutable.value = mutable.value.copy(reviewVisible = false) }
    }
    fun resumeLive() {
        if (mutable.value.saving || models.snapshot == null) return
        job?.cancel(); liveJob?.cancel(); stabilizer.reset(); mutable.value = ScannerUiState()
    }

    fun bind(catalog: CatalogBundle?, profile: UserProfile?) {
        if (this.profile?.id != profile?.id || this.profile?.server != profile?.server) {
            job?.cancel(); liveJob?.cancel(); stabilizer.reset(); mutable.value = ScannerUiState()
        }
        this.catalog = catalog; this.profile = profile
    }
    fun recognize(uri: Uri) {
        val bundle = catalog ?: return
        val region = profile?.server?.wireValue ?: return
        if (mutable.value.saving || models.snapshot == null) return
        job?.cancel(); liveJob?.cancel(); stabilizer.reset(); mutable.value = ScannerUiState(image = uri, busy = true)
        job = viewModelScope.launch {
            try {
                val observations = inferenceMutex.withLock { recognizer.recognize(uri) }
                if (observations.isEmpty()) error(tr("未找到成绩字段，请选择清晰的单张成绩图。"))
                val review = withContext(Dispatchers.Default) { ScoreScanner.review(bundle, ScoreScanner.fields(observations), region) }
                ensureActive()
                // Gray inactive combo labels can also be read by OCR. Leave the status unselected.
                val match = ScoreScanner.automaticMatch(review)
                mutable.value = ScannerUiState(image = uri, fields = review.fields, match = match, clear = review.clear,
                    error = if (match == null) tr("未识别到匹配谱面，请重新扫描。") else null)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { ensureActive(); mutable.value = mutable.value.copy(busy = false, error = e.localizedMessage ?: tr("识别失败")) }
        }
    }
    fun markInputChanged() { mutable.value = mutable.value.copy(saved = false, error = null) }
    fun save(score: Int, clear: ClearType, combo: FullComboType?, chain: FullChainType?,
        profiles: ProfileRepository, scores: ScoreRepository) {
        val bundle = catalog ?: return; val active = profile ?: return; val s = mutable.value
        if (s.saved || s.busy || s.saving) return
        val match = s.match ?: return
        val command = ScoreScanner.prepareSave(bundle, active.server.wireValue, match.key, score.toString(), clear.wireValue, combo?.wireValue.orEmpty())
        if (command == null) { mutable.value = s.copy(error = tr("请确认谱面并输入有效分数。")); return }
        mutable.value = s.copy(saving = true, error = null)
        job = viewModelScope.launch {
            try {
                profiles.withActiveProfile(active.id) {
                    require(profiles.activeProfile.first()?.server == active.server) { tr("玩家档案已变更，请重新识别。") }
                    scores.save(command.songId, command.sheetKey, command.score, ClearType.fromWire(command.clear), FullComboType.fromWire(command.combo), chain)
                }
                mutable.value = mutable.value.copy(saving = false, saved = true, reviewVisible = false,
                    fields = s.fields?.copy(score = command.score.toString()), clear = command.clear, combo = command.combo)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { ensureActive(); mutable.value = mutable.value.copy(saving = false, error = e.localizedMessage) }
        }
    }
}
