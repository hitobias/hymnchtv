package org.cog.hymnchtv.notebook.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FavoriteIdsTest {
    @Test
    fun sameKeyAlwaysGivesSameId() {
        assertThat(FavoriteIds.forKey(HymnKey.of(HymnTypes.DB, 1)))
            .isEqualTo(FavoriteIds.forKey(HymnKey.of(HymnTypes.DB, 1)))
    }

    @Test
    fun differentKeysGiveDifferentIds() {
        val ids = listOf(
            HymnKey.of(HymnTypes.DB, 1), HymnKey.of(HymnTypes.BB, 1),
            HymnKey.of(HymnTypes.DB, 781), HymnKey.of(HymnTypes.ER, 1),
        ).map(FavoriteIds::forKey)
        assertThat(ids.toSet()).hasSize(4)
    }

    @Test
    fun idIsACanonicalUuid() {
        val id = FavoriteIds.forKey(HymnKey.of(HymnTypes.XG, 7))
        assertThat(NotebookValidation.uuid(id)).isEqualTo(id)
    }
}
