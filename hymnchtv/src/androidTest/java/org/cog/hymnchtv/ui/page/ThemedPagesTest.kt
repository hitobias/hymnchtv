package org.cog.hymnchtv.ui.page

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.View
import android.widget.ExpandableListView
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.recyclerview.widget.RecyclerView
import androidx.fragment.app.Fragment
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.reading.ReadingSettingsActivity
import org.cog.hymnchtv.reading.background.BackgroundChoice
import org.cog.hymnchtv.reading.background.BackgroundPolicy
import org.cog.hymnchtv.reading.background.BackgroundPreset
import org.cog.hymnchtv.reading.background.BackgroundSlot
import org.cog.hymnchtv.reading.background.PhotoBackgroundImporter
import org.cog.hymnchtv.reading.background.UiTokens
import org.cog.hymnchtv.reading.background.Wcag
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.ui.settings.SettingsFragment
import org.cog.hymnchtv.ui.toc.TocFragment
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Spec 5a/7: the settings, reading-settings and contents pages follow the home background: page colour = the derived colour on
 * the light, dark, bean-green (reading green) and photo backgrounds; a changed background shows on return; every text is 4.5:1.
 * The fragments are started directly (scenario), not through any navigation.
 */
@RunWith(AndroidJUnit4::class)
class ThemedPagesTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val prefs = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    private class Look(val name: String, val stored: String)

    private val looks = listOf(
        Look("light", BackgroundPreset.PAPER_WHITE.id),
        Look("dark", BackgroundPreset.NIGHTREAD.id),
        Look("bean-green", BackgroundPreset.EYE_GREEN.id),
        Look("photo", BackgroundPolicy.PHOTO),
    )

    @Before
    fun setUp() {
        FragmentHost.grantLaunchPermissions(ctx.packageName)
        clean()
        val bytes = InstrumentationRegistry.getInstrumentation().context.assets.open("test_photo_bg.jpg").use { it.readBytes() }
        val file = File(ctx.cacheDir, "test_photo_bg.jpg").apply { writeBytes(bytes) }
        check(PhotoBackgroundImporter.import(ctx, Uri.fromFile(file))) { "test photo import failed" }
    }

    @After
    fun tearDown() {
        clean()
        PhotoBackgroundImporter.photoFileIn(ctx.filesDir).delete()
    }

    private fun clean() {
        prefs.edit().remove(BackgroundSlot.MAIN.prefKey).commit()
    }

    private fun choose(look: Look) {
        prefs.edit().putString(BackgroundSlot.MAIN.prefKey, look.stored).commit()
    }

    /** The page and card colours the spec's rules give for [look], worked out here from the tokens (not by PagePalette). */
    private fun expected(look: Look): Pair<Int, Int> {
        val choice = BackgroundPolicy.resolve(look.stored, BackgroundSlot.MAIN, false, true)
        val input = BackgroundPolicy.tokenInput(choice)
        val t = UiTokens.from(input)
        val base = (0xFF shl 24) or (input.baseColor and 0xFFFFFF)
        return when {
            choice is BackgroundChoice.Photo -> UiTokens.over(base, t.surface).let { it to UiTokens.over(it, t.surfaceTone) }
            input.isDark -> base to UiTokens.over(base, t.surface)
            else -> UiTokens.over(base, t.surfaceTone) to UiTokens.over(base, t.surface)
        }
    }

    private fun backgroundOf(v: View): Int = when (val b = v.background) {
        is ColorDrawable -> b.color
        is android.graphics.drawable.GradientDrawable -> b.color!!.defaultColor
        else -> error("unexpected background $b")
    }

    private fun <F : Fragment> inMain(make: () -> F, block: (ActivityScenario<MainActivity>, F) -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var fragment: F? = null
            scenario.onActivity { fragment = FragmentHost.show(it, make()) }
            block(scenario, checkNotNull(fragment))
        }
    }

    private fun resume(scenario: ActivityScenario<*>) {
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.moveToState(Lifecycle.State.RESUMED)
    }

    private fun <F : Fragment> pageColour(scenario: ActivityScenario<*>, fragment: F): Int {
        var colour = 0
        scenario.onActivity { colour = backgroundOf(fragment.requireView()) }
        return colour
    }

    // ---- page and card colours ----

    @Test
    fun settingsPageColoursAreTheDerivedColoursOnEveryBackground() {
        for (look in looks) {
            choose(look)
            inMain(::SettingsFragment) { scenario, fragment ->
                val (page, card) = expected(look)
                FragmentHost.eventually { assertWithMessage(look.name).that(pageColour(scenario, fragment)).isEqualTo(page) }
                scenario.onActivity {
                    assertThat(fragment.palette!!.card).isEqualTo(card)
                    assertThat(backgroundOf(fragment.titleBar!!)).isEqualTo(page)
                }
            }
        }
    }

    @Test
    fun contentsPageColoursAreTheDerivedColoursOnEveryBackground() {
        for (look in looks) {
            choose(look)
            inMain(::TocFragment) { scenario, fragment ->
                val (page, card) = expected(look)
                FragmentHost.eventually { assertWithMessage(look.name).that(pageColour(scenario, fragment)).isEqualTo(page) }
                scenario.onActivity {
                    assertThat(fragment.palette!!.card).isEqualTo(card)
                    assertThat(backgroundOf(it.findViewById<View>(R.id.toc_title_bar))).isEqualTo(page)
                    assertThat(backgroundOf(it.findViewById<View>(R.id.toc_list_frame))).isEqualTo(card)
                }
            }
        }
    }

    @Test
    fun readingSettingsPageColoursAreTheDerivedColoursOnEveryBackground() {
        for (look in looks) {
            choose(look)
            ActivityScenario.launch(ReadingSettingsActivity::class.java).use { scenario ->
                val (page, _) = expected(look)
                var fragment: Fragment? = null
                FragmentHost.eventually {
                    scenario.onActivity { fragment = it.supportFragmentManager.findFragmentById(R.id.readingSettingsContainer) }
                    assertThat(fragment).isNotNull()
                    assertWithMessage(look.name).that(pageColour(scenario, fragment!!)).isEqualTo(page)
                }
            }
        }
    }

    // ---- a changed reading colour shows when the page comes back ----

    @Test
    fun settingsPageUpdatesAtOnceWhenTheBackgroundChangesAndThePageResumes() {
        choose(looks[0])
        inMain(::SettingsFragment) { scenario, fragment ->
            FragmentHost.eventually { assertThat(pageColour(scenario, fragment)).isEqualTo(expected(looks[0]).first) }
            // Changed while the page is away (as the background picker does), then the page returns
            scenario.moveToState(Lifecycle.State.STARTED)
            choose(looks[2])
            scenario.moveToState(Lifecycle.State.RESUMED)
            FragmentHost.eventually { assertThat(pageColour(scenario, fragment)).isEqualTo(expected(looks[2]).first) }
            scenario.onActivity { assertThat(backgroundOf(fragment.titleBar!!)).isEqualTo(expected(looks[2]).first) }
        }
    }

    @Test
    fun settingsPageUpdatesWhileVisibleThroughTheListener() {
        choose(looks[0])
        inMain(::SettingsFragment) { scenario, fragment ->
            FragmentHost.eventually { assertThat(pageColour(scenario, fragment)).isEqualTo(expected(looks[0]).first) }
            choose(looks[1])
            FragmentHost.eventually { assertThat(pageColour(scenario, fragment)).isEqualTo(expected(looks[1]).first) }
        }
    }

    @Test
    fun contentsPageUpdatesWhenTheBackgroundChangesAndThePageResumes() {
        choose(looks[1])
        inMain(::TocFragment) { scenario, fragment ->
            FragmentHost.eventually { assertThat(pageColour(scenario, fragment)).isEqualTo(expected(looks[1]).first) }
            scenario.moveToState(Lifecycle.State.STARTED)
            choose(looks[2])
            resume(scenario)
            FragmentHost.eventually { assertThat(pageColour(scenario, fragment)).isEqualTo(expected(looks[2]).first) }
        }
    }

    @Test
    fun readingSettingsPageUpdatesWhenTheBackgroundChangesAndThePageResumes() {
        choose(looks[0])
        ActivityScenario.launch(ReadingSettingsActivity::class.java).use { scenario ->
            fun page(): Int {
                var colour = 0
                scenario.onActivity {
                    colour = backgroundOf(it.supportFragmentManager.findFragmentById(R.id.readingSettingsContainer)!!.requireView())
                }
                return colour
            }
            FragmentHost.eventually { assertThat(page()).isEqualTo(expected(looks[0]).first) }
            scenario.moveToState(Lifecycle.State.STARTED)
            choose(looks[1])
            scenario.moveToState(Lifecycle.State.RESUMED)
            FragmentHost.eventually { assertThat(page()).isEqualTo(expected(looks[1]).first) }
        }
    }

    // ---- contrast of what is really on screen ----

    private fun assertContrast(label: String, fg: Int, bg: Int, min: Double) {
        assertWithMessage("$label ${"#%08x".format(fg)} on ${"#%08x".format(bg)}").that(Wcag.contrast(fg, bg)).isAtLeast(min)
    }

    /** Every text of the preference rows that scroll past: title, summary and value on the card, headings on the page. */
    private fun checkPreferenceTexts(scenario: ActivityScenario<*>, label: String, fragment: PagePreferenceFragment) {
        var count = 0
        scenario.onActivity { activity ->
            val pal = checkNotNull(fragment.palette)
            val list = fragment.listView
            val adapter = checkNotNull(list.adapter)
            assertContrast("$label title bar", titleText(fragment).currentTextColor, pal.page, UiTokens.MIN_TEXT_CONTRAST)
            for (position in 0 until adapter.itemCount) {
                list.scrollToPosition(position)
                list.measure(View.MeasureSpec.makeMeasureSpec(list.width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(list.height, View.MeasureSpec.EXACTLY))
                list.layout(list.left, list.top, list.right, list.bottom)
                val child = list.findViewHolderForAdapterPosition(position)?.itemView ?: continue
                val pref = (adapter as androidx.preference.PreferenceGroupAdapter).getItem(position)
                val isCategory = pref is androidx.preference.PreferenceCategory
                val back = if (isCategory) pal.page else pal.card
                for (id in listOf(android.R.id.title, android.R.id.summary, R.id.seekbar_value)) {
                    val tv = child.findViewById<TextView>(id) ?: continue
                    if (tv.visibility != View.VISIBLE || tv.text.isNullOrEmpty()) continue
                    val min = if (tv.isEnabled) UiTokens.MIN_TEXT_CONTRAST else UiTokens.MIN_DISABLED_CONTRAST
                    assertContrast("$label ${pref?.key ?: pref?.title} ${tv.text}", tv.currentTextColor, back, min)
                    count++
                }
            }
        }
        assertThat(count).isGreaterThan(8)
    }

    private fun titleText(fragment: PagePreferenceFragment): TextView =
        fragment.titleBar!!.findViewById(R.id.page_title)

    @Test
    fun everySettingsTextIsAtLeastFourPointFiveToOneOnEveryBackground() {
        for (look in looks) {
            choose(look)
            inMain(::SettingsFragment) { scenario, fragment ->
                FragmentHost.eventually { assertThat(fragment.palette).isNotNull() }
                checkPreferenceTexts(scenario, "settings/${look.name}", fragment)
            }
        }
    }

    @Test
    fun everyReadingSettingsTextIsAtLeastFourPointFiveToOneOnEveryBackground() {
        for (look in looks) {
            choose(look)
            ActivityScenario.launch(ReadingSettingsActivity::class.java).use { scenario ->
                var fragment: PagePreferenceFragment? = null
                FragmentHost.eventually {
                    scenario.onActivity { fragment = it.supportFragmentManager.findFragmentById(R.id.readingSettingsContainer) as? PagePreferenceFragment }
                    assertThat(fragment?.palette).isNotNull()
                }
                checkPreferenceTexts(scenario, "reading/${look.name}", fragment!!)
            }
        }
    }

    @Test
    fun everyContentsTextIsAtLeastFourPointFiveToOneOnEveryBackground() {
        for (look in looks) {
            choose(look)
            inMain(::TocFragment) { scenario, fragment ->
                FragmentHost.eventually {
                    scenario.onActivity { assertThat(it.findViewById<ExpandableListView>(R.id.hymnToc).expandableListAdapter?.groupCount ?: 0).isGreaterThan(0) }
                }
                scenario.onActivity { activity ->
                    val pal = checkNotNull(fragment.palette)
                    val list = activity.findViewById<ExpandableListView>(R.id.hymnToc)
                    list.expandGroup(0)
                    list.measure(View.MeasureSpec.makeMeasureSpec(list.width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(list.height, View.MeasureSpec.EXACTLY))
                    list.layout(list.left, list.top, list.right, list.bottom)
                    assertThat(list.childCount).isGreaterThan(2)
                    for (i in 0 until list.childCount) {
                        val tv = list.getChildAt(i).let { it.findViewById<TextView>(R.id.hymnCategory) ?: it as? TextView } ?: continue
                        assertContrast("toc/${look.name} row ${tv.text}", tv.currentTextColor, pal.card, UiTokens.MIN_TEXT_CONTRAST)
                    }
                    val bar = activity.findViewById<View>(R.id.toc_title_bar)
                    assertContrast("toc/${look.name} title", bar.findViewById<TextView>(R.id.page_title).currentTextColor, pal.page, UiTokens.MIN_TEXT_CONTRAST)
                    val books = activity.findViewById<com.google.android.material.chip.ChipGroup>(R.id.toc_books)
                    for (i in 0 until books.childCount) {
                        val chip = books.getChildAt(i) as Chip
                        val back = chip.chipBackgroundColor!!.getColorForState(chip.drawableState, 0)
                        assertContrast("toc/${look.name} chip ${chip.text}", chip.currentTextColor, back, UiTokens.MIN_TEXT_CONTRAST)
                    }
                    val tabs = activity.findViewById<TabLayout>(R.id.toc_pages)
                    val colors = checkNotNull(tabs.tabTextColors)
                    assertContrast("toc/${look.name} tab", colors.getColorForState(intArrayOf(), 0), pal.page, UiTokens.MIN_TEXT_CONTRAST)
                    assertContrast("toc/${look.name} tab selected", colors.getColorForState(intArrayOf(android.R.attr.state_selected), 0), pal.page, UiTokens.MIN_TEXT_CONTRAST)
                }
            }
        }
    }

    // ---- structure ----

    @Test
    fun titleBarHasTheBackArrowAndTheTitleAndDividerFollowsTheScroll() {
        choose(looks[0])
        inMain(::SettingsFragment) { scenario, fragment ->
            FragmentHost.eventually { assertThat(fragment.palette).isNotNull() }
            scenario.onActivity {
                val bar = fragment.titleBar!!
                assertThat(bar.findViewById<View>(R.id.page_back).contentDescription.toString()).isEqualTo(ctx.getString(R.string.page_back))
                assertThat(titleText(fragment).text.toString()).isEqualTo(ctx.getString(R.string.page_title_settings))
                assertThat(bar.findViewById<View>(R.id.page_divider).visibility).isEqualTo(View.INVISIBLE)
                val list: RecyclerView = fragment.listView
                list.scrollBy(0, 100000)
            }
            FragmentHost.eventually {
                scenario.onActivity { assertThat(fragment.titleBar!!.findViewById<View>(R.id.page_divider).visibility).isEqualTo(View.VISIBLE) }
            }
        }
    }

    @Test
    fun everySettingsPreferenceKeepsItsKeyAndTitle() {
        inMain(::SettingsFragment) { scenario, fragment ->
            scenario.onActivity {
                for (key in listOf("Theme", "Locale", "reading_settings", "media_config", "check_update", "permission_request", "online_help", "about")) {
                    assertThat(fragment.findPreference<Preference>(key)).isNotNull()
                }
            }
        }
    }
}
