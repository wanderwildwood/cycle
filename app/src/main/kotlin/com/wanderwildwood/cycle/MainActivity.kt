package com.wanderwildwood.cycle

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.wanderwildwood.cycle.cycle.forecast
import com.wanderwildwood.cycle.cycle.summary
import com.wanderwildwood.cycle.data.CycleDatabase
import com.wanderwildwood.cycle.data.DayNote
import com.wanderwildwood.cycle.data.Store
import com.wanderwildwood.cycle.data.looksLikeBackup
import com.wanderwildwood.cycle.data.parseBackup
import com.wanderwildwood.cycle.data.parsePeriodTrackerExport
import com.wanderwildwood.cycle.ui.CalendarScreen
import com.wanderwildwood.cycle.ui.CycleTheme
import com.wanderwildwood.cycle.ui.DayScreen
import com.wanderwildwood.cycle.ui.TodayScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

/**
 * Writes that have to finish even though the screen asking for them is going away.
 *
 * A note is saved as the day screen leaves the composition, and backing out of the app disposes
 * that composition and cancels anything `rememberCoroutineScope` handed out in the same breath —
 * so a note typed and then backed straight out of would race the cancellation and usually lose.
 * This scope belongs to the process instead of to a screen. It is never cancelled because there is
 * nothing left to cancel it for: if the process goes, the write was never going to land anyway.
 */
private val writes = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CycleTheme {
                App()
            }
        }
    }
}

@Composable
private fun App() {
    val context = LocalContext.current
    val store = remember { Store(CycleDatabase.of(context).dao()) }
    val scope = rememberCoroutineScope()

    val days by store.days.collectAsState(initial = emptyList())
    val unknownLengths by store.unknownLengthStarts.collectAsState(initial = emptySet())
    val intensities by store.intensities.collectAsState(initial = emptyMap())

    var empty by remember { mutableStateOf(false) }
    LaunchedEffect(days.size) { empty = store.isEmpty() }

    // Reading a document you pick needs no permission of any kind: the picker hands back a URI
    // that is already granted. This is the whole reason the import works without a manifest line.
    val pick = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) scope.launch {
            withContext(Dispatchers.IO) {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
                // Either file you might be holding: one of our own backups, or a Period Tracker
                // export. Sniffed rather than asked about — at the point this
                // matters you are rebuilding a lost phone.
                if (text != null) {
                    if (looksLikeBackup(text)) store.restore(parseBackup(text))
                    else store.take(parsePeriodTrackerExport(text))
                }
            }
        }
    }

    // Writing a document you name needs no permission either, the same way reading one does
    // not. This runs on [writes] rather than the screen's scope: a backup cancelled halfway leaves
    // a truncated file that still looks like a backup, which is worse than not having written one.
    val save = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) writes.launch {
            val text = store.backup(LocalDate.now())
            context.contentResolver.openOutputStream(uri)
                ?.bufferedWriter()?.use { it.write(text) }
        }
    }

    // Read once per composition: the app is opened, read and closed.
    val today = LocalDate.now()
    val bleedingToday = today in days
    val outlook = forecast(days, today, unknownLengths)

    var showingCalendar by remember { mutableStateOf(false) }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var openDay by remember { mutableStateOf<LocalDate?>(null) }

    openDay?.let { day ->
        BackHandler { openDay = null }
        DayDetail(
            store = store,
            day = day,
            today = today,
            isBleeding = day in days,
            intensity = intensities[day] ?: DEFAULT_INTENSITY,
            onSetBleeding = { bleeding ->
                scope.launch { if (bleeding) store.mark(day) else store.unmark(day) }
            },
            onSetIntensity = { level -> scope.launch { store.mark(day, level) } },
            onSave = { note -> writes.launch { store.write(note) } },
            onBack = { openDay = null },
        )
        return
    }

    if (showingCalendar) {
        BackHandler { showingCalendar = false }
        CalendarScreen(
            month = month,
            today = today,
            marked = days.toSet(),
            forecast = outlook,
            onPreviousMonth = { month = month.minusMonths(1) },
            onNextMonth = { month = month.plusMonths(1) },
            // Tapping a day opens it rather than marking it. The one-tap mark lives on the today
            // screen, where it is the thing you came to do; on the calendar a stray tap while
            // paging months would otherwise write to your record with nothing to show it happened.
            onOpenDay = { day -> openDay = day },
            onBack = { showingCalendar = false },
        )
        return
    }

    TodayScreen(
        today = today,
        bleedingToday = bleedingToday,
        forecast = outlook,
        offerImport = empty,
        onMarkToday = { scope.launch { store.mark(today) } },
        onUnmarkToday = { scope.launch { store.unmark(today) } },
        onOpenCalendar = {
            month = YearMonth.from(today)
            showingCalendar = true
        },
        onSend = {
            // The app cannot reach the network. Whatever you pick here does the sending, over
            // whichever channel you already trust, and only if you pick something.
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, summary(today, bleedingToday, outlook))
            }
            context.startActivity(Intent.createChooser(send, null))
        },
        onImport = { pick.launch(arrayOf("text/plain", "text/*", "*/*")) },
        offerBackup = !empty,
        onBackUp = { save.launch("cycle-backup-$today.txt") },
    )
}

/** Marked without saying how heavy, which is most of them. */
private const val DEFAULT_INTENSITY = 2

/**
 * Wait for the day's note before drawing the screen.
 *
 * Room's flow emits once for a day with no note as well as one with, so this only blanks for the
 * length of a query. Handing [DayScreen] a null it would seed itself from and then replacing it a
 * frame later would clear anything you had already started typing.
 */
@Composable
private fun DayDetail(
    store: Store,
    day: LocalDate,
    today: LocalDate,
    isBleeding: Boolean,
    intensity: Int,
    onSetBleeding: (Boolean) -> Unit,
    onSetIntensity: (Int) -> Unit,
    onSave: (DayNote) -> Unit,
    onBack: () -> Unit,
) {
    val loaded by produceState<List<DayNote?>?>(initialValue = null, key1 = day) {
        store.note(day).collect { value = listOf(it) }
    }
    val held = loaded ?: return

    DayScreen(
        day = day,
        today = today,
        isBleeding = isBleeding,
        intensity = intensity,
        stored = held.first(),
        onSetBleeding = onSetBleeding,
        onSetIntensity = onSetIntensity,
        onSave = onSave,
        onBack = onBack,
    )
}
