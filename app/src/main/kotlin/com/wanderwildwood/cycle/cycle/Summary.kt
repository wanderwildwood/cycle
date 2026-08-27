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

    val estimate = if (forecast.estimated) " (rough — not enough history yet)" else ""

    return when {
        until > 1 -> "Period due ${next.format(DayAndDate)}, $until days away$estimate."
        until == 1 -> "Period due tomorrow$estimate."
        until == 0 -> "Period due today$estimate."
        until == -1 -> "Period is 1 day late$estimate."
        else -> "Period is ${-until} days late$estimate."
    }
}
