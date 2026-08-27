package com.wanderwildwood.cycle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wanderwildwood.cycle.data.DayNote
import com.wanderwildwood.cycle.data.cleanTag
import com.wanderwildwood.cycle.data.tagString
import com.wanderwildwood.cycle.data.tagsOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DayHeading = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.getDefault())

/**
 * The words offered for a day.
 *
 * Short lists on purpose. A long one is a scroll on a panel that redraws slowly, and every word you
 * never pick is a word you have to read past to reach the ones you do. These are a starting set,
 * not a considered vocabulary — they were chosen without anyone who has to use them, and
 * [tagString] keeps whatever is already recorded even after the lists change, so they can be
 * replaced once someone has lived with them.
 */
val MOODS = listOf("Happy", "Calm", "Tired", "Low", "Anxious", "Irritable", "Energetic")

val SYMPTOMS = listOf(
    "Cramps", "Headache", "Backache", "Bloating", "Tender", "Nausea", "Acne", "Cravings", "Restless",
)

/** What the three intensity buttons say, in order; the stored value is the index plus one. */
private val INTENSITIES = listOf("Light", "Medium", "Heavy")

/**
 * One day, in full.
 *
 * Everything the database can hold about a day is on this one screen, because the alternative is
 * a second level of navigation on a phone where every screen change costs a full panel redraw.
 *
 * Bleeding and its intensity are written the moment you tap them: they move the prediction, and
 * the calendar behind this screen should already be right when you go back. The moods, symptoms,
 * intimacy and note are held here and written once on the way out — one write instead of one per
 * keystroke, which matters on this hardware. [DisposableEffect] covers the ways out that are not
 * the button: the back gesture, or leaving the app from this screen.
 */
