package com.wanderwildwood.cycle.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Everything the database holds, as one text file.
 *
 * This exists because the record has exactly one copy. The app cannot reach the network, the
 * manifest sets `allowBackup="false"`, and a release build is not debuggable — so short of this
 * file there is no way for years of records to survive a lost phone.
 *
 * The format is deliberately plain text rather than the database file or JSON. A backup that can
 * only be read by the app that wrote it is worth very little in the situation it exists for; this
 * one can be opened and understood on any machine, and read out by hand if it ever comes to that.
 *
 * It is not Period Tracker's export format. That format carries period starts and ends and nothing
 * else, so writing it would silently drop intensity, moods, symptoms and intimacy — every field
 * added since the import. A backup that quietly loses a quarter of the record is the wrong kind
 * of safe.
 */
data class Backup(val days: List<BleedingDay>, val notes: List<DayNote>)

private val FILE_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.US)
private val DAY = DateTimeFormatter.ISO_LOCAL_DATE

private const val HEADING = "Cycle Tracking backup"

/** What each stored intensity is called in the file, and back again. */
private val INTENSITY_WORDS = mapOf(1 to "light", 2 to "medium", 3 to "heavy")

private val DAY_LINE = Regex("""^\d{4}-\d{2}-\d{2}$""")

/** Continuation lines of a free note are indented by exactly this, and un-indented on the way in. */
private const val INDENT = "  "

/**
 * Write the whole record out, oldest day first.
 *
 * Days with nothing but a note are included: you opened that day and typed something, which is a
 * record even though no bleeding was marked.
 */
fun formatBackup(days: List<BleedingDay>, notes: List<DayNote>, made: LocalDate): String {
    val byDay = notes.filterNot { it.isEmpty() }.associateBy { it.day }
    val everyDay = (days.map { it.day } + byDay.keys).distinct().sorted()
    val bleeding = days.associateBy { it.day }

    val out = StringBuilder()
    out.append(HEADING).append('\n')
    out.append("Made ").append(made.format(FILE_DATE)).append('\n')
    out.append('\n')
    out.append("One block per day, oldest first. Plain text on purpose: this can be read\n")
    out.append("without the app, and read back into it.\n")

    for (epochDay in everyDay) {
        out.append('\n')
        out.append(LocalDate.ofEpochDay(epochDay).format(DAY)).append('\n')

        bleeding[epochDay]?.let { day ->
            out.append("bleeding ").append(INTENSITY_WORDS[day.intensity] ?: "medium").append('\n')
            // The one field that is a statement about what is *not* known: an imported start
            // whose end was never recorded. Without it a restore would read the day as a
            // one-day period and drag the period-length median down.
            if (day.lengthUnknown) out.append("length unknown\n")
        }

        byDay[epochDay]?.let { note ->
            tagsOf(note.moods).takeIf { it.isNotEmpty() }
                ?.let { out.append("moods ").append(it.joinToString(", ")).append('\n') }
            tagsOf(note.symptoms).takeIf { it.isNotEmpty() }
                ?.let { out.append("symptoms ").append(it.joinToString(", ")).append('\n') }
            if (note.intimacy) out.append("intimacy\n")
            if (note.note.isNotBlank()) {
                out.append("note\n")
                // Every line is indented, blank ones included, so that a blank line inside a note
                // cannot be mistaken for the blank line that separates two days.
                note.note.lines().forEach { out.append(INDENT).append(it).append('\n') }
            }
        }
    }

    return out.toString()
}

/**
 * Is this one of our own backups rather than a Period Tracker export?
 *
 * Sniffed rather than asked about, because at the point this matters you are restoring a lost phone
 * and should not have to know which of two file formats you are holding.
 */
fun looksLikeBackup(text: String): Boolean =
    text.lineSequence().firstOrNull()?.trim() == HEADING

/**
 * Read a backup back in.
 *
 * Unrecognised lines are skipped rather than thrown on, for the same reason the other parser skips
 * them: this file may be the only copy left, and refusing all of it over one bad line would be the
 * worst possible trade.
 */
fun parseBackup(text: String): Backup {
    val days = mutableListOf<BleedingDay>()
    val notes = mutableListOf<DayNote>()

    var day: LocalDate? = null
    var intensity: Int? = null
    var lengthUnknown = false
    var moods = ""
    var symptoms = ""
    var intimacy = false
    val note = mutableListOf<String>()
    var inNote = false

    fun flush() {
        val current = day ?: return
        intensity?.let {
            days += BleedingDay(current.toEpochDay(), it, lengthUnknown)
        }
        val free = note.joinToString("\n").trim('\n')
        val row = DayNote(current.toEpochDay(), moods, symptoms, intimacy, free)
        if (!row.isEmpty()) notes += row

        day = null
        intensity = null
        lengthUnknown = false
        moods = ""
        symptoms = ""
        intimacy = false
        note.clear()
        inNote = false
    }

    for (raw in text.lineSequence()) {
        // Inside a note, an indented line is content and belongs to it verbatim — including a line
        // that would otherwise look like a date or a keyword.
        if (inNote && raw.startsWith(INDENT)) {
            note += raw.removePrefix(INDENT)
            continue
        }

        val line = raw.trim()

        if (DAY_LINE.matches(line)) {
            flush()
            day = runCatching { LocalDate.parse(line, DAY) }.getOrNull()
            continue
        }

        if (day == null) continue    // heading and blurb, before the first day
        inNote = false

        when {
            line.startsWith("bleeding") -> {
                val word = line.removePrefix("bleeding").trim().lowercase()
                intensity = INTENSITY_WORDS.entries.firstOrNull { it.value == word }?.key ?: 2
            }
            line == "length unknown" -> lengthUnknown = true
            // Kept in the order the file lists them rather than run back through
            // [tagString], which with no vocabulary to order by would sort them
            // alphabetically and quietly rewrite what you picked.
            line.startsWith("moods") ->
                moods = splitTags(line.removePrefix("moods")).distinct().joinToString("\n")
            line.startsWith("symptoms") ->
                symptoms = splitTags(line.removePrefix("symptoms")).distinct().joinToString("\n")
            line == "intimacy" -> intimacy = true
            line == "note" -> inNote = true
        }
    }
    flush()

    return Backup(
        days = days.distinctBy { it.day }.sortedBy { it.day },
        notes = notes.distinctBy { it.day }.sortedBy { it.day },
    )
}

private fun splitTags(value: String): List<String> =
    value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
