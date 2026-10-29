package com.wanderwildwood.cycle

import com.wanderwildwood.cycle.cycle.fertileAround
import com.wanderwildwood.cycle.cycle.forecast
import com.wanderwildwood.cycle.data.BleedingDay
import com.wanderwildwood.cycle.data.DayNote
import com.wanderwildwood.cycle.data.TEST_NEGATIVE
import com.wanderwildwood.cycle.data.TEST_POSITIVE
import com.wanderwildwood.cycle.data.formatBackup
import com.wanderwildwood.cycle.data.parseBackup
import com.wanderwildwood.cycle.data.readFile
import com.wanderwildwood.cycle.data.Loaded
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Invented dates throughout. */
class OvulationTest {

    private fun d(s: String) = LocalDate.parse(s)
    private fun run(start: String, length: Int) = (0 until length).map { d(start).plusDays(it.toLong()) }

    // Two 28-day cycles, the second period starting 2026-02-01 and the third 2026-03-01.
    private val history = run("2026-01-04", 4) + run("2026-02-01", 4) + run("2026-03-01", 4)
    private val today = d("2026-03-20")

    @Test fun `with no test the prediction is the calendar one`() {
        val f = forecast(history, today)
        assertEquals(d("2026-03-15"), f.ovulation)   // 29 Mar start minus 14
        assertFalse(f.ovulationConfirmed)
        assertEquals(fertileAround(d("2026-03-15")), f.fertile)
        assertTrue(f.pastOvulations.isEmpty())
    }

    @Test fun `a positive test puts ovulation on the next day and moves the window`() {
        val f = forecast(history, today, positiveTests = listOf(d("2026-03-10")))
        assertTrue(f.ovulationConfirmed)
        assertEquals(d("2026-03-11"), f.ovulation)
        assertEquals(fertileAround(d("2026-03-11")), f.fertile)
        // the next period is not moved by a test
        assertEquals(forecast(history, today).nextStart, f.nextStart)
    }

    @Test fun `the first of several positives times the cycle`() {
        val f = forecast(history, today, positiveTests = listOf(d("2026-03-12"), d("2026-03-11")))
        assertEquals(d("2026-03-12"), f.ovulation)
    }

    @Test fun `removing the positive returns to the prediction`() {
        val with = forecast(history, today, positiveTests = listOf(d("2026-03-10")))
        val without = forecast(history, today, positiveTests = emptyList())
        assertTrue(with.ovulation != without.ovulation)
        assertEquals(forecast(history, today).ovulation, without.ovulation)
    }

    @Test fun `re-logging a positive on another day moves ovulation with it`() {
        val a = forecast(history, today, positiveTests = listOf(d("2026-03-10")))
        val b = forecast(history, today, positiveTests = listOf(d("2026-03-13")))
        assertEquals(d("2026-03-11"), a.ovulation)
        assertEquals(d("2026-03-14"), b.ovulation)
    }

    @Test fun `a positive from an earlier cycle does not steer this one`() {
        val f = forecast(history, today, positiveTests = listOf(d("2026-02-12")))
        assertFalse(f.ovulationConfirmed)
        assertEquals(forecast(history, today).ovulation, f.ovulation)
        assertEquals(listOf(d("2026-02-13")), f.pastOvulations.map { it.day })
        assertEquals(fertileAround(d("2026-02-13")), f.pastOvulations.single().fertile)
    }

    @Test fun `each cycle has its own ovulation`() {
        val f = forecast(history, today, positiveTests = listOf(d("2026-01-16"), d("2026-02-12"), d("2026-03-10")))
        assertEquals(d("2026-03-11"), f.ovulation)
        assertEquals(listOf(d("2026-01-17"), d("2026-02-13")), f.pastOvulations.map { it.day })
    }

    @Test fun `a positive before any period or after today is ignored`() {
        val f = forecast(history, today, positiveTests = listOf(d("2025-12-20"), d("2026-03-25")))
        assertFalse(f.ovulationConfirmed)
        assertTrue(f.pastOvulations.isEmpty())
    }

    @Test fun `no periods and a positive predicts nothing`() {
        val f = forecast(emptyList(), today, positiveTests = listOf(d("2026-03-10")))
        assertNull(f.ovulation)
        assertNull(f.fertile)
    }

    @Test fun `the data model counts the new fields as data`() {
        assertTrue(DayNote(1).isEmpty())
        assertFalse(DayNote(1, mucus = "watery").isEmpty())
        assertFalse(DayNote(1, ovulationTest = TEST_NEGATIVE).isEmpty())
    }

    @Test fun `mucus and tests survive the backup round trip`() {
        val notes = listOf(
            DayNote(d("2026-03-09").toEpochDay(), mucus = "egg whites", ovulationTest = TEST_NEGATIVE),
            DayNote(d("2026-03-10").toEpochDay(), mucus = "creamy/sticky", ovulationTest = TEST_POSITIVE, note = "x"),
            DayNote(d("2026-03-11").toEpochDay(), mucus = "watery"),
            DayNote(d("2026-03-12").toEpochDay(), ovulationTest = TEST_POSITIVE),
        )
        val text = formatBackup(listOf(BleedingDay(d("2026-03-01").toEpochDay(), 2)), notes, today)
        val back = parseBackup(text)
        assertEquals(notes, back.notes)
        assertTrue(readFile(text) is Loaded.FromBackup)
    }

    @Test fun `an old backup without the new lines still reads`() {
        val old = "Cycle Tracking backup\nMade 4 March 2026\n\n2026-01-05\nbleeding heavy\nmoods Calm\n"
        val back = parseBackup(old)
        assertEquals(1, back.days.size)
        assertEquals("", back.notes.single().mucus)
        assertEquals("", back.notes.single().ovulationTest)
    }

    @Test fun `unknown mucus or test words are dropped, not stored`() {
        val text = "Cycle Tracking backup\nMade 4 March 2026\n\n2026-01-05\nmucus sparkly\novulation test maybe\nnote\n  hi\n"
        val n = parseBackup(text).notes.single()
        assertEquals("", n.mucus)
        assertEquals("", n.ovulationTest)
    }
}
