package com.wanderwildwood.cycle.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import com.mudita.mmd.components.text.TextMMD
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wanderwildwood.cycle.BuildConfig
import com.wanderwildwood.cycle.R

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
    EInkDialog(onDismiss = onDismiss) {
        TextMMD(text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyLarge)

        Spacer(Modifier.height(14.dp))
        Line(stringResource(R.string.about_privacy))

        Spacer(Modifier.height(14.dp))
        Line(stringResource(R.string.about_licence))

        Spacer(Modifier.height(14.dp))
        Line("wanderthe.dev")

        Spacer(Modifier.height(18.dp))
        Llama()

        Spacer(Modifier.height(20.dp))
        TextMMD(
            text = stringResource(R.string.about_close),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Black)
                .clickable(onClick = onDismiss)
                .padding(vertical = 12.dp),
        )
    }
}

/**
 * A llama at the foot of the About, which opens the page a donation goes to.
 *
 * Three words rather than an address, because the address was more weight than a llama is
 * worth down here. They are a verb and an object, so what happens when you press them is not
 * a surprise even though the page they open is not named.
 *
 * The Kompakt may have nothing registered for a web address at all, so the intent is allowed
 * to fail quietly rather than take the dialog down with it.
 */
@Composable
private fun Llama() {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Straight to the checkout. The Donate button on the site only leads
                // here anyway, so the page in between is a press the reader does not need.
                // The short square.link form, not the long checkout.square.site address it
                // redirects to -- the short one is what the site itself links to, so a
                // regenerated checkout follows it and a published app does not break.
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(DONATE)),
                    )
                }.onFailure {
                    Toast.makeText(context, context.getString(R.string.about_no_browser), Toast.LENGTH_SHORT).show()
                }
            }
            .padding(vertical = 4.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.llama),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(6.dp))
        Line(stringResource(R.string.about_feed_llamas))
    }
}

private const val DONATE = "https://square.link/u/AGu8oT10"

@Composable
private fun Line(text: String, weight: FontWeight = FontWeight.Normal) {
    TextMMD(text = text, style = MaterialTheme.typography.labelSmall, fontWeight = weight)
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
