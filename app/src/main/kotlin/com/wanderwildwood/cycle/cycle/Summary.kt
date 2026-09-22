package com.wanderwildwood.cycle.cycle

import java.time.LocalDate

/**
 * The line you send a partner, decided but not yet worded.
 *
 * One sentence, and only the part a partner has any use for: where you are now and when the next
 * one is due. Not the history, not the notes, not the symptoms — those are yours, and a share
 * feature that quietly includes them is a share feature nobody can use carefully. Nothing here
 * has a field that could hold them.
 *
 * The words come from strings.xml, in the reader's language, at the one place that sends it.
 */
sealed interface Summary {
    /** Today is marked: this is [day] of the period. */
    data class Bleeding(val day: Int) : Summary

    /** Mid-period with today not yet confirmed: only the start, on [on], is said. */
    data class Started(val on: LocalDate) : Summary

    /** No period recorded, so nothing to count from. */
    data object NothingYet : Summary

    /**
     * The next period is expected on [on], [days] from today; negative once that date has passed.
     *
     * [rough] while the numbers are still the defaults rather than hers.
     */
    data class Expected(val on: LocalDate, val days: Int, val rough: Boolean) : Summary
}

/** What to tell a partner today. See [Summary]. */
fun summary(
    today: LocalDate,
    bleedingToday: Boolean,
    forecast: Forecast,
): Summary {
    val current = forecast.periods.lastOrNull()

    if (bleedingToday && current != null) {
        return Summary.Bleeding(dayOfPeriod(forecast, today) ?: 1)
    }

    // Mid-period with today not yet confirmed. The countdown below is measured from a period that
    // has probably not finished, so it would be off by however long this one still runs — and
    // "day two" is not a partner's to be told while you have not said it yourself. The start is the
    // part that is recorded, so the start is the part that gets sent.
    if (current != null && awaitingConfirmation(forecast, today, bleedingToday)) {
        return Summary.Started(current.start)
    }

    val next = forecast.nextStart ?: return Summary.NothingYet
    val until = forecast.daysUntilNextStart ?: return Summary.NothingYet

    // Said only while the numbers are still the defaults rather than hers. It means "there is
    // not enough history yet", which stops being true — unlike the uncertainty in "expected",
    // which does not. Two different admissions; making this one permanent would blur both.
    return Summary.Expected(next, until, rough = forecast.estimated)
}
