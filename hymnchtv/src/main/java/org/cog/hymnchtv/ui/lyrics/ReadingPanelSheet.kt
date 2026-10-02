package org.cog.hymnchtv.ui.lyrics

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.R
import org.cog.hymnchtv.reading.DisplayMode
import org.cog.hymnchtv.reading.LyricsFont
import org.cog.hymnchtv.reading.LyricsFontSize
import org.cog.hymnchtv.reading.ReadingPrefs
import org.cog.hymnchtv.reading.background.BackgroundCategory
import org.cog.hymnchtv.reading.background.BackgroundChoice
import org.cog.hymnchtv.reading.background.BackgroundDrawables
import org.cog.hymnchtv.reading.background.BackgroundPrefs
import org.cog.hymnchtv.reading.background.BackgroundPreset
import org.cog.hymnchtv.reading.background.BackgroundSlot

/**
 * "Aa" quick panel on the lyrics page (visual redesign 6b). Every control writes the reading settings' own keys
 * ([ReadingPanelPrefs]) and then asks the activity to apply them to the pages at once.
 */
class ReadingPanelSheet : BottomSheetDialogFragment() {
    private val host: ContentHandler get() = requireActivity() as ContentHandler
    private val dots = mutableListOf<Pair<BackgroundPreset, DotViews>>()

    private class DotViews(val cell: FrameLayout, val disc: View, val tick: TextView)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.sheet_reading_panel, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        buildThemeRow(view)
        bindSize(view)
        bindFont(view)
        bindMode(view)
        view.findViewById<Button>(R.id.aa_open_settings).setOnClickListener {
            dismiss()
            host.openReadingSettings()
        }
    }

    override fun onStart() {
        super.onStart()
        host.setChromeHeld(true)
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        host.setChromeHeld(false)
        super.onDismiss(dialog)
    }

    private fun prefs() = host.sPreference

    private fun currentPreset(): BackgroundPreset? =
        (BackgroundPrefs.resolve(prefs(), BackgroundSlot.LYRICS) as? BackgroundChoice.Preset)?.preset

    private fun buildThemeRow(view: View) {
        val row = view.findViewById<LinearLayout>(R.id.aa_theme_row)
        val density = resources.displayMetrics.density
        fun px(dp: Int) = (dp * density).toInt()
        BackgroundPreset.entries.filter { it.category == BackgroundCategory.READING }.forEach { preset ->
            val disc = View(requireContext()).apply { layoutParams = FrameLayout.LayoutParams(px(DOT_DP), px(DOT_DP), Gravity.CENTER) }
            val tick = TextView(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(px(DOT_DP), px(DOT_DP), Gravity.CENTER)
                gravity = Gravity.CENTER
                text = TICK
                textSize = 16f
                setTextColor(preset.textColor)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            val cell = FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(px(CELL_DP), px(CELL_DP)).apply { marginEnd = px(GAP_DP) }
                addView(disc)
                addView(tick)
                setOnClickListener { chooseTheme(preset) }
            }
            row.addView(cell)
            dots += preset to DotViews(cell, disc, tick)
        }
        row.addView(Button(requireContext(), null, androidx.appcompat.R.attr.borderlessButtonStyle).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(CELL_DP))
            text = getString(R.string.c_aa_more)
            minWidth = 0
            setOnClickListener {
                dismiss()
                host.openBackgroundPicker()
            }
        })
        refreshDots()
        // Start scrolled to the chosen dot (narrow screens show about five)
        val scroll = view.findViewById<HorizontalScrollView>(R.id.aa_theme_scroll)
        val selected = dots.firstOrNull { it.first == currentPreset() }?.second?.cell
        if (selected != null) scroll.post { scroll.scrollTo(selected.left - px(CELL_DP), 0) }
    }

    private fun chooseTheme(preset: BackgroundPreset) {
        ReadingPanelPrefs.setTheme(prefs(), BackgroundChoice.Preset(preset))
        host.applyReadingTheme()
        host.onChromeInteraction()
        refreshDots()
    }

    private fun refreshDots() {
        val density = resources.displayMetrics.density
        val chosen = currentPreset()
        val ring = com.google.android.material.color.MaterialColors.getColor(
            requireView(), com.google.android.material.R.attr.colorOutline,
        )
        val accent = com.google.android.material.color.MaterialColors.getColor(
            requireView(), androidx.appcompat.R.attr.colorPrimary,
        )
        dots.forEach { (preset, views) ->
            val selected = preset == chosen
            views.disc.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(preset.baseColor)
                setStroke(((if (selected) 3 else 1) * density).toInt(), if (selected) accent else ring)
            }
            views.tick.visibility = if (selected) View.VISIBLE else View.INVISIBLE
            val name = getString(BackgroundDrawables.nameRes(preset))
            views.cell.contentDescription = if (selected) getString(R.string.c_aa_theme_selected, name) else name
            views.cell.isSelected = selected
        }
    }

    private fun bindSize(view: View) {
        val slider = view.findViewById<Slider>(R.id.aa_size_slider)
        val sizes = LyricsFontSize.entries
        val names = listOf(R.string.font_size_small, R.string.font_size_medium, R.string.font_size_large, R.string.font_size_xlarge)
        slider.value = ReadingPrefs.fontSize(prefs()).ordinal.toFloat()
        slider.setLabelFormatter { getString(names[it.toInt().coerceIn(0, names.lastIndex)]) }
        slider.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            ReadingPanelPrefs.setFontSize(prefs(), sizes[value.toInt().coerceIn(0, sizes.lastIndex)])
            host.applyReadingPrefsToPages(false)
            host.onChromeInteraction()
        }
    }

    private fun bindFont(view: View) {
        val group = view.findViewById<MaterialButtonToggleGroup>(R.id.aa_font_group)
        group.check(if (ReadingPrefs.lyricsFont(prefs()) == LyricsFont.KAI) R.id.aa_font_kai else R.id.aa_font_system)
        group.addOnButtonCheckedListener { _, id, checked ->
            if (!checked) return@addOnButtonCheckedListener
            ReadingPanelPrefs.setFont(prefs(), if (id == R.id.aa_font_kai) LyricsFont.KAI else LyricsFont.SYSTEM)
            host.applyReadingPrefsToPages(false)
        }
    }

    private fun bindMode(view: View) {
        val group = view.findViewById<MaterialButtonToggleGroup>(R.id.aa_mode_group)
        group.check(
            when (ReadingPrefs.displayMode(prefs())) {
                DisplayMode.SCORE_ONLY -> R.id.aa_mode_score
                DisplayMode.LYRICS_ONLY -> R.id.aa_mode_lyrics
                else -> R.id.aa_mode_both
            },
        )
        group.addOnButtonCheckedListener { _, id, checked ->
            if (!checked) return@addOnButtonCheckedListener
            ReadingPanelPrefs.setDisplayMode(
                prefs(),
                when (id) {
                    R.id.aa_mode_score -> DisplayMode.SCORE_ONLY
                    R.id.aa_mode_lyrics -> DisplayMode.LYRICS_ONLY
                    else -> DisplayMode.SCORE_AND_LYRICS
                },
            )
            host.applyReadingPrefsToPages(true)
        }
    }

    companion object {
        const val TAG = "reading_panel"
        private const val DOT_DP = 32
        private const val CELL_DP = 48
        private const val GAP_DP = 8
        private const val TICK = "✓"
    }
}
