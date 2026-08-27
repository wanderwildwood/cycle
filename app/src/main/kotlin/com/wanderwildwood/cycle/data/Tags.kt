package com.wanderwildwood.cycle.data

/**
 * Moods and symptoms are stored as one newline-separated string per day.
 *
 * These two functions are the whole of that format. Keeping them here rather than inline in the
 * screen means the round trip can be tested without a device, and means an imported note that
 * arrived with stray whitespace or a trailing newline reads back as the same set you picked.
 */

/** The tags in a stored string, in the order they were written, with blanks dropped. */
fun tagsOf(stored: String): List<String> =
    stored.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

/**
 * Tags back into a stored string.
 *
 * [vocabulary] fixes the order, so the string does not churn just because you tapped things in a
 * different order. Anything not in the vocabulary — a tag from an older version of the list — is
 * kept and written after the rest, because dropping something you recorded is not this function's
 * decision to make.
 */
fun tagString(chosen: Collection<String>, vocabulary: List<String> = emptyList()): String {
    val set = chosen.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    val known = vocabulary.filter { it in set }
    val unknown = set.filterNot { it in vocabulary }.sorted()
    return (known + unknown).joinToString("\n")
}

/** As long a tag as a chip can carry on the panel without wrapping the row into nonsense. */
const val MAX_TAG = 24

/**
 * A word you typed, reduced to something both storage formats can carry.
 *
 * Newlines separate tags in the database and commas separate them in the backup file, so a tag
 * holding either would come back as two tags, or as one that had swallowed the next. Both are
 * turned into spaces rather than refused: you are naming how you feel, and the app rejecting your
 * word over a punctuation mark it happens to use internally would be the app's problem, not yours.
 *
 * Returns an empty string for anything that was only whitespace, which the caller drops.
 */
fun cleanTag(raw: String): String =
    raw.replace('\n', ' ')
        .replace('\r', ' ')
        .replace(',', ' ')
        .trim()
        .replace(Regex("\\s+"), " ")
        .take(MAX_TAG)
