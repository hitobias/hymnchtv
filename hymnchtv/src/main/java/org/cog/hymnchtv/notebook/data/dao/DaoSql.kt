package org.cog.hymnchtv.notebook.data.dao

/** WHERE fragment matching the embedded HymnKey columns; DAO methods name their params hymnType/hymnNo/isFu. */
internal const val HYMN_MATCH = "hymnType = :hymnType AND hymnNo = :hymnNo AND isFu = :isFu"
