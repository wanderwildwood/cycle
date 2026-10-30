package com.wanderwildwood.cycle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import com.mudita.mmd.components.text.TextMMD
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wanderwildwood.cycle.R
import com.wanderwildwood.cycle.cycle.Forecast
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

private val SpokenDate = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.getDefault())
private val MonthAndYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

/**
 * Your history, and what is expected next.
 *
 * Recorded and predicted are told apart by fill, never by shade: a day you marked is solid, a day
 * the app is guessing at is an outline. On a panel with no colour that distinction has to survive
 * being drawn in one ink, and an outline reading as "not yet true" is the one convention that
 * needs no key.
 *
 * Tapping a day opens it; see [DayScreen] for everything a day can hold.
 */
@Composable
fun CalendarScreen(
    month: YearMonth,
    today: LocalDate,
    marked: Set<LocalDate>,
    recorded: Set<LocalDate>,
    forecast: Forecast,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onThisMonth: () -> Unit,
    onBack: () -> Unit,
) {
    val predicted = predictedPeriod(forecast)
    // The window for this cycle (predicted, or placed by a positive test) and for earlier cycles
    // that had a positive test.
    val windows = listOfNotNull(forecast.fertile) + forecast.pastOvulations.map { it.fertile }
    val ovulations = listOfNotNull(forecast.ovulation) + forecast.pastOvulations.map { it.day }
    fun inWindow(d: LocalDate?) = d != null && windows.any { d in it }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Stepper("‹", stringResource(R.string.calendar_previous_month), onPreviousMonth)
            // Pressing the month's name comes back to this month, so the way home from a long
            // walk through the years is one press and not a closed screen.
            TextMMD(
                text = month.atDay(1).format(MonthAndYear),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.calendar_this_month),
                        onClick = onThisMonth,
                    )
                    .padding(top = 10.dp),
            )
            Stepper("›", stringResource(R.string.calendar_next_month), onNextMonth)
        }

        Spacer(Modifier.height(16.dp))

        val firstDay = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        val weekdays = (0..6).map { firstDay.plus(it.toLong()) }

        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach { day ->
                TextMMD(
                    text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        for (week in weeksOf(month, firstDay)) {
            Row(Modifier.fillMaxWidth()) {
                week.forEachIndexed { column, day ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (day == null) {
                            Spacer(Modifier.size(48.dp))
                        } else {
                            Day(
                                day = day,
                                isToday = day == today,
                                isMarked = day in marked,
                                isPredicted = predicted?.contains(day) == true,
                                hasData = day in recorded,
                                isFertile = inWindow(day),
                                // The box is open between neighbours in the same row, so a
                                // window reads as one box (one per row where it wraps).
                                edgeLeft = column == 0 || !inWindow(day.minusDays(1)),
                                edgeRight = column == 6 || !inWindow(day.plusDays(1)),
                                isOvulation = day in ovulations,
                                // Every day opens, including ones still to come — the rule that a
                                // day you have not lived yet cannot be marked as bleeding is
                                // enforced on the day screen, where it is the one that applies.
                                onClick = { onOpenDay(day) },
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Key()

        Spacer(Modifier.height(12.dp))

        TextMMD(
            text = stringResource(R.string.calendar_done),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .border(width = 1.dp, color = Color.Black)
                .clickable(role = Role.Button, onClick = onBack)
                .padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun Stepper(glyph: String, description: String, onClick: () -> Unit) {
    TextMMD(
        text = glyph,
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(top = 4.dp),
    )
}

@Composable
private fun Day(
    day: LocalDate,
    isToday: Boolean,
    isMarked: Boolean,
    isPredicted: Boolean,
    hasData: Boolean,
    isFertile: Boolean,
    edgeLeft: Boolean,
    edgeRight: Boolean,
    isOvulation: Boolean,
    onClick: (() -> Unit)?,
) {
    // Said in words as well as drawn: the date, then whatever is true of it.
    val states = buildList {
        if (isToday) add(stringResource(R.string.calendar_day_today))
        if (isMarked) add(stringResource(R.string.calendar_day_recorded))
        if (isPredicted) add(stringResource(R.string.calendar_day_expected))
        if (hasData) add(stringResource(R.string.calendar_day_data))
        if (isFertile) add(stringResource(R.string.calendar_day_fertile))
        if (isOvulation) add(stringResource(R.string.calendar_day_ovulation))
    }
    val spoken = (listOf(day.format(SpokenDate)) + states).joinToString(", ")
    Box(
        modifier = Modifier
            .size(48.dp)
            .semantics(mergeDescendants = true) { contentDescription = spoken }
            .then(if (isFertile) Modifier.dottedBox(edgeLeft, edgeRight) else Modifier)
            .then(if (isToday) Modifier.border(1.dp, Color.Black) else Modifier)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .then(
                    when {
                        isMarked -> Modifier.background(Color.Black, CircleShape)
                        isPredicted -> Modifier.border(1.dp, Color.Black, CircleShape)
                        else -> Modifier
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            TextMMD(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isMarked) Color.White else Color.Black,
                // Anything logged besides bleeding: the date is underlined.
                textDecoration = if (hasData) TextDecoration.Underline else null,
            )
        }

        // Ovulation is a dot under the date; the fertile window is the dotted box around its days.
        if (isOvulation) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp)
                    .size(5.dp)
                    .background(Color.Black, CircleShape),
            )
        }
    }
}

/**
 * A dotted outline: top and bottom always, and a side only where the window starts or ends in
 * this row. Drawn in the day's own 48 dp cell, so neighbours join into one box.
 */
private fun Modifier.dottedBox(left: Boolean, right: Boolean): Modifier = drawBehind {
    val stroke = 2.dp.toPx()
    val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))
    val inset = stroke / 2
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
        drawLine(Color.Black, Offset(x1, y1), Offset(x2, y2), strokeWidth = stroke, pathEffect = dash)
    line(0f, inset, size.width, inset)
    line(0f, size.height - inset, size.width, size.height - inset)
    if (left) line(inset, 0f, inset, size.height)
    if (right) line(size.width - inset, 0f, size.width - inset, size.height)
}

/**
 * A key, because several of these marks are not guessable.
 */
@Composable
private fun Key() {
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        KeyRow(filled = true) { TextMMD(stringResource(R.string.calendar_key_recorded), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal) }
        KeyRow(filled = false) { TextMMD(stringResource(R.string.calendar_key_expected), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextMMD("12", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal, textDecoration = TextDecoration.Underline, textAlign = TextAlign.Center, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            TextMMD(stringResource(R.string.calendar_key_data), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal)
        }
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 22.dp, height = 18.dp).dottedBox(left = true, right = true))
            Spacer(Modifier.size(6.dp))
            TextMMD(stringResource(R.string.calendar_key_fertile), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal)
            Spacer(Modifier.size(14.dp))
            Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(5.dp).background(Color.Black, CircleShape))
            }
            Spacer(Modifier.size(6.dp))
            TextMMD(stringResource(R.string.calendar_key_ovulation), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal)
        }
    }
}

@Composable
private fun KeyRow(filled: Boolean, label: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(18.dp)
                .then(
                    if (filled) Modifier.background(Color.Black, CircleShape)
                    else Modifier.border(1.dp, Color.Black, CircleShape)
                )
        )
        Spacer(Modifier.size(6.dp))
        label()
    }
    Spacer(Modifier.height(3.dp))
}

/** The days the next period is expected to run, or null while there is nothing to predict from. */
fun predictedPeriod(forecast: Forecast): ClosedRange<LocalDate>? {
    val start = forecast.nextStart ?: return null
    return start..start.plusDays((forecast.periodLength - 1).toLong())
}

/** The month laid out as weeks, padded with nulls so every row has seven cells. */
fun weeksOf(month: YearMonth, firstDayOfWeek: DayOfWeek): List<List<LocalDate?>> {
    val first = month.atDay(1)
    val lead = ((first.dayOfWeek.value - firstDayOfWeek.value) + 7) % 7
    val cells = mutableListOf<LocalDate?>()
    repeat(lead) { cells += null }
    for (d in 1..month.lengthOfMonth()) cells += month.atDay(d)
    while (cells.size % 7 != 0) cells += null
    return cells.chunked(7)
}
