package com.wanderwildwood.cycle

import com.wanderwildwood.cycle.data.BleedingDay
import com.wanderwildwood.cycle.data.DayNote
import com.wanderwildwood.cycle.data.formatBackup
import com.wanderwildwood.cycle.data.looksLikeBackup
import com.wanderwildwood.cycle.data.parseBackup
import com.wanderwildwood.cycle.data.parsePeriodTrackerExport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The backup is the only copy of a record that cannot be reconstructed from anywhere else, so what
 * these tests are really checking is that nothing is quietly dropped on the way out or the way in.
 *
 * Every date and note here is invented. Real history does not belong in this repository.
 */
class BackupTest {

    private val made = LocalDate.of(2026, 3, 4)

    private fun day(date: LocalDate) = date.toEpochDay()

    private fun roundTrip(days: List<BleedingDay>, notes: List<DayNote>): Pair<List<BleedingDay>, List<DayNote>> {
        val parsed = parseBackup(formatBackup(days, notes, made))
        return parsed.days to parsed.notes
    }

    @Test fun `a bare bleeding day survives the round trip`() {
        val days = listOf(BleedingDay(day(LocalDate.of(2026, 1, 5)), intensity = 3))
        val (back, notes) = roundTrip(days, emptyList())
        assertEquals(days, back)
        assertEquals(emptyList<DayNote>(), notes)
    }

    @Test fun `every intensity survives`() {
        val days = (1..3).map { BleedingDay(day(LocalDate.of(2026, 1, it)), intensity = it) }
        assertEquals(days, roundTrip(days, emptyList()).first)
    }

    @Test fun `an unrecorded length is not lost`() {
        val days = listOf(BleedingDay(day(LocalDate.of(2026, 1, 5)), lengthUnknown = true))
        val back = roundTrip(days, emptyList()).first
        assertEquals(days, back)
        assertTrue(back.single().lengthUnknown)
    }

    @Test fun `moods symptoms intimacy and a note all survive together`() {
        val notes = listOf(
            DayNote(
                day = day(LocalDate.of(2026, 1, 5)),
                moods = "Calm\nTired",
                symptoms = "Cramps\nHeadache",
                intimacy = true,
                note = "Slept badly.",
            )
        )
        assertEquals(notes, roundTrip(emptyList(), notes).second)
    }

    @Test fun `tags keep the order they were recorded in`() {
        val notes = listOf(DayNote(day(LocalDate.of(2026, 1, 5)), moods = "Tired\nCalm"))
        assertEquals("Tired\nCalm", roundTrip(emptyList(), notes).second.single().moods)
    }

    @Test fun `a day with only a note and no bleeding is kept`() {
        val notes = listOf(DayNote(day(LocalDate.of(2026, 1, 5)), note = "Nothing much."))
        val (days, back) = roundTrip(emptyList(), notes)
        assertEquals(emptyList<BleedingDay>(), days)
        assertEquals(notes, back)
    }

    @Test fun `a note of several lines survives`() {
        val notes = listOf(DayNote(day(LocalDate.of(2026, 1, 5)), note = "One.\nTwo.\nThree."))
        assertEquals("One.\nTwo.\nThree.", roundTrip(emptyList(), notes).second.single().note)
    }

    @Test fun `a note holding a blank line is not read as the end of the day`() {
        val notes = listOf(DayNote(day(LocalDate.of(2026, 1, 5)), note = "One.\n\nTwo."))
        val (_, back) = roundTrip(emptyList(), notes)
        assertEquals("One.\n\nTwo.", back.single().note)
        assertEquals(1, back.size)
    }

    @Test fun `a note that looks like the file format is still just a note`() {
        val text = "2026-02-02\nbleeding heavy\nintimacy"
        val notes = listOf(DayNote(day(LocalDate.of(2026, 1, 5)), note = text))
        val (days, back) = roundTrip(emptyList(), notes)
        assertEquals(text, back.single().note)
        assertEquals(emptyList<BleedingDay>(), days)   // the note did not become a second day
    }

    @Test fun `an empty record writes a file that reads back as empty`() {
        val parsed = parseBackup(formatBackup(emptyList(), emptyList(), made))
        assertEquals(emptyList<BleedingDay>(), parsed.days)
        assertEquals(emptyList<DayNote>(), parsed.notes)
    }

    @Test fun `a note with nothing on it is not written out`() {
        val notes = listOf(DayNote(day(LocalDate.of(2026, 1, 5))))
        val text = formatBackup(emptyList(), notes, made)
        assertFalse(text.contains("2026-01-05"))
    }

    @Test fun `days come out oldest first whatever order they went in`() {
        val days = listOf(
            BleedingDay(day(LocalDate.of(2026, 3, 1))),
            BleedingDay(day(LocalDate.of(2026, 1, 1))),
            BleedingDay(day(LocalDate.of(2026, 2, 1))),
        )
        val back = roundTrip(days, emptyList()).first
        assertEquals(back.sortedBy { it.day }, back)
    }

    @Test fun `a line the parser does not know is skipped, not thrown on`() {
        val text = """
            Cycle Tracking backup
            Made 4 March 2026

            2026-01-05
            bleeding light
            something from a later version
            intimacy
        """.trimIndent()
        val parsed = parseBackup(text)
        assertEquals(1, parsed.days.size)
        assertEquals(1, parsed.days.single().intensity)
        assertTrue(parsed.notes.single().intimacy)
    }

    @Test fun `our own backup is told apart from a Period Tracker export`() {
        assertTrue(looksLikeBackup(formatBackup(emptyList(), emptyList(), made)))
        assertFalse(looksLikeBackup("Period Start\tJul 5, 2022\nPeriod End\tJul 8, 2022"))
        assertFalse(looksLikeBackup(""))
    }

    @Test fun `the old export format still parses as itself`() {
        val imported = parsePeriodTrackerExport("Period Start\tJul 5, 2022\nPeriod End\tJul 8, 2022")
        assertEquals(1, imported.periods.size)
    }

    @Test fun `the file can be read without the app`() {
        val days = listOf(BleedingDay(day(LocalDate.of(2026, 1, 5)), intensity = 3))
        val notes = listOf(DayNote(day(LocalDate.of(2026, 1, 5)), symptoms = "Cramps"))
        val text = formatBackup(days, notes, made)
        assertTrue(text.startsWith("Cycle Tracking backup"))
        assertTrue(text.contains("Made 4 March 2026"))
        assertTrue(text.contains("2026-01-05"))
        assertTrue(text.contains("bleeding heavy"))
        assertTrue(text.contains("symptoms Cramps"))
    }

    @Test fun `a word typed by hand survives the file`() {
        val notes = listOf(
            DayNote(
                day = day(LocalDate.of(2026, 1, 5)),
                moods = "Calm\nQuietly hopeful",
                symptoms = "Sore hips",
            )
        )
        val back = roundTrip(emptyList(), notes).second.single()
        assertEquals("Calm\nQuietly hopeful", back.moods)
        assertEquals("Sore hips", back.symptoms)
    }
}
