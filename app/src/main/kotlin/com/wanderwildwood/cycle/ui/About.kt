package com.wanderwildwood.cycle.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.wanderwildwood.cycle.BuildConfig

/**
 * What this is, what it does with what you tell it, and where the source lives.
 *
 * This app asks for no permissions and shows no settings, which means that without this there
 * was nowhere at all to read its version, its licence, or the fact that nothing it holds ever
 * leaves the phone. Those are the things somebody wants to know before trusting a record like
 * this one to a piece of software, and they were the things it did not say.
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // The stock dialog dims the window behind it. On the panel that is not a shadow but a
        // screenful of dithered grey, repainted on the way in and again on the way out.
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0f)
        }

        Surface(
            color = Color.White,
            contentColor = Color.Black,
            border = BorderStroke(1.dp, Color.Black),
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Text(text = "Cycle", fontSize = 20.sp)
                Spacer(Modifier.height(2.dp))
                Line("Version ${BuildConfig.VERSION_NAME}")

                Spacer(Modifier.height(14.dp))
                Line(
                    "A period tracker. You mark the days you bleed; it counts, and tells you " +
                        "roughly when the next one is due."
                )

                Spacer(Modifier.height(14.dp))
                Line(
                    "It asks for no permissions and cannot reach the network. What you record " +
                        "stays on this phone, and is sent nowhere — not to a server, not to " +
                        "anyone. Backing up and sending a summary both go through a file or an " +
                        "app you pick yourself, and only when you pick one."
                )

                Spacer(Modifier.height(14.dp))
                Line("Free software under the GNU General Public License v3.")

                Spacer(Modifier.height(14.dp))
                Line("Source, and your rights to it:")
                Line("github.com/wanderwildwood/cycle", weight = FontWeight.Medium)

                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Close",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.Black)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun Line(text: String, weight: FontWeight = FontWeight.Normal) {
    Text(text = text, fontSize = 14.sp, fontWeight = weight)
}

/**
 * The way in: a small `i` where every other app of mine puts it.
 *
 * Drawn rather than shipped as an asset, because it is two circles and a line and a drawable
 * would be a file to keep in step with the ink colour.
 */
@Composable
fun AboutMark(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val stroke = 1.5.dp.toPx()
            val radius = size.minDimension / 2f - stroke / 2f
            val middle = Offset(size.width / 2f, size.height / 2f)
            drawCircle(Color.Black, radius = radius, center = middle, style = Stroke(stroke))
            drawCircle(Color.Black, radius = stroke * 0.75f, center = Offset(middle.x, size.height * 0.28f))
            drawLine(
                Color.Black,
                Offset(middle.x, size.height * 0.45f),
                Offset(middle.x, size.height * 0.74f),
                stroke,
            )
        }
    }
}
