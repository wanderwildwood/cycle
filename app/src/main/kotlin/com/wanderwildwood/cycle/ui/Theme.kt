package com.wanderwildwood.cycle.ui

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.text.TextStyle

/**
 * Black on white, and nothing else.
 *
 * The panel has sixteen greys and no colour. Everything here is one of two values; the calendar
 * distinguishes a period day from a fertile day by shape and fill, never by hue, which is also
 * why it stays legible to someone who could not tell pink from lilac in the first place.
 */
private val Monochrome = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    secondary = Color.Black,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color.White,
    onSurfaceVariant = Color.Black,
    outline = Color.Black,
    error = Color.Black,
    onError = Color.White,
)

/**
 * Touch feedback is drawn as nothing at all.
 *
 * A ripple is an animation: on e-ink it arrives as a grey smear that then has to be cleared, so
 * the feedback costs two full redraws and looks like a fault.
 */
private object NoIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = EmptyNode()
    override fun hashCode(): Int = -1
    override fun equals(other: Any?): Boolean = other === this

    private class EmptyNode : Modifier.Node()
}

@Composable
fun CycleTheme(content: @Composable () -> Unit) {
    // MaterialTheme leaves LocalTextStyle alone, so a bare Text falls back to the system face
    // unless it is set here too.
    CompositionLocalProvider(
        LocalIndication provides NoIndication,
        LocalTextStyle provides TextStyle(
            fontFamily = Lato,
            fontWeight = Reading,
            color = Color.Black,
        ),
    ) {
        MaterialTheme(colorScheme = Monochrome, typography = CycleTypography, content = content)
    }
}
