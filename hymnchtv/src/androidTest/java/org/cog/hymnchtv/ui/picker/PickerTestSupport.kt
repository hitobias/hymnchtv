package org.cog.hymnchtv.ui.picker

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.hymnhistory.HistoryRecord
import org.cog.hymnchtv.persistance.DatabaseBackend
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.ui.home.HomePrefs

/** Shared set-up of the picker and navigation tests: the real [MainActivity] on a clean remembered source and history. */
object PickerTestSupport {
    val ctx: Context get() = ApplicationProvider.getApplicationContext()

    private val digitIds = listOf(
        org.cog.hymnchtv.R.id.n0, org.cog.hymnchtv.R.id.n1, org.cog.hymnchtv.R.id.n2, org.cog.hymnchtv.R.id.n3, org.cog.hymnchtv.R.id.n4,
        org.cog.hymnchtv.R.id.n5, org.cog.hymnchtv.R.id.n6, org.cog.hymnchtv.R.id.n7, org.cog.hymnchtv.R.id.n8, org.cog.hymnchtv.R.id.n9,
    )

    fun prepare() {
        FragmentHost.grantLaunchPermissions(ctx.packageName)
        ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE).edit().remove(HomePrefs.LAST_HYMN_TYPE).commit()
    }

    fun cleanUp() {
        ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE).edit().remove(HomePrefs.LAST_HYMN_TYPE).commit()
    }

    fun launch(block: (ActivityScenario<MainActivity>) -> Unit) = ActivityScenario.launch(MainActivity::class.java).use(block)

    /** Clicks the keys of [digits] in order; the home tab scrolls on small screens. */
    fun type(digits: String) {
        digits.forEach { onView(withId(digitIds[it - '0'])).perform(scrollTo(), click()) }
    }

    /** Replaces the whole history (call from the instrumentation thread, never from onActivity). */
    fun resetHistory(vararg records: HistoryRecord) {
        val db = DatabaseBackend.getInstance(ctx)
        db.historyRecords.forEach { db.deleteHymnHistory(it) }
        records.forEach { db.storeHymnHistory(it) }
    }
}
