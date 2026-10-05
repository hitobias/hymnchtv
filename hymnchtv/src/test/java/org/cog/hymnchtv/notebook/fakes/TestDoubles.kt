package org.cog.hymnchtv.notebook.fakes

import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.IdGenerator
import org.cog.hymnchtv.notebook.model.Occasion
import org.cog.hymnchtv.notebook.settings.NotebookPrefs
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/** Canonical lowercase UUIDs that are easy to read in failures: testUuid(1) = 00000000-0000-0000-0000-000000000001. */
fun testUuid(n: Int): String = UUID(0L, n.toLong()).toString()

const val TEST_DEVICE = "00000000-0000-0000-0000-0000000000aa"

class MutableClock(@Volatile var now: Long) : Clock {
    override fun nowMillis(): Long = now
}

class SequentialIds(private val start: Int = 0) : IdGenerator {
    private val counter = AtomicInteger(start)
    override fun newId(): String = testUuid(counter.incrementAndGet())
}

/**
 * Backing fields are private: a public `var autoRecordEnabled` would clash with setAutoRecordEnabled() on the JVM.
 * [autoRecord] defaults to true so tests exercise recording unless they say otherwise; the production default
 * (NotebookPrefs.DEFAULT_AUTO_RECORD, off) is pinned by NotebookPrefsDefaultsTest.
 */
class FakeNotebookPrefs(
    autoRecord: Boolean = true,
    lastChosen: Occasion? = null,
    private val device: String = TEST_DEVICE,
) : NotebookPrefs {
    @Volatile
    private var autoRecord: Boolean = autoRecord

    @Volatile
    private var lastChosen: Occasion? = lastChosen

    override val autoRecordEnabled: Boolean
        get() = autoRecord

    override val lastChosenOccasion: Occasion?
        get() = lastChosen

    override fun setAutoRecordEnabled(enabled: Boolean) {
        autoRecord = enabled
    }

    override fun setLastChosenOccasion(occasion: Occasion) {
        lastChosen = occasion
    }

    override fun deviceId(): String = device
}
