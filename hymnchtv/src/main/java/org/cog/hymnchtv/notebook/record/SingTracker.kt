package org.cog.hymnchtv.notebook.record

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.SingSource
import org.cog.hymnchtv.notebook.repo.SingLogRepository
import org.cog.hymnchtv.notebook.settings.NotebookPrefs
import timber.log.Timber
import java.util.TimeZone

/**
 * Turns lyrics-page visibility and media completion into AUTO sing logs.
 *
 * Threading: the on* methods are called from the main thread (any thread is safe), return immediately and
 * never do I/O. [scope] is NotebookGraph.appScope (process lifetime): a pending 2-minute timer dies with the
 * process, which is acceptable. Dedupe is atomic in [SingLogRepository.recordUnlessDuplicate]; once a write
 * starts it runs NonCancellable. Failures are logged and dropped, never thrown to the caller.
 */
class SingTracker(
    private val singLogs: SingLogRepository,
    private val prefs: NotebookPrefs,
    private val clock: Clock,
    private val scope: CoroutineScope,
    private val config: AutoRecordConfig = AutoRecordConfig(),
    private val zone: () -> TimeZone = { TimeZone.getDefault() },
) {
    private data class Visible(val key: HymnKey, val job: Job)

    private val lock = Any()

    /** Replaced (never mutated) under [lock]. */
    @Volatile
    private var visible: Visible? = null

    private val recordedFlow = MutableSharedFlow<SingLogEntity>(extraBufferCapacity = 16)

    /**
     * Each new AUTO log, emitted on [scope]'s threads. Java: NotebookAsync.observeAutoRecorded (main thread).
     * Kotlin UI: lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { recorded.collect { … } } }.
     */
    val recorded: SharedFlow<SingLogEntity> = recordedFlow.asSharedFlow()

    /** The hymn's lyrics became visible; starts the continuous-visibility timer unless it already runs. */
    fun onHymnVisible(key: HymnKey) {
        synchronized(lock) {
            val current = visible
            if (current != null && current.key == key && current.job.isActive) return
            current?.job?.cancel()
            val job = scope.launch {
                delay(config.visibleThresholdMillis)
                recordIfAllowed(key)
            }
            visible = Visible(key, job)
        }
    }

    /** The hymn's lyrics are no longer visible (page changed, activity paused); its timer stops. */
    fun onHymnHidden(key: HymnKey) {
        synchronized(lock) {
            val current = visible ?: return
            if (current.key != key) return
            current.job.cancel()
            visible = null
        }
    }

    /** Media playback of the hymn reached its end. */
    fun onMediaCompleted(key: HymnKey) {
        scope.launch { recordIfAllowed(key) }
    }

    /** Returns the new log, or null when disabled, duplicate or failed. */
    internal suspend fun recordIfAllowed(key: HymnKey): SingLogEntity? = withContext(NonCancellable) {
        try {
            if (!prefs.autoRecordEnabled) return@withContext null
            val now = clock.nowMillis()
            val occasion = OccasionInference.infer(now, zone(), prefs.lastChosenOccasion)
            singLogs.recordUnlessDuplicate(key, now, occasion, SingSource.AUTO, config.dedupeWindowMillis)
                ?.also { recordedFlow.tryEmit(it) }
        } catch (e: Exception) {
            Timber.e(e, "Auto record failed for %s", key)
            null
        }
    }
}
