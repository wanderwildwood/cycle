package com.wanderwildwood.cycle.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.wanderwildwood.cycle.R

/**
 * Lato, bundled rather than asked for.
 *
 * The device's own default closes up at arm's length on a low-contrast panel; Lato's apertures
 * stay open. It ships with the app because this app never reaches the network.
 *
 * Two faces, because two are all the app draws: nothing sets italic and nothing sets bold, so the
 * other two were 142 KB of an APK that had just been cut to 2.19 MB. If a screen ever wants bold,
 * Compose synthesises it from the regular until a real face is put back.
 *
 * SIL Open Font License 1.1 — see LICENSES/OFL-1.1.txt.
 */
val Lato = FontFamily(
    Font(R.font.lato_regular, FontWeight.Normal),
    Font(R.font.lato_semibold, FontWeight.SemiBold),
)

/** The panel loses the thin end of every stroke, so regular reads grey and semibold reads black. */
val Reading = FontWeight.SemiBold

/** Material's scale, in Lato. Sizes are set at each call site, not taken from here. */
val CycleTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = Lato),
        displayMedium = displayMedium.copy(fontFamily = Lato),
        displaySmall = displaySmall.copy(fontFamily = Lato),
        headlineLarge = headlineLarge.copy(fontFamily = Lato),
        headlineMedium = headlineMedium.copy(fontFamily = Lato),
        headlineSmall = headlineSmall.copy(fontFamily = Lato),
        titleLarge = titleLarge.copy(fontFamily = Lato),
        titleMedium = titleMedium.copy(fontFamily = Lato),
        titleSmall = titleSmall.copy(fontFamily = Lato),
        bodyLarge = bodyLarge.copy(fontFamily = Lato),
        bodyMedium = bodyMedium.copy(fontFamily = Lato),
        bodySmall = bodySmall.copy(fontFamily = Lato),
        labelLarge = labelLarge.copy(fontFamily = Lato),
        labelMedium = labelMedium.copy(fontFamily = Lato),
        labelSmall = labelSmall.copy(fontFamily = Lato),
    )
}
