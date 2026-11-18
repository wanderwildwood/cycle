package com.wanderwildwood.cycle.data

/**
 * How a day arriving from a file meets a day already on the phone.
 *
 * Import and restore are offered before any period day is marked, but a mood or a note may
 * already be there: typed on a new phone in the days before the backup was found. What is on the
 * phone stays as it is, and the file only adds to it.
 */

/**
 * [added] written under [old], the existing text first, within [MAX_NOTE].
 *
 * A note already at the limit is left as it is rather than cut: the day screen would otherwise
 * drop its end at the first keystroke, with nothing to say so.
 */
fun joinNotes(old: String, added: String): String = when {
    added.isBlank() || old.contains(added) -> old
    old.isBlank() -> capped(added, MAX_NOTE)
    old.length >= MAX_NOTE -> old
    else -> capped(old + "\n" + added, MAX_NOTE)
}

/** Both lists of tags, the existing ones first, each once. */
private fun joinTags(old: String, added: String): String =
    (tagsOf(old) + tagsOf(added)).distinct().joinToString("\n")

/**
 * [incoming] laid over [old]: anything [old] holds is kept, a field it left blank is filled in,
 * tags are added to its own, and the note goes under its note.
 */
fun mergeDay(old: DayNote?, incoming: DayNote): DayNote {
    if (old == null) return incoming.copy(note = capped(incoming.note, MAX_NOTE))
    return old.copy(
        moods = joinTags(old.moods, incoming.moods),
        symptoms = joinTags(old.symptoms, incoming.symptoms),
        intimacy = old.intimacy || incoming.intimacy,
        note = joinNotes(old.note, incoming.note),
        mucus = old.mucus.ifBlank { incoming.mucus },
        ovulationTest = old.ovulationTest.ifBlank { incoming.ovulationTest },
    )
}
