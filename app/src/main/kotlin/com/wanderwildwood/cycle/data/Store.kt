package com.wanderwildwood.cycle.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * A day marked as bleeding.
 *
 * Dates are stored as epoch days rather than text: it sorts and subtracts as an integer, and it
 * carries no timezone to be wrong about later.
 */
@Entity(tableName = "bleeding")
data class BleedingDay(
    @PrimaryKey val day: Long,
    /** 1 light, 2 medium, 3 heavy. Marked without a level is 2. */
    @ColumnInfo(name = "intensity") val intensity: Int = 2,
    /**
     * Set on the first day of an imported period whose end was never recorded.
     *
     * The day is real - it was marked as a period start. What is not known is how long it ran,
     * so this day is one stored day standing for an unknown number, and the period-length average
     * has to leave it out rather than read it as a one-day period.
     */
    @ColumnInfo(name = "length_unknown", defaultValue = "0") val lengthUnknown: Boolean = false,
)

/**
 * Whatever else was recorded about a day.
 *
 * Moods and symptoms are stored as newline-separated strings rather than their own tables. There
 * is one user and no querying across them; a join table would be structure for its own sake.
 */
@Entity(tableName = "notes")
data class DayNote(
    @PrimaryKey val day: Long,
    @ColumnInfo(name = "moods") val moods: String = "",
    @ColumnInfo(name = "symptoms") val symptoms: String = "",
    @ColumnInfo(name = "intimacy") val intimacy: Boolean = false,
    @ColumnInfo(name = "note") val note: String = "",
) {
    /** Nothing recorded but the date, which is not worth a row. */
    fun isEmpty(): Boolean =
        moods.isBlank() && symptoms.isBlank() && !intimacy && note.isBlank()
}

@Dao
interface CycleDao {

    @Query("SELECT * FROM bleeding ORDER BY day")
    fun bleedingDays(): Flow<List<BleedingDay>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun mark(day: BleedingDay)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markAll(days: List<BleedingDay>)

    @Query("SELECT COUNT(*) FROM bleeding")
    suspend fun count(): Int

    /**
     * A snapshot rather than the flow above: a backup is a single read of everything as it stands,
     * and collecting a flow to take one value from it would be a subscription with nothing to
     * subscribe to.
     */
    @Query("SELECT * FROM bleeding ORDER BY day")
    suspend fun allBleeding(): List<BleedingDay>

    @Query("SELECT * FROM notes ORDER BY day")
    suspend fun allNotes(): List<DayNote>

    @Query("DELETE FROM bleeding WHERE day = :day")
    suspend fun unmark(day: Long)

    @Query("SELECT * FROM notes WHERE day = :day")
    fun note(day: Long): Flow<DayNote?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun write(note: DayNote)

    @Query("DELETE FROM notes WHERE day = :day")
    suspend fun clearNote(day: Long)
}

@Database(entities = [BleedingDay::class, DayNote::class], version = 1, exportSchema = false)
abstract class CycleDatabase : RoomDatabase() {
    abstract fun dao(): CycleDao

    companion object {
        @Volatile private var instance: CycleDatabase? = null

        fun of(context: Context): CycleDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                CycleDatabase::class.java,
                "cycle.db",
            ).build().also { instance = it }
        }
    }
}

/**
 * The rest of the app asks for dates, not rows.
 */
class Store(private val dao: CycleDao) {

    val days: Flow<List<LocalDate>> =
        dao.bleedingDays().map { rows -> rows.map { LocalDate.ofEpochDay(it.day) } }

    /** How heavy each recorded day was, for the day screen to show the right button as chosen. */
    val intensities: Flow<Map<LocalDate, Int>> =
        dao.bleedingDays().map { rows -> rows.associate { LocalDate.ofEpochDay(it.day) to it.intensity } }

    /** The starts whose length was never recorded, for [com.wanderwildwood.cycle.cycle.forecast]. */
    val unknownLengthStarts: Flow<Set<LocalDate>> =
        dao.bleedingDays().map { rows ->
            rows.filter { it.lengthUnknown }.map { LocalDate.ofEpochDay(it.day) }.toSet()
        }

    suspend fun isEmpty(): Boolean = dao.count() == 0

    suspend fun mark(day: LocalDate, intensity: Int = 2) =
        dao.mark(BleedingDay(day.toEpochDay(), intensity))

    suspend fun unmark(day: LocalDate) = dao.unmark(day.toEpochDay())

    fun note(day: LocalDate): Flow<DayNote?> = dao.note(day.toEpochDay())

    /**
     * Write a day's note, or remove the row once nothing is left on it.
     *
     * Leaving an empty row behind would mean a day you opened and changed nothing on is stored
     * exactly like a day you deliberately emptied, and the day screen is opened by a single tap on
     * the calendar — so most days it is opened, it is only being read.
     */
    suspend fun write(note: DayNote) =
        if (note.isEmpty()) dao.clearNote(note.day) else dao.write(note)

    /**
     * The whole record as one text file, ready to be written wherever you point it.
     */
    suspend fun backup(made: LocalDate): String =
        formatBackup(dao.allBleeding(), dao.allNotes(), made)

    /**
     * Take back a file this app wrote.
     *
     * Unlike [take] this restores intensity, moods, symptoms and intimacy as well as the days, so a
     * phone rebuilt from a backup is the phone that was lost rather than an outline of it.
     */
    suspend fun restore(backup: Backup) {
        dao.markAll(backup.days)
        backup.notes.forEach { dao.write(it) }
    }

    /**
     * Take on a parsed export.
     *
     * Existing days are replaced rather than merged: an import is your history arriving, and two
     * versions of the same day would only differ by which one was written last anyway.
     */
    suspend fun take(imported: Imported) {
        val marks = imported.periods.flatMap { period ->
            period.days().map { day ->
                BleedingDay(
                    day = day.toEpochDay(),
                    lengthUnknown = period.end == null,
                )
            }
        }
        dao.markAll(marks)
        imported.notes
            .groupBy { it.day }
            .forEach { (day, lines) ->
                dao.write(DayNote(day = day.toEpochDay(), note = lines.joinToString("\n") { it.text }))
            }
    }
}
