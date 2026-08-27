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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wanderwildwood.cycle.cycle.Forecast
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

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
    forecast: Forecast,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onBack: () -> Unit,
) {
    val predicted = predictedPeriod(forecast)
    val fertile = forecast.fertile

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
            Stepper("‹", onPreviousMonth)
            Text(
                text = month.atDay(1).format(MonthAndYear),
                fontSize = 19.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Stepper("›", onNextMonth)
        }

        Spacer(Modifier.height(16.dp))

        val firstDay = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        val weekdays = (0..6).map { firstDay.plus(it.toLong()) }

        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        for (week in weeksOf(month, firstDay)) {
            Row(Modifier.fillMaxWidth()) {
                for (day in week) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (day == null) {
                            Spacer(Modifier.size(44.dp))
                        } else {
                            Day(
                                day = day,
                                isToday = day == today,
                                isMarked = day in marked,
                                isPredicted = predicted?.contains(day) == true,
                                isFertile = fertile?.contains(day) == true,
                                isOvulation = day == forecast.ovulation,
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

        Text(
            text = "Today",
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
private fun Stepper(glyph: String, onClick: () -> Unit) {
    Text(
        text = glyph,
        fontSize = 26.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick)
            .padding(top = 4.dp),
    )
}

@Composable
private fun Day(
    day: LocalDate,
    isToday: Boolean,
    isMarked: Boolean,
    isPredicted: Boolean,
    isFertile: Boolean,
    isOvulation: Boolean,
    onClick: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .then(if (isToday) Modifier.border(1.dp, Color.Black) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
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
            Text(
                text = day.dayOfMonth.toString(),
                fontSize = 14.sp,
                color = if (isMarked) Color.White else Color.Black,
            )
        }

        // Fertile is a bar, ovulation is a dot.
        //
        // These were a hollow ring and a filled one to begin with, and at the size a mark under a
        // date can be, the ring's own outline closes it up: on the panel both drew as the same
        // black dot. Two marks that differ in shape survive being three pixels across; two that
        // differ only in fill do not.
        if (isFertile || isOvulation) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp)
                    .then(
                        if (isOvulation) Modifier.size(5.dp).background(Color.Black, CircleShape)
                        else Modifier.size(width = 13.dp, height = 2.dp).background(Color.Black)
                    ),
            )
        }
    }
}

/**
 * A key, because three of these marks are not guessable.
 */
@Composable
private fun Key() {
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        KeyRow(filled = true) { Text("Recorded", fontSize = 12.sp, fontWeight = FontWeight.Normal) }
        KeyRow(filled = false) { Text("Expected", fontSize = 12.sp, fontWeight = FontWeight.Normal) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 13.dp, height = 2.dp).background(Color.Black))
            }
            Spacer(Modifier.size(6.dp))
            Text("Fertile", fontSize = 12.sp, fontWeight = FontWeight.Normal)
            Spacer(Modifier.size(14.dp))
            Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(5.dp).background(Color.Black, CircleShape))
            }
            Spacer(Modifier.size(6.dp))
            Text("Ovulation", fontSize = 12.sp, fontWeight = FontWeight.Normal)
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
