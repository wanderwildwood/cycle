package com.wanderwildwood.cycle

import com.wanderwildwood.cycle.data.cleanTag
import com.wanderwildwood.cycle.data.tagString
import com.wanderwildwood.cycle.data.tagsOf
import org.junit.Assert.assertEquals
import org.junit.Test

class TagsTest {

    private val vocabulary = listOf("Cramps", "Headache", "Bloating")

    @Test fun `an empty string holds no tags`() {
        assertEquals(emptyList<String>(), tagsOf(""))
        assertEquals(emptyList<String>(), tagsOf("\n\n"))
    }

    @Test fun `blank lines and stray whitespace are not tags`() {
        assertEquals(listOf("Cramps", "Headache"), tagsOf("  Cramps \n\n Headache\n"))
    }

    @Test fun `order follows the vocabulary, not the tapping`() {
        assertEquals("Cramps\nBloating", tagString(listOf("Bloating", "Cramps"), vocabulary))
    }

    @Test fun `a tag no longer in the list is kept`() {
        // The lists are provisional; a rewrite of them must not silently delete recorded history.
        assertEquals("Cramps\nDizziness", tagString(listOf("Dizziness", "Cramps"), vocabulary))
    }

    @Test fun `the round trip is stable`() {
        val chosen = listOf("Headache", "Cramps")
        assertEquals(chosen.sorted(), tagsOf(tagString(chosen, vocabulary)).sorted())
    }

    @Test fun `a typed tag keeps its word`() {
        assertEquals("Weepy", cleanTag("  Weepy  "))
        assertEquals("Sore hips", cleanTag("Sore hips"))
    }

    @Test fun `a comma in a typed tag cannot split it in the backup file`() {
        assertEquals("tired sore", cleanTag("tired, sore"))
    }

    @Test fun `a newline in a typed tag cannot split it in the database`() {
        assertEquals("tired sore", cleanTag("tired\nsore"))
    }

    @Test fun `whitespace only is not a tag`() {
        assertEquals("", cleanTag("   "))
        assertEquals("", cleanTag("\n , \n"))
    }

    @Test fun `a very long tag is cut to something a chip can hold`() {
        assertEquals(24, cleanTag("x".repeat(80)).length)
    }

    @Test fun `a typed tag survives being stored and read back`() {
        val stored = tagString(setOf("Cramps", "Sore hips"), vocabulary)
        assertEquals(listOf("Cramps", "Sore hips"), tagsOf(stored))
    }
}