@Composable
fun DayScreen(
    day: LocalDate,
    today: LocalDate,
    isBleeding: Boolean,
    intensity: Int,
    stored: DayNote?,
    onSetBleeding: (Boolean) -> Unit,
    onSetIntensity: (Int) -> Unit,
    onSave: (DayNote) -> Unit,
    onBack: () -> Unit,
) {
    // Seeded per day: moving to another day rebuilds the state rather than carrying yours over.
    var moods by remember(day, stored) { mutableStateOf(tagsOf(stored?.moods.orEmpty()).toSet()) }
    var symptoms by remember(day, stored) {
        mutableStateOf(tagsOf(stored?.symptoms.orEmpty()).toSet())
    }
    var intimacy by remember(day, stored) { mutableStateOf(stored?.intimacy ?: false) }
    var note by remember(day, stored) { mutableStateOf(stored?.note.orEmpty()) }

    val latest = rememberUpdatedState(
        DayNote(
            day = day.toEpochDay(),
            moods = tagString(moods, MOODS),
            symptoms = tagString(symptoms, SYMPTOMS),
            intimacy = intimacy,
            note = note,
        )
    )
    val save by rememberUpdatedState(onSave)
    DisposableEffect(day) { onDispose { save(latest.value) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(
            text = day.format(DayHeading),
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(14.dp))

        val scroll = rememberScrollState()

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scroll),
        ) {
            // A day that has not happened yet is not yours to record, the same rule the calendar
            // already applies. You can still write a note against it.
            if (day <= today) {
                Choice(
                    label = if (isBleeding) "Bleeding" else "Not bleeding",
                    chosen = isBleeding,
                    onClick = { onSetBleeding(!isBleeding) },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (isBleeding) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        INTENSITIES.forEachIndexed { index, name ->
                            Choice(
                                label = name,
                                chosen = intensity == index + 1,
                                onClick = { onSetIntensity(index + 1) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
            }

            Heading("Mood")
            Tags(
                vocabulary = MOODS,
                chosen = moods,
                onToggle = { tag -> moods = if (tag in moods) moods - tag else moods + tag },
                onAdd = { tag -> moods = moods + tag },
            )

            Spacer(Modifier.height(18.dp))

            Heading("Symptoms")
            Tags(
                vocabulary = SYMPTOMS,
                chosen = symptoms,
                onToggle = { tag -> symptoms = if (tag in symptoms) symptoms - tag else symptoms + tag },
                onAdd = { tag -> symptoms = symptoms + tag },
            )

            Spacer(Modifier.height(18.dp))

            Choice(
                label = "Intimacy",
                chosen = intimacy,
                onClick = { intimacy = !intimacy },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(18.dp))

            Heading("Note")
            BasicTextField(
                value = note,
                onValueChange = { note = it },
                textStyle = TextStyle(fontSize = 15.sp, color = Color.Black),
                cursorBrush = SolidColor(Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp)
                    .border(1.dp, Color.Black)
                    .padding(10.dp),
            )

            Spacer(Modifier.height(16.dp))
        }

        // The panel draws no scrollbar and no overscroll, so a fold that lands between two rows
        // of chips reads as the end of the screen — on a bleeding day it cut exactly below the
        // second symptom row, leaving intimacy and the whole note field undiscoverable, with any
        // imported notes among them. Driven off the scroll state rather than a spacing tweak
        // because it stays right whatever the vocabulary and whatever the day is carrying.
        if (scroll.canScrollForward) {
            Text(
                text = "\u25BE",
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
            )
        }

        Text(
            text = "Done",
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = Color.Black)
                .clickable(onClick = onBack)
                .padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun Heading(text: String) {
    Text(text = text, fontSize = 13.sp)
    Spacer(Modifier.height(6.dp))
}

/**
 * The offered words, then any you added yourself, then a way to add another.
 *
 * Words of your own are listed after the vocabulary rather than mixed into it, so the fixed list
 * stays in the order it was written and the ones you invented sit together where you left them.
 * They have to be drawn at all: [tagString] keeps a tag the vocabulary no longer contains, so a
 * word you typed and could not then see would be recorded against the day with no way to take it
 * off again.
 */
@Composable
private fun Tags(
    vocabulary: List<String>,
    chosen: Set<String>,
    onToggle: (String) -> Unit,
    onAdd: (String) -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }

    val ownWords = (chosen - vocabulary.toSet()).sorted()

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        vocabulary.forEach { tag ->
            Choice(label = tag, chosen = tag in chosen, onClick = { onToggle(tag) })
        }
        ownWords.forEach { tag ->
            Choice(label = tag, chosen = true, onClick = { onToggle(tag) })
        }
        if (!adding) {
            Choice(label = "Other\u2026", chosen = false, onClick = { adding = true })
        }
    }

    if (adding) {
        fun commit() {
            val tag = cleanTag(typed)
            if (tag.isNotEmpty()) onAdd(tag)
            typed = ""
            adding = false
        }

        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            BasicTextField(
                value = typed,
                onValueChange = { typed = it },
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = Color.Black),
                cursorBrush = SolidColor(Color.Black),
                // Enter commits, so the common case never needs the button beside it.
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commit() }),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .border(1.dp, Color.Black)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
            )
            Choice(label = "Add", chosen = false, onClick = { commit() })
        }
    }
}

/**
 * A word you have either picked or not.
 *
 * Picked is filled and unpicked is outlined, the same as the calendar: what is true is solid, what
 * is not is a line. The mark is a filled disc as well as the inversion, because on this panel a
 * black box with white text and a white box with black text can be told apart at a glance, but a
 * row of them at arm's length reads as texture — the disc is the part that survives that.
 */
@Composable
private fun Choice(
    label: String,
    chosen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = 44.dp)
            .border(1.dp, Color.Black)
            .then(if (chosen) Modifier.background(Color.Black) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .then(
                    if (chosen) Modifier.background(Color.White, CircleShape)
                    else Modifier.border(1.dp, Color.Black, CircleShape)
                ),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            color = if (chosen) Color.White else Color.Black,
        )
    }
}
