package com.wanderwildwood.cycle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.wanderwildwood.cycle.cycle.awaitingConfirmation
import com.wanderwildwood.cycle.cycle.dayOfPeriod
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DayAndMonth = DateTimeFormatter.ofPattern("EEEE d MMMM")

/**
 * Where you are today, and the one thing you came here to do.
 *
 * The count is the whole screen. Everything else is small and underneath it, because on the days
 * this app gets opened at all, the number is usually the entire question.
 */
@Composable
fun TodayScreen(
    today: LocalDate,
    bleedingToday: Boolean,
    forecast: Forecast,
    offerImport: Boolean,
    onMarkToday: () -> Unit,
    onUnmarkToday: () -> Unit,
    onOpenCalendar: () -> Unit,
    onSend: () -> Unit,
    onImport: () -> Unit,
    offerBackup: Boolean,
    onBackUp: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = today.format(DayAndMonth),
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
        )

        Spacer(Modifier.height(40.dp))

        // Mid-period, today not yet said either way. It changes what the count means and what
        // the button is for, so it is worked out once and both read it.
        val awaiting = awaitingConfirmation(forecast, today, bleedingToday)

        val headline = headline(today, bleedingToday, awaiting, forecast)
        Text(
            text = headline.first,
            fontSize = 44.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        headline.second?.let {
            Spacer(Modifier.height(8.dp))
            Text(text = it, fontSize = 17.sp, fontWeight = FontWeight.Normal)
        }

        Spacer(Modifier.height(40.dp))

        // Three states, one button.
        //
        // "Period started" is the wording the trackers people arrive from use, so it stays.
        // Once a period is running the same button becomes "Bleeding today", because every day
        // is confirmed by hand, and on day two "Period started" is asking you to start one you
        // have already started.
        //
        // The other way round is deliberately "Undo" and not "Period ended": it unmarks *today*,
        // and on day three "Period ended" would read as closing the period while actually deleting
        // a day. Nothing here ends a period. You stop confirming and it ends itself, which is why
        // there is no third button for it.
        Action(
            label = when {
                bleedingToday -> "Undo"
                awaiting -> "Bleeding today"
                else -> "Period started"
            },
            onClick = if (bleedingToday) onUnmarkToday else onMarkToday,
        )

        Spacer(Modifier.height(16.dp))

        Action(label = "Calendar", onClick = onOpenCalendar)

        Spacer(Modifier.height(16.dp))

        // Nothing worth sending until there is a date to send.
        if (forecast.nextStart != null || bleedingToday) {
            Action(label = "Send to\u2026", onClick = onSend)
        }

        // Weighted rather than a fixed gap: whatever is left over collapses here, so the
        // footer sits on the bottom of the panel instead of being pushed past it. A fixed 40dp
        // put "Back up…" at y=780 on a 757px content box — drawn nowhere, still tappable.
        Spacer(Modifier.weight(1f))

        // Offered while the app is empty and then never again. An import is something you do
        // once; a permanent button for it would be a permanent reminder of a finished job.
        if (offerImport) {
            Action(label = "Import history", onClick = onImport)
            Spacer(Modifier.height(24.dp))
        }

        // Quiet, and only worth saying once there is something behind it.
        if (!forecast.estimated) {
            Text(
                text = "Cycle ${forecast.cycleLength} days · period ${forecast.periodLength} days",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
            )
        }

        // The only way this record survives the phone it is on, so it has to be findable — but it
        // is a rare, deliberate act, not a thing to do today. Plain text rather than another
        // bordered box: a fourth one would crowd the count, which is what you came here for.
        if (offerBackup) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Back up\u2026",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .clickable(onClick = onBackUp)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}

/**
 * The count, and the line under it. The second half is null when there is nothing worth adding.
 */
private fun headline(
    today: LocalDate,
    bleedingToday: Boolean,
    awaiting: Boolean,
    forecast: Forecast,
): Pair<String, String?> {
    if (bleedingToday) {
        val day = dayOfPeriod(forecast, today) ?: 1
        return "Day $day" to "of your period"
    }

    // The countdown is measured from a period that has probably not ended, so during this window it
    // is wrong — it would tell you a period was weeks away on the second day of one. The count is
    // still worth showing; what it needs is the line underneath saying you have not confirmed it.
    if (awaiting) {
        val day = dayOfPeriod(forecast, today) ?: 1
        return "Day $day" to "not recorded yet"
    }

    val until = forecast.daysUntilNextStart ?: return "No history yet" to "Mark a day to begin"

    return when {
        until > 1 -> "$until days" to "until your period"
        until == 1 -> "Tomorrow" to "your period is due"
        until == 0 -> "Today" to "your period is due"
        until == -1 -> "1 day late" to null
        else -> "${-until} days late" to null
    }
}

/**
 * A box with a word in it.
 *
 * Material's Button draws a filled shape with elevation, which on the panel is a black slab that
 * has to be redrawn whenever anything near it moves. An outline says the same thing for one line
 * of ink.
 */
@Composable
private fun Action(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 17.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Color.Black)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    )
}
