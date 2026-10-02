package org.cog.hymnchtv

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class DownloadOwnershipTest {
    private val media = mapOf(7L to File("a.mp3"))

    @Test
    fun ownIdIsOwned() {
        assertThat(DownloadOwnership.owns(media, 7L)).isTrue()
    }

    @Test
    fun foreignIdIsNotOwned() {
        // e.g. the update apk download id: the media receiver must not remove it
        assertThat(DownloadOwnership.owns(media, 8L)).isFalse()
        assertThat(DownloadOwnership.owns(emptyMap<Long, File>(), 8L)).isFalse()
        assertThat(DownloadOwnership.owns(media, -1L)).isFalse()
    }
}
