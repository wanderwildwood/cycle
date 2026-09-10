package com.wanderwildwood.cycle.ui

import androidx.compose.runtime.Composable
import com.mudita.mmd.ThemeMMD

/**
 * Black on white, from MMD.
 *
 * This used to be sixty lines: a monochrome colour scheme, an object to suppress the ripple,
 * and a typography that wrapped every Material style in a bundled copy of Lato. All three
 * were correct, and all three are what ThemeMMD already does — so what stood here was a
 * careful reimplementation of a library the other apps in this shop were already using.
 *
 * The font is the same font. MMD bundles Lato for the same reason this app did: the panel
 * loses the thin end of every stroke and the device's own face closes up at arm's length,
 * where Lato's apertures stay open. So the two ttf files this app carried have gone with the
 * rest of it, and the type now comes from the same place as every other app's.
 *
 * The one visible change is weight. This app set SemiBold; MMD sets Medium, which is what
 * Go, Birding, Music Box and the others have been reading at all along. Matching them is the
 * point.
 */
@Composable
fun CycleTheme(content: @Composable () -> Unit) = ThemeMMD(content = content)
