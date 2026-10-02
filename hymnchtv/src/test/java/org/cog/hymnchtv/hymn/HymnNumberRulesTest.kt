package org.cog.hymnchtv.hymn

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.junit.Test

class HymnNumberRulesTest {
    private fun can(book: String, prefix: String, digit: Int, fu: Boolean = false) =
        HymnNumberRules.canAppendDigit(book, prefix, fu, digit)

    @Test fun validNumberCountsMatchTheBooks() {
        mapOf(
            HymnTypes.DB to 786, HymnTypes.BB to 513, HymnTypes.XB to 168, HymnTypes.XG to 205,
            HymnTypes.ER to 330, HymnTypes.YB to 277,
        ).forEach { (book, n) -> assertWithMessage(book).that(HymnNumberRules.storedNumbers(book)).hasSize(n) }
    }

    @Test fun gapsAndMissingNumbers() {
        assertThat(HymnNumberRules.isValid(HymnTypes.BB, 37, false)).isTrue()
        assertThat(HymnNumberRules.isValid(HymnTypes.BB, 38, false)).isFalse()
        assertThat(HymnNumberRules.isValid(HymnTypes.ER, 17, false)).isTrue()
        assertThat(HymnNumberRules.isValid(HymnTypes.ER, 18, false)).isFalse()
        assertThat(HymnNumberRules.isValid(HymnTypes.XB, 168, false)).isFalse()
        assertThat(HymnNumberRules.isValid(HymnTypes.XB, 171, false)).isTrue()
        assertThat(HymnNumberRules.isValid(HymnTypes.XG, 34, false)).isFalse()
    }

    @Test fun dbPlainNumbersStopAt780AndFuIs1To6() {
        assertThat(HymnNumberRules.isValid(HymnTypes.DB, 780, false)).isTrue()
        assertThat(HymnNumberRules.isValid(HymnTypes.DB, 781, false)).isFalse()
        assertThat(HymnNumberRules.isValid(HymnTypes.DB, 6, true)).isTrue()
        assertThat(HymnNumberRules.isValid(HymnTypes.DB, 7, true)).isFalse()
        assertThat(HymnNumberRules.isValid(HymnTypes.YB, 2, true)).isTrue()
        assertThat(HymnNumberRules.isValid(HymnTypes.YB, 3, true)).isFalse()
    }

    @Test fun displayNumbersSplitPlainAndFu() {
        assertThat(HymnNumberRules.displayNumbers(HymnTypes.DB, true)).containsExactly(1, 2, 3, 4, 5, 6).inOrder()
        assertThat(HymnNumberRules.displayNumbers(HymnTypes.YB, true)).containsExactly(1, 2).inOrder()
        assertThat(HymnNumberRules.displayNumbers(HymnTypes.DB, false)).hasSize(780)
        assertThat(HymnNumberRules.displayNumbers(HymnTypes.BB, true)).isEmpty()
    }

    @Test fun digitKeysFollowRealCompletions() {
        assertThat(can(HymnTypes.BB, "3", 9)).isFalse()
        assertThat(can(HymnTypes.BB, "3", 7)).isTrue()
        assertThat(can(HymnTypes.BB, "3", 8)).isFalse()
        assertThat(can(HymnTypes.BB, "100", 1)).isTrue()
        assertThat(can(HymnTypes.BB, "100", 0)).isFalse()
        assertThat(can(HymnTypes.XB, "16", 8)).isFalse()
        assertThat(can(HymnTypes.XB, "17", 1)).isTrue()
        assertThat(can(HymnTypes.XG, "3", 4)).isFalse()
        assertThat(can(HymnTypes.ER, "12", 5)).isFalse()
        assertThat(can(HymnTypes.DB, "78", 1)).isFalse()
        assertThat(can(HymnTypes.DB, "", 7, fu = true)).isFalse()
        assertThat(can(HymnTypes.DB, "", 6, fu = true)).isTrue()
        assertThat(can(HymnTypes.DB, "", 0)).isFalse()
    }

    @Test fun lengthIsCappedAtFourDigits() {
        assertThat(can(HymnTypes.BB, "1001", 0)).isFalse()
    }

    @Test fun englishDigitsFollowTheEnglishNumberSet() {
        assertThat(HymnNumberRules.canAppendEnglishDigit("1", 2, listOf(12, 30))).isTrue()
        assertThat(HymnNumberRules.canAppendEnglishDigit("1", 3, listOf(12, 30))).isFalse()
        assertThat(HymnNumberRules.canAppendEnglishDigit("", 0, listOf(12, 30))).isFalse()
    }

    @Test fun alsoValidInListsOtherBooksOnly() {
        val also = HymnNumberRules.alsoValidIn(40, isFu = false, except = HymnSource.BB)
        assertThat(also).containsExactly(HymnSource.DB, HymnSource.XB, HymnSource.XG, HymnSource.YB).inOrder()
        assertThat(HymnNumberRules.alsoValidIn(1, isFu = true, except = HymnSource.DB)).containsExactly(HymnSource.YB)
    }
}
