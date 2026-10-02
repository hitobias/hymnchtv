package org.cog.hymnchtv.ui.picker

import android.content.Context
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.hymn.HymnSource
import org.cog.hymnchtv.notebook.model.HymnTypes

/** User-visible text for a [HymnRef]; the appendix (附) numbering of the main and youth books comes from [HymnRef.displayNo]. */
object HymnLabels {
    private fun typeName(ctx: Context, book: String): String = ctx.getString(
        when (book) {
            HymnTypes.DB -> R.string.hymn_type_name_db
            HymnTypes.BB -> R.string.hymn_type_name_bb
            HymnTypes.XB -> R.string.hymn_type_name_xb
            HymnTypes.XG -> R.string.hymn_type_name_xg
            HymnTypes.YB -> R.string.hymn_type_name_yb
            HymnTypes.ER -> R.string.hymn_type_name_er
            else -> R.string.hymn_type_name_db
        },
    )

    private fun numberLabel(ctx: Context, ref: HymnRef): String =
        if (ref.isFu) ctx.getString(R.string.c_label_fu, ref.displayNo) else ctx.getString(R.string.c_label_no, ref.displayNo)

    /** e.g. "補充本 第 37 首", "大本詩歌 附 3". */
    fun headline(ctx: Context, ref: HymnRef): String = "${typeName(ctx, ref.book)} ${numberLabel(ctx, ref)}"

    /** Compact form for the recent chips: "補37", "大123", "新詩12", "大附3", "青附1". */
    fun chip(ctx: Context, ref: HymnRef): String {
        val short = shortName(ctx, ref.book)
        return ctx.getString(if (ref.isFu) R.string.c_chip_fu_fmt else R.string.c_chip_fmt, short, ref.displayNo)
    }

    /** For TalkBack: "補充本，第 37 首，祂的計劃". */
    fun spoken(ctx: Context, ref: HymnRef, title: String?): String =
        if (title.isNullOrBlank()) {
            headline(ctx, ref).replaceFirst(" ", "，")
        } else {
            ctx.getString(R.string.c_chip_desc, typeName(ctx, ref.book), numberLabel(ctx, ref), title)
        }

    /** Short name of a source for the buttons ("大本", "補充", ..., "英文"). */
    fun sourceName(ctx: Context, source: HymnSource): String = ctx.getString(
        when (source) {
            HymnSource.DB -> R.string.c_src_db
            HymnSource.BB -> R.string.c_src_bb
            HymnSource.XB -> R.string.c_src_xb
            HymnSource.XG -> R.string.c_src_xg
            HymnSource.YB -> R.string.c_src_yb
            HymnSource.ER -> R.string.c_src_er
            HymnSource.ENGLISH -> R.string.c_src_en
        },
    )

    /** Long name ("補充本") of a source, used where a sentence needs a book name. */
    fun longName(ctx: Context, source: HymnSource): String =
        source.book?.let { typeName(ctx, it) } ?: sourceName(ctx, source)

    private fun shortName(ctx: Context, book: String): String = ctx.getString(
        when (book) {
            HymnTypes.DB -> R.string.c_short_db
            HymnTypes.BB -> R.string.c_short_bb
            HymnTypes.XB -> R.string.c_short_xb
            HymnTypes.XG -> R.string.c_short_xg
            HymnTypes.YB -> R.string.c_short_yb
            HymnTypes.ER -> R.string.c_short_er
            else -> R.string.c_short_db
        },
    )
}
