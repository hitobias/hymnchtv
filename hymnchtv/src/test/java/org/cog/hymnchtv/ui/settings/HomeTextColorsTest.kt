package org.cog.hymnchtv.ui.settings

import android.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeTextColorsTest {
    private val black = 0xFF212121.toInt() // the app's grey900, the "black" choice

    @Test
    fun everyChoiceRoundTrips() {
        for (name in HomeTextColors.NAMES) {
            val color = checkNotNull(HomeTextColors.colorOf(name, black)) { name }
            assertThat(HomeTextColors.nameOf(color, black)).isEqualTo(name)
        }
    }

    @Test
    fun namesMapToTheOldMenuColors() {
        assertThat(HomeTextColors.colorOf("red", black)).isEqualTo(Color.RED)
        assertThat(HomeTextColors.colorOf("grey", black)).isEqualTo(Color.GRAY)
        assertThat(HomeTextColors.colorOf("black", black)).isEqualTo(black)
    }

    @Test
    fun unknownNameOrColorIsNull() {
        assertThat(HomeTextColors.colorOf("purple", black)).isNull()
        assertThat(HomeTextColors.nameOf(0xFF123456.toInt(), black)).isNull()
    }
}
