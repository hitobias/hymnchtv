package org.cog.hymnchtv

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.RadioButton
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainThreadDbSupport.awaitUntil
import org.cog.hymnchtv.MainThreadDbSupport.launchMainActivityIfNeeded
import org.cog.hymnchtv.concurrent.IoGate
import org.cog.hymnchtv.mediaconfig.LyricsEnglishRecord
import org.cog.hymnchtv.mediaconfig.MediaConfig
import org.cog.hymnchtv.mediaconfig.MediaRecord
import org.cog.hymnchtv.persistance.DatabaseBackend
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * The lyrics screen (ContentHandler) and the English lyrics record reach the database on AppExecutors.io only.
 * The app database refuses main-thread queries, so a regression throws; the io gate proves nothing waits for it.
 */
@RunWith(AndroidJUnit4::class)
class ContentHandlerMainThreadDbTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    private val backend = DatabaseBackend.getInstance(ctx)
    private val link = "https://example.org/ch-mt-test-1"
    private var mainScenario: ActivityScenario<MainActivity>? = null

    private fun media(type: MediaType) = MediaRecord(MainActivity.HYMN_DB, 1, false, type)

    @Before
    fun setUp() {
        mainScenario = launchMainActivityIfNeeded()
        MediaType.values().forEach { backend.deleteMediaRecord(media(it)) }
    }

    @After
    fun tearDown() {
        MediaType.values().forEach { backend.deleteMediaRecord(media(it)) }
        mainScenario?.close()
    }

    private fun resumedMediaConfig(): Activity? {
        var found: Activity? = null
        instrumentation.runOnMainSync {
            found = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .firstOrNull { it is MediaConfig }
        }
        return found
    }

    private fun launch(): ActivityScenario<ContentHandler> {
        val extras = Bundle().apply {
            putString(MainActivity.ATTR_HYMN_TYPE, MainActivity.HYMN_DB)
            putInt(MainActivity.ATTR_HYMN_NUMBER, 1)
        }
        return ActivityScenario.launch(Intent(ctx, ContentHandler::class.java).putExtras(extras))
    }

    private fun <T> ActivityScenario<ContentHandler>.read(block: (ContentHandler) -> T): T {
        val ref = AtomicReference<T>()
        onActivity { ref.set(block(it)) }
        return ref.get()
    }

    private fun ActivityScenario<ContentHandler>.jiaoChangColor(): Int =
        read { it.findViewById<RadioButton>(R.id.btn_jiaochang).currentTextColor }

    @Test
    fun mediaButtonsShowTheStoredLinkAfterTheBackgroundRead() {
        backend.storeMediaRecord(MediaRecord(MainActivity.HYMN_DB, 1, false, MediaType.HYMN_JIAOCHANG, link, null))
        val gate = IoGate.close()
        try {
            launch().use { scenario ->
                Thread.sleep(500)
                // the state lookup is queued behind the gate: the button still has its initial look
                val before = scenario.jiaoChangColor()
                gate.release()
                assertThat(awaitUntil { scenario.jiaoChangColor() != before || before != Color.GRAY }).isTrue()
                assertThat(awaitUntil { scenario.jiaoChangColor() != Color.GRAY }).isTrue()
            }
        } finally {
            gate.release()
        }
    }

    @Test
    fun deleteEnglishLyricsRunsInTheBackground() {
        launch().use { scenario ->
            val eng = scenario.read { it.hymnNoEng }
            assertThat(eng).isNotNull()
            backend.storeLyricsEng(eng!!, "<h1>mt-test</h1>")
            val gate = IoGate.close()
            try {
                scenario.onActivity { it.onLyricsAction(R.id.lyrcsEnglishDelete) }
                Thread.sleep(500)
                assertThat(backend.getLyricsEnglish(eng)).isNotEmpty()
            } finally {
                gate.release()
            }
            assertThat(awaitUntil { backend.getLyricsEnglish(eng).isNullOrEmpty() }).isTrue()
        }
    }

    @Test
    fun mediaConfigMenuOpensThePrefilledScreenAfterTheBackgroundRead() {
        // the screen asks for the media type the player has selected (a saved preference): store all of them
        MediaType.values().forEach { backend.storeMediaRecord(MediaRecord(MainActivity.HYMN_DB, 1, false, it, link, null)) }
        val monitor = instrumentation.addMonitor(MediaConfig::class.java.name, null, false)
        try {
            launch().use { scenario ->
                val gate = IoGate.close()
                try {
                    scenario.onActivity {
                        it.onLyricsAction(R.id.media_config)
                        it.onLyricsAction(R.id.media_config) // a second tap while the lookup is pending is ignored
                    }
                    Thread.sleep(700)
                    assertThat(monitor.hits).isEqualTo(0)
                } finally {
                    gate.release()
                }
                assertThat(awaitUntil { monitor.hits > 0 }).isTrue()
                assertThat(monitor.hits).isEqualTo(1) // the second tap was ignored
                var started: Activity? = null
                assertThat(awaitUntil { started = resumedMediaConfig(); started != null }).isTrue()
                assertThat(started!!.intent.getStringExtra(MainActivity.ATTR_MEDIA_URI)).isEqualTo(link)
                instrumentation.runOnMainSync { started!!.finish() }
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    @Test
    fun aResultForADestroyedScreenIsDropped() {
        val monitor = instrumentation.addMonitor(MediaConfig::class.java.name, null, false)
        try {
            launch().use { scenario ->
                val gate = IoGate.close()
                try {
                    scenario.onActivity { it.onLyricsAction(R.id.media_config) }
                    scenario.recreate() // rotation: the screen that asked is destroyed while the lookup waits
                } finally {
                    gate.release()
                }
                // an io task queued after the lookup finishes after it; its main-thread post follows the lookup's
                val flushed = java.util.concurrent.CountDownLatch(1)
                org.cog.hymnchtv.concurrent.AppExecutors.io("flush") {
                    org.cog.hymnchtv.concurrent.AppExecutors.MAIN.post { flushed.countDown() }
                }
                assertThat(flushed.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue()
                assertThat(monitor.hits).isEqualTo(0)
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    @Test
    fun shareMenuReadsTheLinkInTheBackgroundThenOffersTheChooser() {
        backend.storeMediaRecord(MediaRecord(MainActivity.HYMN_DB, 1, false, MediaType.HYMN_MEDIA, link, null))
        launch().use { scenario ->
            // registered after the launch: a blocking monitor must not see the activity start itself
            val monitor = instrumentation.addMonitor(
                IntentFilter(Intent.ACTION_CHOOSER), Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null), true)
            try {
                val gate = IoGate.close()
                try {
                    scenario.onActivity { it.onLyricsAction(R.id.lyrcsShare) }
                    Thread.sleep(500)
                    assertThat(monitor.hits).isEqualTo(0)
                } finally {
                    gate.release()
                }
                assertThat(awaitUntil { monitor.hits > 0 }).isTrue()
            } finally {
                instrumentation.removeMonitor(monitor)
            }
        }
    }

    @Test
    fun fetchPlayHymnLooksUpTheRecordInTheBackgroundThenListsIt() {
        backend.storeMediaRecord(MediaRecord(MainActivity.HYMN_DB, 1, false, MediaType.HYMN_MEDIA, link, null))
        launch().use { scenario ->
            val delivered = AtomicReference<List<Uri>?>()
            val called = java.util.concurrent.atomic.AtomicBoolean(false)
            val gate = IoGate.close()
            try {
                scenario.onActivity {
                    it.fetchPlayHymn(MediaType.HYMN_MEDIA, false) { uris -> delivered.set(uris); called.set(true) }
                }
                Thread.sleep(500)
                assertThat(called.get()).isFalse()
            } finally {
                gate.release()
            }
            assertThat(awaitUntil { called.get() }).isTrue()
            assertThat(delivered.get()!!.map { it.toString() }).containsExactly(link)
        }
    }

    @Test
    fun englishLyricsAreReadInTheBackgroundThenShown() {
        backend.storeLyricsEng(9990, "<h1>mt-test-lyrics</h1>")
        try {
            val shown = AtomicReference<String?>()
            val record = LyricsEnglishRecord(ctx)
            record.registerLyricsListener { lyrics, _ -> shown.set(lyrics) }
            val gate = IoGate.close()
            try {
                instrumentation.runOnMainSync { record.fetchLyrics(9990, false) }
                Thread.sleep(500)
                assertThat(shown.get()).isNull()
            } finally {
                gate.release()
            }
            assertThat(awaitUntil { shown.get() != null }).isTrue()
            assertThat(shown.get()).contains("mt-test-lyrics")
        } finally {
            backend.deleteLyricsEng(9990)
        }
    }

    private fun launchEr(no: Int): ActivityScenario<ContentHandler> {
        val extras = Bundle().apply {
            putString(MainActivity.ATTR_HYMN_TYPE, MainActivity.HYMN_ER)
            putInt(MainActivity.ATTR_HYMN_NUMBER, no)
        }
        return ActivityScenario.launch(Intent(ctx, ContentHandler::class.java).putExtras(extras))
    }

    @Test
    fun deleteEnglishLyricsOfAnErGeHymnRemovesTheOffsetRowAndKeepsTheDaBenRowOfTheSameNumber() {
        launchEr(1).use { scenario ->
            val eng = scenario.read { it.hymnNoEng }
            assertThat(eng).isNotNull()
            val erKey = LyricsEnglishRecord.dbHymnNo(eng!!, true)
            assertThat(erKey).isNotEqualTo(eng)
            backend.storeLyricsEng(erKey, "<h1>er-row</h1>")
            backend.storeLyricsEng(eng, "<h1>daben-row</h1>")
            try {
                scenario.onActivity { it.onLyricsAction(R.id.lyrcsEnglishDelete) }
                assertThat(awaitUntil { backend.getLyricsEnglish(erKey).isNullOrEmpty() }).isTrue()
                assertThat(backend.getLyricsEnglish(eng)).contains("daben-row")
            } finally {
                backend.deleteLyricsEng(erKey)
                backend.deleteLyricsEng(eng)
            }
        }
    }

    @Test
    fun aShareStartedOnOneHymnStaysOnThatHymnWhenTheUserSwipesBeforeTheReadFinishes() {
        val tmp = org.cog.hymnchtv.persistance.FileBackend.getHymnchtvStore(org.cog.hymnchtv.persistance.FileBackend.TMP, true)!!
        val first = listOf(java.io.File(tmp, "db1.png"), java.io.File(tmp, "db1.txt"))
        val second = listOf(java.io.File(tmp, "db2.png"), java.io.File(tmp, "db2.txt"))
        (first + second).forEach { it.delete() }
        launch().use { scenario ->
            val monitor = instrumentation.addMonitor(
                IntentFilter(Intent.ACTION_CHOOSER), Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null), true)
            try {
                val gate = IoGate.close()
                try {
                    scenario.onActivity { it.onLyricsAction(R.id.lyrcsShare) }
                    scenario.onActivity { it.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager).setCurrentItem(1, false) }
                    assertThat(awaitUntil { scenario.read { it.hymnNo } != 1 }).isTrue()
                } finally {
                    gate.release()
                }
                assertThat(awaitUntil { monitor.hits > 0 }).isTrue()
                assertThat(first.all { it.exists() }).isTrue()
                assertThat(second.any { it.exists() }).isFalse()
            } finally {
                instrumentation.removeMonitor(monitor)
                (first + second).forEach { it.delete() }
            }
        }
    }

    @Test
    fun aFailedLinkReadStillCompletesTheShareAndLaterTapsWork() {
        launch().use { scenario ->
            val monitor = instrumentation.addMonitor(
                IntentFilter(Intent.ACTION_CHOOSER), Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null), true)
            val room = backend.roomDatabase().openHelper.writableDatabase
            try {
                room.execSQL("ALTER TABLE media_record RENAME TO media_record_hidden")
                try {
                    scenario.onActivity { it.onLyricsAction(R.id.lyrcsShare) }
                    assertThat(awaitUntil { monitor.hits > 0 }).isTrue()
                    val before = monitor.hits
                    scenario.onActivity { it.onLyricsAction(R.id.lyrcsShare) } // the pending flag was reset
                    assertThat(awaitUntil { monitor.hits > before }).isTrue()
                } finally {
                    room.execSQL("ALTER TABLE media_record_hidden RENAME TO media_record")
                }
            } finally {
                instrumentation.removeMonitor(monitor)
            }
        }
    }

    @Test
    fun aFailedMediaStateReadLeavesTheScreenUsable() {
        val room = backend.roomDatabase().openHelper.writableDatabase
        room.execSQL("ALTER TABLE media_record RENAME TO media_record_hidden")
        try {
            launch().use { scenario ->
                Thread.sleep(500)
                assertThat(scenario.read { it.hymnNo }).isEqualTo(1)
            }
        } finally {
            room.execSQL("ALTER TABLE media_record_hidden RENAME TO media_record")
        }
    }

    private fun ActivityScenario<ContentHandler>.player(): MediaGuiController =
        read { it.supportFragmentManager.findFragmentById(R.id.mediaPlayer) as MediaGuiController }

    @Test
    fun playPressedWhileTheLookupRunsIsRepeatedForTheMediaTypeChosenMeanwhile() {
        val jcLink = "https://example.org/ch-mt-test-jc.mp3"
        backend.storeMediaRecord(MediaRecord(MainActivity.HYMN_DB, 1, false, MediaType.HYMN_MEDIA, link, null))
        backend.storeMediaRecord(MediaRecord(MainActivity.HYMN_DB, 1, false, MediaType.HYMN_JIAOCHANG, jcLink, null))
        launch().use { scenario ->
            scenario.onActivity { it.findViewById<RadioButton>(R.id.btn_media).isChecked = true }
            val player = scenario.player() // kept: the screen may swap the fragment once playback starts
            val gate = IoGate.close()
            try {
                scenario.onActivity {
                    player.startPlay()
                    it.findViewById<RadioButton>(R.id.btn_jiaochang).isChecked = true // the type changes mid-lookup
                }
                Thread.sleep(300)
            } finally {
                gate.release()
            }
            assertThat(awaitUntil { player.mFetchedTypes.size >= 2 }).isTrue()
            Thread.sleep(500) // a retry that loops would keep adding lookups
            assertThat(player.mFetchedTypes).containsExactly(MediaType.HYMN_MEDIA, MediaType.HYMN_JIAOCHANG).inOrder()
        }
    }
}
