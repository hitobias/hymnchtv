package org.cog.hymnchtv.ui.page

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.PreferenceViewHolder
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.picker.KaiText

/**
 * Paints the preference rows from the page's [PagePalette] (spec 5a): rows of one category share a card whose top and bottom
 * rows carry the 16dp corners, separated by an inset 1dp divider; category headings use the category colour in HymnalKai.
 * [palette] is read on every bind, so a notifyDataSetChanged after a background change repaints everything.
 */
class PagePreferenceAdapter(group: PreferenceGroup, private val palette: () -> PagePalette) : PreferenceGroupAdapter(group) {
    override fun onBindViewHolder(holder: PreferenceViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val pref = getItem(position) ?: return
        val pal = palette()
        if (pref is PreferenceCategory) {
            bindCategory(holder, pal)
            return
        }
        bindRow(holder, pal, isFirstInCard(position), isLastInCard(position), pref.isEnabled)
    }

    /** Whether the row at [position] is the first of its card: the previous visible item is a category heading or nothing. */
    fun isFirstInCard(position: Int): Boolean = position == 0 || getItem(position - 1) is PreferenceCategory

    fun isLastInCard(position: Int): Boolean = position == itemCount - 1 || getItem(position + 1) is PreferenceCategory

    fun isCategory(position: Int): Boolean = getItem(position) is PreferenceCategory

    private fun bindCategory(holder: PreferenceViewHolder, pal: PagePalette) {
        val title = holder.findViewById(android.R.id.title) as TextView
        title.setTextColor(pal.category)
        KaiText.applyForUi(title, title.context, bold = false)
        holder.itemView.background = null
    }

    private fun bindRow(holder: PreferenceViewHolder, pal: PagePalette, first: Boolean, last: Boolean, enabled: Boolean) {
        val view = holder.itemView
        val density = view.resources.displayMetrics.density
        view.background = cardBackground(pal, first, last, view.resources.getDimension(R.dimen.shape_radius_medium))
        (holder.findViewById(android.R.id.title) as? TextView)?.apply {
            setTextColor(if (enabled) pal.onCard else pal.tokens.disabledOnSurface)
            KaiText.applyForUi(this, context, bold = false)
        }
        (holder.findViewById(android.R.id.summary) as? TextView)?.setTextColor(if (enabled) pal.muted else pal.tokens.disabledOnSurface)
        (holder.findViewById(android.R.id.icon) as? ImageView)?.imageTintList = ColorStateList.valueOf(pal.muted)
        holder.findViewById(R.id.hymnal_divider)?.apply {
            setBackgroundColor(pal.divider)
            visibility = if (last) View.GONE else View.VISIBLE
            layoutParams = layoutParams.apply { height = density.toInt().coerceAtLeast(1) }
        }
        (holder.findViewById(R.id.seekbar_value) as? TextView)?.setTextColor(pal.onCard)
        (holder.findViewById(androidx.preference.R.id.switchWidget) as? SwitchCompat)?.let { tintSwitch(it, pal) }
        (holder.findViewById(R.id.seekbar) as? SeekBar)?.let { tintSeekBar(it, pal) }
    }

    private fun tintSwitch(widget: SwitchCompat, pal: PagePalette) {
        val checked = intArrayOf(android.R.attr.state_checked)
        widget.thumbTintList = ColorStateList(arrayOf(checked, intArrayOf()), intArrayOf(pal.onAccent, pal.muted))
        widget.trackTintList = ColorStateList(arrayOf(checked, intArrayOf()), intArrayOf(pal.accent, pal.switchOff))
    }

    private fun tintSeekBar(bar: SeekBar, pal: PagePalette) {
        bar.progressTintList = ColorStateList.valueOf(pal.accent)
        bar.thumbTintList = ColorStateList.valueOf(pal.accent)
        bar.progressBackgroundTintList = ColorStateList.valueOf(pal.switchOff)
    }

    private fun cardBackground(pal: PagePalette, first: Boolean, last: Boolean, radius: Float): RippleDrawable {
        val top = if (first) radius else 0f
        val bottom = if (last) radius else 0f
        val radii = floatArrayOf(top, top, top, top, bottom, bottom, bottom, bottom)
        fun shape(color: Int) = GradientDrawable().apply {
            setColor(color)
            cornerRadii = radii
        }
        val ripple = ColorStateList.valueOf((pal.onCard and 0xFFFFFF) or (RIPPLE_ALPHA shl 24))
        return RippleDrawable(ripple, shape(pal.card), shape(pal.card))
    }

    private companion object {
        const val RIPPLE_ALPHA = 0x1F
    }
}
