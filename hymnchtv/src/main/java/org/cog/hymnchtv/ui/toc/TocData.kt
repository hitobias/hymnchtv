package org.cog.hymnchtv.ui.toc

/**
 * The category names of each hymn book and the hymn number at which each category starts (the last entry is one past the
 * end). Moved out of the HymnToc activity so the TOC can be built without loading an Activity class;
 * TocConstants re-exports them for Java callers.
 */
object TocData {
    /* 大本诗歌 db toc category */
    @JvmField
    val hymnCategoryDb: Array<String> = arrayOf(
        "颂三一神", "敬拜父", "赞美主", "圣灵丰满", "得救证实", "羡慕", "奉献", "联合基督",
        "经历基督", "经历神", "十架夸耀", "十架道路", "复活生命", "鼓励", "试炼安慰", "里面生命", "神医",
        "祷告", "读经", "召会", "聚会", "属灵争战", "事奉", "传扬福音", "福音", "受浸", "国度", "荣耀盼望", "终极显出", "附",
    )

    /* 补充本 bb toc category */
    @JvmField
    val hymnCategoryBb: Array<String> = arrayOf(
        "赞美的话", "灵与生命", "享受基督", "爱慕耶稣", "追求长进", "教会异象", "建造合一", "教会生活",
        "事奉福音", "盼望预备", "新约经纶",
    )

    /* 新诗歌本 xg toc category */
    @JvmField
    val hymnCategoryXg: Array<String> = arrayOf("新诗歌")

    @JvmField
    val hymnCategoryYb: Array<String> = arrayOf("青年诗歌")

    /* 新歌颂咏 xb toc category */
    @JvmField
    val hymnCategoryXb: Array<String> = arrayOf("新路实行", "福音喜信", "生命与灵", "召会生活", "新耶路撒冷", "新诗歌")

    /* 儿童诗歌 er toc category */
    @JvmField
    val hymnCategoryEr: Array<String> = arrayOf(
        "神的创造", "主的爱", "圣灵同在", "主的看顾", "赞美喜乐", "祷告读经", "爱主", "亲近倚靠",
        "彰显主", "召会聚会", "传扬福音", "发光争战", "经文故事",
    )

    /** Array contains the max hymnNo max (i.e. start number of next category) for each category */
    @JvmField
    val categoryDb: IntArray = intArrayOf(
        1, 6, 53, 194, 229, 269, 330, 356, 367, 441, 454, 458, 472,
        474, 490, 529, 548, 551, 579, 592, 624, 632, 650, 662, 670, 740, 745, 752, 768, 781, 787,
    )

    @JvmField
    val categoryBb: IntArray = intArrayOf(1, 101, 201, 301, 401, 501, 601, 701, 801, 901, 1001, 1101)

    @JvmField
    val categoryEr: IntArray = intArrayOf(1, 101, 201, 301, 401, 501, 601, 701, 801, 901, 1001, 1101, 1201, 1301)

    @JvmField
    val categoryXg: IntArray = intArrayOf(1, 300)

    @JvmField
    val categoryXb: IntArray = intArrayOf(1, 40, 74, 110, 131, 143, 170)

    @JvmField
    val categoryYb: IntArray = intArrayOf(1, 300)
}
