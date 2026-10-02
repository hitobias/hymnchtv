package org.cog.hymnchtv.hymn

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.junit.Test

class HymnRefTest {
    @Test fun dbFuIsStoredAbove780() {
        val ref = HymnRef.fromEntry(HymnTypes.DB, 3, isFu = true)!!
        assertThat(ref).isEqualTo(HymnRef(HymnTypes.DB, 783))
        assertThat(ref.isFu).isTrue(); assertThat(ref.displayNo).isEqualTo(3)
    }

    @Test fun youthFuIsStoredAbove275() {
        val ref = HymnRef.fromEntry(HymnTypes.YB, 1, isFu = true)!!
        assertThat(ref.storedNo).isEqualTo(276); assertThat(ref.isFu).isTrue(); assertThat(ref.displayNo).isEqualTo(1)
        assertThat(HymnRef(HymnTypes.YB, 275).isFu).isFalse()
    }

    @Test fun noFuInOtherBooksAndOutOfRangeFuIsNull() {
        assertThat(HymnRef.fromEntry(HymnTypes.BB, 1, isFu = true)).isNull()
        assertThat(HymnRef.fromEntry(HymnTypes.DB, 7, isFu = true)).isNull()
        assertThat(HymnRef.fromEntry(HymnTypes.YB, 3, isFu = true)).isNull()
    }

    @Test fun storedAndDisplayRoundTrip() {
        for (book in HymnTypes.ALL) for (no in HymnNumberRules.storedNumbers(book)) {
            val ref = HymnRef(book, no)
            assertThat(HymnRef.fromEntry(book, ref.displayNo, ref.isFu)).isEqualTo(ref)
        }
    }

    @Test fun dummyEnglishOnlyHymnIsNotValid() = assertThat(HymnRef(HymnTypes.BB, 2000).isValid).isFalse()

    @Test fun plainNumberAboveFuOffsetIsRejected() {
        assertThat(HymnRef.fromEntry(HymnTypes.DB, 781, isFu = false)).isNull()
        assertThat(HymnRef.fromEntry(HymnTypes.DB, 780, isFu = false)).isEqualTo(HymnRef(HymnTypes.DB, 780))
    }
}
