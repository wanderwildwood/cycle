package com.wanderwildwood.cycle.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * One period as Period Tracker recorded it.
 *
 * [end] is null for the entries where no end was ever marked — an export carries a fair number of
 * them, and guessing a length would put days on the calendar that nobody said happened.
 */
data class ImportedPeriod(val start: LocalDate, val end: LocalDate?)

/** A dated line you typed, or a symptom the export recorded against a day. */
data class ImportedNote(val day: LocalDate, val text: String)

data class Imported(val periods: List<ImportedPeriod>, val notes: List<ImportedNote>)

private val START = Regex("""^Period Start\s+(.+)$""")
private val END = Regex("""^Period End\s+(.+)$""")
private val NOTE = Regex("""^(\d{1,2}/\d{1,2}/\d{2})\s+(.+)$""")

private val LONG_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
private val NOTE_DATE = DateTimeFormatter.ofPattern("M/d/yy", Locale.US)

/**
 * Read the "Period Tracker Notes" export out of GP Apps' Period Tracker.
 *
 * The format is plain text with tab-separated labels, newest entry first. The `Cycle Length`
 * field is deliberately ignored: it is the gap to the following period, which is already implied
 * by the start dates, and on the newest entry it is not a measurement at all but that app's own
 * prediction. Deriving it here means one source of truth instead of two that can disagree.
 *
 * Anything the parser does not recognise is skipped rather than thrown on. A four-year export is
 * the only copy of this history outside a proprietary app; refusing all of it over one odd line
 * would be the worst possible trade.
 */
fun parsePeriodTrackerExport(text: String): Imported {
    val periods = mutableListOf<ImportedPeriod>()
    val notes = mutableListOf<ImportedNote>()

    var start: LocalDate? = null

    for (raw in text.lineSequence()) {
        val line = raw.trim()

        START.find(line)?.let { m ->
            start?.let { periods += ImportedPeriod(it, null) }   // a start with no end line
            start = parseLongDate(m.groupValues[1])
            return@let
        }

        END.find(line)?.let { m ->
            val s = start
            if (s != null) {
                periods += ImportedPeriod(s, parseLongDate(m.groupValues[1]))
                start = null
            }
            return@let
        }

        NOTE.find(line)?.let { m ->
            val day = parseNoteDate(m.groupValues[1])
            if (day != null) notes += ImportedNote(day, m.groupValues[2].trim())
        }
    }
    start?.let { periods += ImportedPeriod(it, null) }

    return Imported(
        periods = periods.distinctBy { it.start }.sortedBy { it.start },
        notes = notes.distinctBy { it.day to it.text }.sortedBy { it.day },
    )
}

private fun parseLongDate(value: String): LocalDate? =
    runCatching { LocalDate.parse(value.trim(), LONG_DATE) }.getOrNull()

private fun parseNoteDate(value: String): LocalDate? =
    runCatching { LocalDate.parse(value.trim(), NOTE_DATE) }.getOrNull()

/** Days to mark, for the periods whose end was actually recorded. */
fun ImportedPeriod.days(): List<LocalDate> {
    val last = end ?: return listOf(start)
    return generateSequence(start) { if (it < last) it.plusDays(1) else null }.toList()
}
