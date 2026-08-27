package com.wanderwildwood.cycle

import com.wanderwildwood.cycle.data.days
import com.wanderwildwood.cycle.data.parsePeriodTrackerExport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The fixture below is invented — dates, notes and all. The format is the real one; the content is
 * not, and no part of anybody's real export belongs in this repository.
 */
class ImportTest {

    private val export = """
        Period Tracker Notes

        Period Start	Mar 12, 2021
        Period End	Mar 15, 2021
        Cycle Length	31

        Period Start	Feb 09, 2021
        Period End	n/a
        Cycle Length	31

        Period Start	Jan 05, 2021
        Period End	Jan 07, 2021
        Cycle Length	35
        Notes	
        1/20/21	Symptoms: headache (mild)
        1/12/21	Notes: swimming
    """.trimIndent()

    @Test fun `reads every period, oldest first`() {
        val out = parsePeriodTrackerExport(export)
        assertEquals(3, out.periods.size)
        assertEquals(LocalDate.parse("2021-01-05"), out.periods.first().start)
        assertEquals(LocalDate.parse("2021-03-12"), out.periods.last().start)
    }

    @Test fun `an unrecorded end stays unknown rather than being guessed`() {
        val out = parsePeriodTrackerExport(export)
        val february = out.periods.single { it.start == LocalDate.parse("2021-02-09") }
        assertNull(february.end)
        assertEquals(listOf(LocalDate.parse("2021-02-09")), february.days())
    }

    @Test fun `a recorded end expands to every day inclusive`() {
        val out = parsePeriodTrackerExport(export)
        val march = out.periods.single { it.start == LocalDate.parse("2021-03-12") }
        assertEquals(4, march.days().size)
        assertEquals(LocalDate.parse("2021-03-15"), march.days().last())
    }

    @Test fun `notes keep their own dates`() {
        val out = parsePeriodTrackerExport(export)
        assertEquals(2, out.notes.size)
        assertEquals(LocalDate.parse("2021-01-12"), out.notes.first().day)
        assertEquals("Symptoms: headache (mild)", out.notes.last().text)
    }

    @Test fun `the stated cycle length is ignored`() {
        // The newest entry's Cycle Length is that app's prediction, not a gap to anything.
        val out = parsePeriodTrackerExport(export)
        assertTrue(out.periods.all { it.end == null || it.end!! >= it.start })
    }

    @Test fun `a line the parser does not know is skipped, not thrown on`() {
        val out = parsePeriodTrackerExport(
            "Period Start\tMar 12, 2021\nsomething else entirely\nPeriod End\tMar 15, 2021\n"
        )
        assertEquals(1, out.periods.size)
        assertEquals(LocalDate.parse("2021-03-15"), out.periods.single().end)
    }

    @Test fun `an empty export yields nothing rather than failing`() {
        val out = parsePeriodTrackerExport("")
        assertTrue(out.periods.isEmpty())
        assertTrue(out.notes.isEmpty())
    }
}
