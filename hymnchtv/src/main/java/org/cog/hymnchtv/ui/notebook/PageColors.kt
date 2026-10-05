package org.cog.hymnchtv.ui.notebook

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import com.google.android.material.button.MaterialButton
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.ui.page.PagePalette
import org.cog.hymnchtv.ui.page.PageTitleBar
import org.cog.hymnchtv.ui.theme.SystemBars

/** Colours of a notebook page: the same palette as the settings pages (from the home background), re-read on every resume. */
object PageColors {
    fun paint(activity: Activity, root: View, bar: PageTitleBar): PagePalette {
        val palette = PagePalette.fromPrefs(activity.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE))
        root.setBackgroundColor(palette.page)
        bar.apply(palette)
        SystemBars.styleIcons(activity, palette.isDark, SystemBars.legacyNavColor(palette.isDark, palette.page, palette.onCard))
        return palette
    }

    /** The page's main action: accent fill (at least 3:1 on the card), text and icon in the on-accent colour. */
    fun accentButton(button: MaterialButton, palette: PagePalette) {
        button.backgroundTintList = ColorStateList.valueOf(palette.accent)
        button.setTextColor(palette.onAccent)
        button.iconTint = ColorStateList.valueOf(palette.onAccent)
    }
}
