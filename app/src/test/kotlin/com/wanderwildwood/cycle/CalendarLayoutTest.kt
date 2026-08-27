package com.wanderwildwood.cycle

import com.wanderwildwood.cycle.cycle.forecast
import com.wanderwildwood.cycle.ui.predictedPeriod
import com.wanderwildwood.cycle.ui.weeksOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarLayoutTest {

    @Test fun `every row has seven cells`() {
        for (m in 1..12) {
            val weeks = weeksOf(YearMonth.of(2026, m), DayOfWeek.MONDAY)
            assertTrue(weeks.all { it.size == 7 })
        }
    }

    @Test fun `the first of the month lands on its real weekday`() {
        // 1 August 2026 is a Saturday.
        val weeks = weeksOf(YearMonth.of(2026, 8), DayOfWeek.MONDAY)
        val first = weeks.first()
        assertEquals(5, first.indexOfFirst { it != null })
        assertEquals(LocalDate.parse("2026-08-01"), first[5])
    }

    @Test fun `a week starting Sunday shifts the lead by one`() {
        val weeks = weeksOf(YearMonth.of(2026, 8), DayOfWeek.SUNDAY)
        assertEquals(6, weeks.first().indexOfFirst { it != null })
    }

    @Test fun `no day is lost or repeated`() {
        val month = YearMonth.of(2026, 2)
        val days = weeksOf(month, DayOfWeek.MONDAY).flatten().filterNotNull()
        assertEquals(month.lengthOfMonth(), days.size)
        assertEquals(days.size, days.distinct().size)
    }

    @Test fun `a month that fills exactly gets no padding`() {
        // February 2027 has 28 days and starts on a Monday.
        val weeks = weeksOf(YearMonth.of(2027, 2), DayOfWeek.MONDAY)
        assertEquals(4, weeks.size)
        assertTrue(weeks.flatten().none { it == null })
    }

    @Test fun `the expected period runs for the average length`() {
        val history = (0..2).map { LocalDate.parse("2021-01-05").plusDays(it.toLong()) } +
            (0..2).map { LocalDate.parse("2021-02-09").plusDays(it.toLong()) } +
            (0..3).map { LocalDate.parse("2021-03-12").plusDays(it.toLong()) }
        val f = forecast(history, LocalDate.parse("2021-03-20"))
        val range = predictedPeriod(f)!!
        assertEquals(f.nextStart, range.start)
        assertEquals(f.periodLength.toLong(), range.start.until(range.endInclusive).days + 1L)
    }

    @Test fun `nothing is predicted with no history`() {
        assertNull(predictedPeriod(forecast(emptyList(), LocalDate.parse("2026-08-27"))))
    }
}
