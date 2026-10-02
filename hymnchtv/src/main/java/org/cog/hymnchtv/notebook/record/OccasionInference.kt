package org.cog.hymnchtv.notebook.record

import org.cog.hymnchtv.notebook.model.Occasion
import java.util.Calendar
import java.util.TimeZone

/**
 * Guesses the occasion of an auto-recorded log (the user can correct it), using local wall-clock time in [zone]:
 * 1. Sunday 06:00 (incl.) – 13:00 (excl.) → LORDS_DAY.
 * 2. Otherwise the occasion the user last picked by hand, except LORDS_DAY which becomes HOME.
 * 3. Never picked → HOME.
 * Calendar handles DST; java.time would need API 26 (minSdk is 24).
 */
object OccasionInference {
    const val LORDS_DAY_START_HOUR = 6
    const val LORDS_DAY_END_HOUR = 13

    @JvmStatic
    fun infer(nowMillis: Long, zone: TimeZone, lastChosen: Occasion?): Occasion {
        val calendar = Calendar.getInstance(zone).apply { timeInMillis = nowMillis }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val isSunday = calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
        if (isSunday && hour >= LORDS_DAY_START_HOUR && hour < LORDS_DAY_END_HOUR) return Occasion.LORDS_DAY
        return when (lastChosen) {
            null, Occasion.LORDS_DAY -> Occasion.HOME
            else -> lastChosen
        }
    }
}
