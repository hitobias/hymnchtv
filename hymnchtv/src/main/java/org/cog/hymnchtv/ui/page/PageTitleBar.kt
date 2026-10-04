package org.cog.hymnchtv.ui.page

import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.picker.KaiText

/**
 * The title bar of a full page (spec 5a): a back arrow and the page name on the page colour, with a 1dp divider that appears
 * once the content below has scrolled. Reusable inside any fragment (settings, contents, ...): the host only has to put it
 * at the top of the page, give it [onBack] if the default is not wanted, and call [apply] with the page's [PagePalette].
 *
 * It pads itself at the top by the status bar and display cutout when the window hands it the insets (a host that already
 * consumed or applied them gets no extra padding, since a consumed inset never reaches this view). Side and bottom insets are
 * the page's business: see [PageInsets].
 */
class PageTitleBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val back: ImageButton
    private val title: TextView
    private val divider: View
    private var scrolled = false

    /** Called when the back arrow is tapped; defaults to the activity's back dispatch (pops the page like the system back key). */
    var onBack: () -> Unit = { context.findActivity()?.onBackPressedDispatcher?.onBackPressed() }

    init {
        orientation = VERTICAL
        View.inflate(context, R.layout.page_title_bar, this)
        back = findViewById(R.id.page_back)
        title = findViewById(R.id.page_title)
        divider = findViewById(R.id.page_divider)
        back.setOnClickListener { onBack() }
        val a = context.obtainStyledAttributes(attrs, R.styleable.PageTitleBar)
        try {
            a.getString(R.styleable.PageTitleBar_pageTitle)?.let { setTitle(it) }
        } finally {
            a.recycle()
        }
        ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(0, bars.top, 0, 0)
            insets
        }
    }

    fun setTitle(text: CharSequence) {
        title.text = text
        KaiText.applyForUi(title, context, bold = false)
    }

    /** Page colour behind, text and arrow in the card text colour, divider in the page palette's divider colour. */
    fun apply(palette: PagePalette) {
        setBackgroundColor(palette.page)
        title.setTextColor(palette.onCard)
        back.imageTintList = ColorStateList.valueOf(palette.onCard)
        divider.setBackgroundColor(palette.divider)
        showDivider(scrolled)
    }

    /** Shows the divider while [scrolled]. */
    fun showDivider(scrolled: Boolean) {
        this.scrolled = scrolled
        divider.visibility = if (scrolled) VISIBLE else INVISIBLE
    }

    /** Follows [list]: the divider shows exactly while the list can still scroll up (content is hidden behind the bar). */
    fun trackScroll(list: RecyclerView) {
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                showDivider(recyclerView.canScrollVertically(-1))
            }
        })
    }

    private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
