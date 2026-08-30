package com.wanderwildwood.cycle

import com.wanderwildwood.cycle.cycle.forecast
import com.wanderwildwood.cycle.cycle.summary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class SummaryTest {

    private fun run(start: String, length: Int) =
        (0 until length).map { LocalDate.parse(start).plusDays(it.toLong()) }

    private val history = run("2021-01-05", 3) + run("2021-02-09", 3) + run("2021-03-12", 4)

    @Test fun `says the day when bleeding is marked`() {
        val today = LocalDate.parse("2021-03-13")
        val s = summary(today, true, forecast(history, today))
        assertEquals("Day 2 of my period.", s)
    }

    @Test fun `says the date and the count otherwise`() {
        val today = LocalDate.parse("2021-03-20")
        val s = summary(today, false, forecast(history, today))
        assertEquals("Period expected Wednesday 14 April, 25 days away.", s)
    }

    @Test fun `says how far past the estimate once the date has passed`() {
        val today = LocalDate.parse("2021-04-15")
        val s = summary(today, false, forecast(history, today))
        assertEquals("Period is 1 day later than expected.", s)
    }

    @Test fun `says nothing confident with no history`() {
        val today = LocalDate.parse("2021-03-20")
        assertEquals("Nothing recorded yet.", summary(today, false, forecast(emptyList(), today)))
    }

    @Test fun `carries no symptoms, notes or history`() {
        val today = LocalDate.parse("2021-03-20")
        val s = summary(today, false, forecast(history, today))
        // Whatever else changes, the line must not grow to include the rest of the record.
        assertFalse(s.contains("2021-01"))
        assertFalse(s.contains("cycle", ignoreCase = true))
        assertEquals(1, s.count { it == '.' })
    }
}
