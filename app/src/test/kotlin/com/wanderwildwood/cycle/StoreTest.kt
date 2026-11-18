package com.wanderwildwood.cycle

import com.wanderwildwood.cycle.data.Backup
import com.wanderwildwood.cycle.data.BleedingDay
import com.wanderwildwood.cycle.data.CycleDao
import com.wanderwildwood.cycle.data.DayNote
import com.wanderwildwood.cycle.data.Imported
import com.wanderwildwood.cycle.data.ImportedNote
import com.wanderwildwood.cycle.data.MAX_NOTE
import com.wanderwildwood.cycle.data.Store
import com.wanderwildwood.cycle.data.joinNotes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * What a restore or an import does to days already on the phone, against a database held in two
 * maps. Every date and note here is invented.
 */
class StoreTest {

    /** The parts of Room's behaviour the store leans on: REPLACE on insert, rows by day. */
    private class FakeDao : CycleDao {
        val bleeding = MutableStateFlow<Map<Long, BleedingDay>>(emptyMap())
        val notes = MutableStateFlow<Map<Long, DayNote>>(emptyMap())

        override fun bleedingDays(): Flow<List<BleedingDay>> = bleeding.map { it.values.sortedBy { d -> d.day } }
        override suspend fun mark(day: BleedingDay) { bleeding.value = bleeding.value + (day.day to day) }
        override suspend fun markAll(days: List<BleedingDay>) { bleeding.value = bleeding.value + days.associateBy { it.day } }
        override suspend fun count(): Int = bleeding.value.size
        override suspend fun allBleeding(): List<BleedingDay> = bleeding.value.values.sortedBy { it.day }
        override fun notes(): Flow<List<DayNote>> = notes.map { it.values.sortedBy { n -> n.day } }
        override suspend fun allNotes(): List<DayNote> = notes.value.values.sortedBy { it.day }
        override suspend fun unmark(day: Long) { bleeding.value = bleeding.value - day }
        override fun note(day: Long): Flow<DayNote?> = notes.map { it[day] }
        override suspend fun write(note: DayNote) { notes.value = notes.value + (note.day to note) }
        override suspend fun writeAll(notes: List<DayNote>) { this.notes.value = this.notes.value + notes.associateBy { it.day } }
        override suspend fun clearNote(day: Long) { notes.value = notes.value - day }
    }

    private val first = LocalDate.of(2026, 1, 5)
    private val day = first.toEpochDay()

    @Test fun `a restore keeps what a day already holds and adds the backup's note under it`() = runBlocking {
        val dao = FakeDao()
        dao.write(DayNote(day, moods = "Calm", note = "Typed on the new phone."))
        Store(dao).restore(
            Backup(
                days = listOf(BleedingDay(day, intensity = 3)),
                notes = listOf(
                    DayNote(day, moods = "Tired", symptoms = "Cramps", intimacy = true, note = "From the backup.", mucus = "watery"),
                ),
            )
        )
        val back = dao.notes.value.getValue(day)
        assertEquals("Calm\nTired", back.moods)
        assertEquals("Cramps", back.symptoms)
        assertTrue(back.intimacy)
        assertEquals("watery", back.mucus)
        assertEquals("Typed on the new phone.\nFrom the backup.", back.note)
        assertEquals(3, dao.bleeding.value.getValue(day).intensity)
    }

    @Test fun `a restore over a day with a test logged keeps the day's own result`() = runBlocking {
        val dao = FakeDao()
        dao.write(DayNote(day, ovulationTest = "negative"))
        Store(dao).restore(Backup(emptyList(), listOf(DayNote(day, ovulationTest = "positive"))))
        assertEquals("negative", dao.notes.value.getValue(day).ovulationTest)
    }

    @Test fun `a restore onto an empty phone writes the backup as it is`() = runBlocking {
        val dao = FakeDao()
        val note = DayNote(day, moods = "Calm", note = "Slept well.")
        Store(dao).restore(Backup(emptyList(), listOf(note)))
        assertEquals(note, dao.notes.value.getValue(day))
    }

    @Test fun `an imported note added to a long one stays within the limit, the day's own text first`() = runBlocking {
        val dao = FakeDao()
        val own = "a".repeat(MAX_NOTE - 10)
        dao.write(DayNote(day, note = own))
        Store(dao).take(Imported(emptyList(), listOf(ImportedNote(first, "b".repeat(50)))))
        val back = dao.notes.value.getValue(day).note
        assertEquals(MAX_NOTE, back.length)
        assertTrue(back.startsWith(own))
    }

    @Test fun `an imported note over the limit is cut to it`() = runBlocking {
        val dao = FakeDao()
        Store(dao).take(Imported(emptyList(), listOf(ImportedNote(first, "c".repeat(MAX_NOTE * 2)))))
        assertEquals(MAX_NOTE, dao.notes.value.getValue(day).note.length)
    }

    @Test fun `a note already at the limit is not cut by a join`() {
        val own = "a".repeat(MAX_NOTE + 5)
        assertEquals(own, joinNotes(own, "more"))
    }
}
