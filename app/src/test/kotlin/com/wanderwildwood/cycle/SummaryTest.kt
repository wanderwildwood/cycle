package com.wanderwildwood.cycle

import com.wanderwildwood.cycle.cycle.Summary
import com.wanderwildwood.cycle.cycle.forecast
import com.wanderwildwood.cycle.cycle.summary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File
import java.time.LocalDate

class SummaryTest {

    private fun run(start: String, length: Int) =
        (0 until length).map { LocalDate.parse(start).plusDays(it.toLong()) }

    private val history = run("2021-01-05", 3) + run("2021-02-09", 3) + run("2021-03-12", 4)

    @Test fun `says the day when bleeding is marked`() {
        val today = LocalDate.parse("2021-03-13")
        val s = summary(today, true, forecast(history, today))
        assertEquals(Summary.Bleeding(day = 2), s)
    }

    @Test fun `says the date and the count otherwise`() {
        val today = LocalDate.parse("2021-03-20")
        val s = summary(today, false, forecast(history, today))
        assertEquals(Summary.Expected(on = LocalDate.parse("2021-04-14"), days = 25, rough = false), s)
    }

    @Test fun `says how far past the estimate once the date has passed`() {
        val today = LocalDate.parse("2021-04-15")
        val s = summary(today, false, forecast(history, today))
        // Negative: a day past the expected date, which is worded as later than expected.
        assertEquals(Summary.Expected(on = LocalDate.parse("2021-04-14"), days = -1, rough = false), s)
    }

    @Test fun `says nothing confident with no history`() {
        val today = LocalDate.parse("2021-03-20")
        assertEquals(Summary.NothingYet, summary(today, false, forecast(emptyList(), today)))
    }

    @Test fun `carries no symptoms, notes or history`() {
        val today = LocalDate.parse("2021-03-20")
        val s = summary(today, false, forecast(history, today))
        // Whatever else changes, the line must not grow to include the rest of the record: what is
        // decided is the next date and the count, and equality says there is nothing more in it.
        assertEquals(Summary.Expected(on = LocalDate.parse("2021-04-14"), days = 25, rough = false), s)
        assertFalse(s.toString().contains("2021-01"))
        assertFalse(s.toString().contains("cycle", ignoreCase = true))
    }

    @Test fun `every worded line is one sentence`() {
        // The wording lives in strings.xml, so that is where the one-sentence rule is held.
        // Gradle runs unit tests from the module directory.
        val lines = Regex("""<(?:string|item)\b[^>]*>([^<]*)</(?:string|item)>""")
            .findAll(summaryStrings())
            .map { it.groupValues[1] }
            .toList()
        assertEquals(15, lines.size)
        for (line in lines) {
            assertEquals(line, 1, line.count { it == '.' })
            assertFalse(line, line.contains("cycle", ignoreCase = true))
        }
    }

    /** The summary_ strings and plurals out of the English strings.xml, as raw XML. */
    private fun summaryStrings(): String =
        Regex("""<(string|plurals) name="summary_[^"]*"[^>]*>.*?</\1>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(File("src/main/res/values/strings.xml").readText())
            .joinToString("\n") { it.value }
}
