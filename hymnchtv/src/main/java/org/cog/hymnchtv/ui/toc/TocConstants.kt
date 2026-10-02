package org.cog.hymnchtv.ui.toc

/**
 * Table-of-contents constants (page names, toc file prefixes, category tables) formerly kept on the HymnToc activity.
 * Java callers use static imports of these fields.
 */
object TocConstants {
    // TocType for user selection
    const val TOC_TITLE = "目录"
    const val TOC_CATEGORY = "诗歌类别"
    const val TOC_STROKE = "笔画索引"
    const val TOC_PINYIN = "拼音索引"
    const val TOC_ENGLISH = "英中对照"

    @JvmField
    val hymnTocPage: List<String> = listOf(TOC_TITLE, TOC_CATEGORY, TOC_STROKE, TOC_PINYIN, TOC_ENGLISH)

    // The TOC prefix for creating the correct toc text file name
    const val TOC_ER = "toc_er"
    const val TOC_XB = "toc_xb"
    const val TOC_XG = "toc_xg"
    const val TOC_YB = "toc_yb"
    const val TOC_BB = "toc_bb"
    const val TOC_DB = "toc_db"

    @JvmField
    val hymnCategoryDb: Array<String> = TocData.hymnCategoryDb
    @JvmField
    val hymnCategoryBb: Array<String> = TocData.hymnCategoryBb
    @JvmField
    val hymnCategoryXg: Array<String> = TocData.hymnCategoryXg
    @JvmField
    val hymnCategoryYb: Array<String> = TocData.hymnCategoryYb
    @JvmField
    val hymnCategoryXb: Array<String> = TocData.hymnCategoryXb
    @JvmField
    val hymnCategoryEr: Array<String> = TocData.hymnCategoryEr

    @JvmField
    val category_db: IntArray = TocData.categoryDb
    @JvmField
    val category_bb: IntArray = TocData.categoryBb
    @JvmField
    val category_er: IntArray = TocData.categoryEr
    @JvmField
    val category_xg: IntArray = TocData.categoryXg
    @JvmField
    val category_xb: IntArray = TocData.categoryXb
    @JvmField
    val category_yb: IntArray = TocData.categoryYb
}
