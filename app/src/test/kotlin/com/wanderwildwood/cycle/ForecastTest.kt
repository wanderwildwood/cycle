package com.wanderwildwood.cycle

import com.wanderwildwood.cycle.cycle.forecast
import com.wanderwildwood.cycle.cycle.periodsFrom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ForecastTest {

    private fun run(start: String, length: Int) =
        (0 until length).map { LocalDate.parse(start).plusDays(it.toLong()) }

    @Test fun `no history predicts nothing`() {
        val f = forecast(emptyList(), LocalDate.parse("2021-08-27"))
        assertTrue(f.periods.isEmpty())
        assertNull(f.nextStart)
        assertNull(f.ovulation)
        assertNull(f.fertile)
    }

    @Test fun `consecutive days are one period`() {
        val periods = periodsFrom(run("2021-08-01", 5))
        assertEquals(1, periods.size)
        assertEquals(5, periods.first().length)
    }

    @Test fun `a single missed day does not split a period`() {
        val days = run("2021-08-01", 2) + run("2021-08-04", 2)   // 3rd not logged
        val periods = periodsFrom(days)
        assertEquals(1, periods.size)
        assertEquals(LocalDate.parse("2021-08-05"), periods.first().end)
    }

    @Test fun `the spread of recent cycles is carried, not just their middle`() {
        // 28, then 32: a median of 30 that no cycle actually was.
        val days = run("2021-06-01", 4) + run("2021-06-29", 4) + run("2021-07-31", 4)
        val f = forecast(days, LocalDate.parse("2021-08-05"))
        assertEquals(30, f.cycleLength)
        assertEquals(28..32, f.cycleRange)
    }

    @Test fun `one cycle has nothing to be a range of`() {
        val days = run("2021-06-01", 4) + run("2021-06-29", 4)
        val f = forecast(days, LocalDate.parse("2021-07-05"))
        assertNull(f.cycleRange)
    }

    @Test fun `a real gap starts a new period`() {
        val periods = periodsFrom(run("2021-06-01", 5) + run("2021-06-29", 5))
        assertEquals(2, periods.size)
    }

    @Test fun `cycle length reads the last three cycles only`() {
        // Starts 30, 30, 26, 26, 26 days apart. The first two must not pull the figure up.
        val days = run("2021-01-01", 4) + run("2021-01-31", 4) + run("2021-03-02", 4) +
            run("2021-03-28", 4) + run("2021-04-23", 4) + run("2021-05-19", 4)
        val f = forecast(days, LocalDate.parse("2021-05-30"))
        assertEquals(26, f.cycleLength)
        assertEquals(LocalDate.parse("2021-06-14"), f.nextStart)
    }

    @Test fun `ovulation is fourteen days before the next period`() {
        val days = run("2021-07-01", 5) + run("2021-07-29", 5)
        val f = forecast(days, LocalDate.parse("2021-08-10"))
        assertEquals(LocalDate.parse("2021-08-26"), f.nextStart)
        assertEquals(LocalDate.parse("2021-08-12"), f.ovulation)
    }

    @Test fun `the fertile window is eight days and contains ovulation`() {
        val days = run("2021-07-01", 5) + run("2021-07-29", 5)
        val f = forecast(days, LocalDate.parse("2021-08-10"))
        val fertile = f.fertile!!
        assertEquals(LocalDate.parse("2021-08-07"), fertile.start)
        assertEquals(LocalDate.parse("2021-08-14"), fertile.endInclusive)
        assertTrue(f.ovulation!! in fertile)
    }

    @Test fun `days until goes negative when the prediction has passed`() {
        val days = run("2021-07-01", 5) + run("2021-07-29", 5)
        val f = forecast(days, LocalDate.parse("2021-08-29"))
        assertEquals(-3, f.daysUntilNextStart)
    }

    @Test fun `a period still in progress does not shorten the period length`() {
        // Three finished five-day periods, then one that started yesterday.
        val days = run("2021-05-01", 5) + run("2021-05-29", 5) + run("2021-06-26", 5) +
            run("2021-07-24", 2)
        val f = forecast(days, LocalDate.parse("2021-07-25"))
        assertEquals(5, f.periodLength)
    }

    @Test fun `one long cycle does not move the prediction`() {
        // 28, 52, 27 days apart. A mean would report 36 and date the next period nine days late;
        // the median reports the 28-day cycle underneath. One long cycle among short ones is the
        // case the median exists for, which is why it is the one written down.
        val days = run("2021-01-05", 3) + run("2021-02-02", 3) + run("2021-03-26", 3) +
            run("2021-04-22", 3)
        val f = forecast(days, LocalDate.parse("2021-05-01"))
        assertEquals(28, f.cycleLength)
        assertEquals(LocalDate.parse("2021-05-20"), f.nextStart)
    }

    @Test fun `one long period does not stretch the period length`() {
        // Three, eight and three days of bleeding: the middle one must not carry the figure.
        val days = run("2021-01-05", 3) + run("2021-02-02", 8) + run("2021-03-02", 3)
        val f = forecast(days, LocalDate.parse("2021-03-20"))
        assertEquals(3, f.periodLength)
    }

    @Test fun `with two cycles recorded the middle pair is averaged`() {
        // No single middle value yet, so 26 and 29 give 28 (27.5 rounded).
        val days = run("2021-01-05", 3) + run("2021-01-31", 3) + run("2021-03-01", 3)
        val f = forecast(days, LocalDate.parse("2021-03-10"))
        assertEquals(28, f.cycleLength)
    }

    @Test fun `defaults are flagged as estimated until a cycle has been recorded`() {
        val f = forecast(run("2021-08-01", 4), LocalDate.parse("2021-08-10"))
        assertTrue(f.estimated)
        assertEquals(28, f.cycleLength)
        assertEquals(LocalDate.parse("2021-08-29"), f.nextStart)
    }
}
