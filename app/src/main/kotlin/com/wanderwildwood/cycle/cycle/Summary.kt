package com.wanderwildwood.cycle.cycle

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DayAndDate = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())

/**
 * The line you send a partner.
 *
 * One sentence, and only the part a partner has any use for: where you are now and when the next
 * one is due. Not the history, not the notes, not the symptoms — those are yours, and a share
 * feature that quietly includes them is a share feature nobody can use carefully.
 */
fun summary(
    today: LocalDate,
    bleedingToday: Boolean,
    forecast: Forecast,
): String {
    val current = forecast.periods.lastOrNull()

    if (bleedingToday && current != null) {
        val day = dayOfPeriod(forecast, today) ?: 1
        return "Day $day of my period."
    }

    // Mid-period with today not yet confirmed. The countdown below is measured from a period that
    // has probably not finished, so it would be off by however long this one still runs — and
    // "day two" is not a partner's to be told while you have not said it yourself. The start is the
    // part that is recorded, so the start is the part that gets sent.
    if (current != null && awaitingConfirmation(forecast, today, bleedingToday)) {
        return "Period started ${current.start.format(DayAndDate)}."
    }

    val next = forecast.nextStart ?: return "Nothing recorded yet."
    val until = forecast.daysUntilNextStart ?: return "Nothing recorded yet."

    // Said only while the numbers are still the defaults rather than hers. It means "there is
    // not enough history yet", which stops being true — unlike the uncertainty below, which
    // does not. Two different admissions; making this one permanent would blur both.
    val estimate = if (forecast.estimated) " (rough — not enough history yet)" else ""

    // "Expected", not "due", and "later than expected", not "late".
    //
    // A date worked out from the median of three cycles is a guess, and it stays a guess after
    // thirty of them: more history makes it better, never certain. "Due" is the language of a
    // timetable, and against a timetable a body that arrives on its own schedule is at fault —
    // so the app was quietly reporting a failure of hers whenever its own arithmetic missed.
    // The estimate is the thing that was wrong. This says so, in the same breath and no more
    // words than before.
    return when {
        until > 1 -> "Period expected ${next.format(DayAndDate)}, $until days away$estimate."
        until == 1 -> "Period expected tomorrow$estimate."
        until == 0 -> "Period expected today$estimate."
        until == -1 -> "Period is 1 day later than expected$estimate."
        else -> "Period is ${-until} days later than expected$estimate."
    }
}
