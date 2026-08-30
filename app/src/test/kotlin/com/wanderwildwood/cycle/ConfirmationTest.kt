package com.wanderwildwood.cycle

import com.wanderwildwood.cycle.cycle.awaitingConfirmation
import com.wanderwildwood.cycle.cycle.dayOfPeriod
import com.wanderwildwood.cycle.cycle.forecast
import com.wanderwildwood.cycle.cycle.summary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * A period is confirmed a day at a time and is never ended.
 *
 * A tap for each day it is still going; when the taps stop, it stops. Everything here is
 * that rule seen from one side or another.
 */
class ConfirmationTest {

    private fun run(start: String, length: Int) =
        (0 until length).map { LocalDate.parse(start).plusDays(it.toLong()) }

    /**
     * Two earlier periods so there is a cycle length, then one starting 2021-03-04.
     *
     * The dates are invented and deliberately nowhere near anybody's. A test that reads
     * realistically is not worth a real person's record living in a public history.
     */
    private val history = run("2021-01-07", 3) + run("2021-02-04", 3) + run("2021-03-04", 1)

    private fun at(date: String) = forecast(history, LocalDate.parse(date))

    @Test fun `a day already recorded is not awaiting anything`() {
        val today = LocalDate.parse("2021-03-04")
        assertFalse(awaitingConfirmation(at("2021-03-04"), today, bleedingToday = true))
    }

    @Test fun `the day after a recorded day is awaiting confirmation`() {
        val today = LocalDate.parse("2021-03-05")
        assertTrue(awaitingConfirmation(at("2021-03-05"), today, bleedingToday = false))
    }

    @Test fun `two days on it is still awaiting, because a tap would still join`() {
        val today = LocalDate.parse("2021-03-06")
        assertTrue(awaitingConfirmation(at("2021-03-06"), today, bleedingToday = false))
    }

    @Test fun `three days on the period has ended itself`() {
        // Nothing was pressed to end it. Confirmation stopped, so it is over.
        val today = LocalDate.parse("2021-03-07")
        assertFalse(awaitingConfirmation(at("2021-03-07"), today, bleedingToday = false))
    }

    @Test fun `with no history there is nothing to confirm`() {
        val today = LocalDate.parse("2021-03-05")
        assertFalse(awaitingConfirmation(forecast(emptyList(), today), today, bleedingToday = false))
    }

    @Test fun `the count keeps running through an unconfirmed day`() {
        assertEquals(2, dayOfPeriod(at("2021-03-05"), LocalDate.parse("2021-03-05")))
    }

    @Test fun `the sent line does not give out a countdown mid-period`() {
        // The old behaviour here read "Period due ..., 26 days away" on day two of a period.
        val today = LocalDate.parse("2021-03-05")
        val line = summary(today, bleedingToday = false, forecast = at("2021-03-05"))
        assertEquals("Period started Thursday 4 March.", line)
    }

    @Test fun `the sent line goes back to the countdown once the period has ended`() {
        val today = LocalDate.parse("2021-03-07")
        val line = summary(today, bleedingToday = false, forecast = at("2021-03-07"))
        assertTrue(line, line.startsWith("Period expected"))
    }

    @Test fun `confirming a day two days later keeps it one period`() {
        // The window matches what periodsFrom joins across, so a tap offered is a tap that works.
        val days = run("2021-03-04", 1) + run("2021-03-06", 1)
        val f = forecast(days, LocalDate.parse("2021-03-06"))
        assertEquals(1, f.periods.size)
    }
}
