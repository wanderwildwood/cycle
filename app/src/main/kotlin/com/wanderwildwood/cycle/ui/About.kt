package com.wanderwildwood.cycle.ui

import android.content.Intent
import android.content.res.Resources
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
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
 * What this is, what it does with what you tell it, the licence, and where the source lives.
 *
 * A full screen on a [PagedColumn] rather than a dialog: with the privacy and terms text, the
 * medical note and the licence it is far longer than a dialog can be, and it pages on whole rows
 * like every other list here. This app asks for no permissions and shows no settings, so without
 * this there was nowhere at all to read what it holds or what it promises.
 */
@Composable
fun AboutScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    BackHandler(onBack = onDismiss)
    val licence = remember { licenceParagraphs(context.resources) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        PagedColumn(modifier = Modifier.weight(1f)) {
            item {
                Column {
                    TextMMD(
                        text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(14.dp))
                }
            }
            section(R.string.about_privacy_heading, R.string.about_privacy_body)
            section(R.string.about_terms_heading, R.string.about_terms_body)
            section(R.string.about_medical_heading, R.string.about_medical_body)
            item {
                Column {
                    Heading(stringResource(R.string.about_open_source_heading))
                    Body(stringResource(R.string.about_open_source_body))
                }
            }
            listOf(
                R.string.about_oss_mmd,
                R.string.about_oss_androidx,
                R.string.about_oss_kotlin,
                R.string.about_oss_lato,
            ).forEach { item { Body("\u2022 " + stringResource(it)) } }
            item {
                Link(stringResource(R.string.about_source_line), stringResource(R.string.about_open_source), SOURCE)
            }
            item {
                Link("wanderthe.dev", stringResource(R.string.about_open_website), "https://wanderthe.dev")
            }
            item {
                Link(
                    stringResource(R.string.about_feed_llamas),
                    stringResource(R.string.about_open_donate),
                    DONATE,
                    llama = true,
                )
            }
            item { Spacer(Modifier.height(18.dp)) }
            item { Heading(stringResource(R.string.about_licence_heading)) }
            items(licence) { Body(it) }
        }

        Spacer(Modifier.height(8.dp))
        TextMMD(
            text = stringResource(R.string.about_close),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .border(1.dp, Color.Black)
                .clickable(role = Role.Button, onClick = onDismiss)
                .padding(vertical = 12.dp),
        )
    }
}

private fun LazyListScope.section(title: Int, body: Int) {
    // One row, so a page never ends on a heading whose words are on the next one.
    item {
        Column {
            Heading(stringResource(title))
            Body(stringResource(body))
            Spacer(Modifier.height(14.dp))
        }
    }
}

/** The licence text as paragraphs: hard-wrapped lines joined, blank lines kept as breaks. */
private fun licenceParagraphs(resources: Resources): List<String> =
    resources.openRawResource(R.raw.gpl_3_0).bufferedReader().use { it.readText() }
        .split(Regex("\n\\s*\n"))
        .map { block -> block.lines().joinToString(" ") { it.trim() }.trim() }
        .filter { it.isNotEmpty() }

/**
 * A line that opens a web address, at least 48dp tall.
 *
 * The Kompakt may have nothing registered for a web address at all, so the intent is allowed to
 * fail quietly rather than take the screen down with it.
 */
@Composable
private fun Link(text: String, description: String, url: String, llama: Boolean = false) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClickLabel = description) {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }.onFailure {
                    Toast.makeText(context, context.getString(R.string.about_no_browser), Toast.LENGTH_SHORT).show()
                }
            },
    ) {
        if (llama) {
            Image(
                painter = painterResource(R.drawable.llama),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        TextMMD(text = text, style = MaterialTheme.typography.bodySmall)
    }
}

private const val SOURCE = "https://github.com/wanderwildwood/cycle"
private const val DONATE = "https://square.link/u/AGu8oT10"

@Composable
private fun Heading(text: String) {
    TextMMD(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun Body(text: String) {
    TextMMD(text = text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal)
    Spacer(Modifier.height(6.dp))
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
            .size(48.dp)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.about_label), onClick = onClick),
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
