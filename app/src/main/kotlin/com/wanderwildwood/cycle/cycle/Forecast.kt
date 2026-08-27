package com.wanderwildwood.cycle.cycle

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * One run of bleeding days, taken as a single period.
 */
data class Period(val start: LocalDate, val end: LocalDate) {
    val length: Int get() = (ChronoUnit.DAYS.between(start, end) + 1).toInt()
}

/**
 * Everything the screens need to say about where you are in your cycle.
 *
 * [nextStart], [ovulation] and [fertile] are null until there is at least one recorded period to
 * count from; a forecast with nothing behind it would be a guess dressed up as a date.
 */
data class Forecast(
    val periods: List<Period>,
    val cycleLength: Int,
    val periodLength: Int,
    val nextStart: LocalDate?,
    val ovulation: LocalDate?,
    val fertile: ClosedRange<LocalDate>?,
    /** Negative once the predicted date has passed, which the screens read as "late". */
    val daysUntilNextStart: Int?,
    val estimated: Boolean,
)

/**
 * Two days apart still counts as the same period.
 *
 * A day missed in the middle of a period is a logging gap, not the end of one; without this a
 * single forgotten tap splits one period into two and halves the next cycle length. Three days
 * apart starts a new period, which is safe because no real cycle is that short.
 */
const val TOLERATED_GAP_DAYS = 2L

/**
 * How many past cycles the prediction is taken over.
 *
 * Three, because it is what the trackers people arrive here from predict over, and a prediction
 * that moves for a reason you recognise is worth more than a more defensible one that does not.
 */
private const val WINDOW = 3

/** Used only until you have recorded a cycle of your own. */
private const val DEFAULT_CYCLE_LENGTH = 28
private const val DEFAULT_PERIOD_LENGTH = 3

/** Ovulation is counted back from the next period, not forward from the last one. */
private const val LUTEAL_DAYS = 14L

/** Five days before ovulation through two days after: eight days inclusive. */
private const val FERTILE_BEFORE = 5L
private const val FERTILE_AFTER = 2L

/**
 * Group logged days into periods, oldest first.
 */
fun periodsFrom(days: Collection<LocalDate>): List<Period> {
    if (days.isEmpty()) return emptyList()
    val sorted = days.distinct().sorted()
    val out = mutableListOf<Period>()
    var start = sorted.first()
    var previous = start
    for (day in sorted.drop(1)) {
        if (ChronoUnit.DAYS.between(previous, day) > TOLERATED_GAP_DAYS) {
            out += Period(start, previous)
            start = day
        }
        previous = day
    }
    out += Period(start, previous)
    return out
}

/**
 * Work out where you are, from the days you have logged.
 *
 * The numbers are taken over the last [WINDOW] cycles only. Older history stays on the calendar but
 * stops steering the prediction, so a cycle that settles down is reflected within a few months
 * rather than being held back by a year of earlier numbers.
 *
 * [unknownLengthStarts] are periods you started but never marked an end for — imported history has
 * a number of them. They still count as cycle starts, because the start is what you recorded and
 * it is what the cycle length is measured between. They are left out of the *period length*, where
 * a single stored day would otherwise read as a one-day period and pull it down.
 */
fun forecast(
    days: Collection<LocalDate>,
    today: LocalDate,
    unknownLengthStarts: Set<LocalDate> = emptySet(),
): Forecast {
    val periods = periodsFrom(days)

    val gaps = periods.zipWithNext { a, b -> ChronoUnit.DAYS.between(a.start, b.start).toInt() }
    val recentGaps = gaps.takeLast(WINDOW)
    val cycleLength = if (recentGaps.isEmpty()) DEFAULT_CYCLE_LENGTH else recentGaps.median()

    // The period in progress is excluded: its length is not known until it ends, and counting a
    // period that is one day old so far would pull the figure down every single month.
    val finished = periods
        .filter { ChronoUnit.DAYS.between(it.end, today) > TOLERATED_GAP_DAYS }
        .filterNot { it.start in unknownLengthStarts }
    val recentLengths = finished.takeLast(WINDOW).map { it.length }
    val periodLength = if (recentLengths.isEmpty()) DEFAULT_PERIOD_LENGTH else recentLengths.median()

    val lastStart = periods.lastOrNull()?.start
    val nextStart = lastStart?.plusDays(cycleLength.toLong())
    val ovulation = nextStart?.minusDays(LUTEAL_DAYS)
    val fertile = ovulation?.let { it.minusDays(FERTILE_BEFORE)..it.plusDays(FERTILE_AFTER) }

    return Forecast(
        periods = periods,
        cycleLength = cycleLength,
        periodLength = periodLength,
        nextStart = nextStart,
        ovulation = ovulation,
        fertile = fertile,
        daysUntilNextStart = nextStart?.let { ChronoUnit.DAYS.between(today, it).toInt() },
        // True while the numbers are still the defaults rather than your own.
        estimated = recentGaps.isEmpty(),
    )
}

/**
 * The middle value, not the mean.
 *
 * Cycles cluster tightly and then occasionally do not. One unusually long cycle inside a window of
 * three is enough to pull a mean far past anything that has actually happened, and to date the next
 * period more than a week late — a failure an averaging tracker makes and then keeps making. A
 * median ignores the outlier and keeps reporting the cycle underneath it, then moves once a change
 * is real rather than once it is loud.
 *
 * With an even count there is no single middle, so the two middle values are averaged; that only
 * happens while you have fewer than [WINDOW] cycles recorded.
 */
private fun List<Int>.median(): Int {
    val sorted = sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 1) {
        sorted[middle]
    } else {
        Math.round((sorted[middle - 1] + sorted[middle]) / 2.0).toInt()
    }
}


/**
 * Whether today is a day you have yet to confirm.
 *
 * A period is never declared over. You confirm each day it is still going, and when you stop
 * confirming, it stops — there is no "period ended" button because not tapping one is already the
 * answer. This is the window in which that has not been settled yet: your last recorded day is
 * recent enough that the period is probably still running, but today is not recorded, so the app
 * does not know and must not say.
 *
 * The window is [TOLERATED_GAP_DAYS] because that is the same span [periodsFrom] joins across. The
 * app offers to continue a period exactly as long as tapping would in fact continue it, rather
 * than offering a continuation that would quietly start a second period instead.
 */
fun awaitingConfirmation(forecast: Forecast, today: LocalDate, bleedingToday: Boolean): Boolean {
    if (bleedingToday) return false
    val last = forecast.periods.lastOrNull() ?: return false
    return ChronoUnit.DAYS.between(last.end, today) in 1..TOLERATED_GAP_DAYS
}

/** Which day of the current period today is, counting the first recorded day as day one. */
fun dayOfPeriod(forecast: Forecast, today: LocalDate): Int? {
    val start = forecast.periods.lastOrNull()?.start ?: return null
    return (ChronoUnit.DAYS.between(start, today) + 1).toInt()
}
