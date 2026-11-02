package com.wanderwildwood.cycle.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.cycle.R

/** The `i` that opens the About dialog (Cycle has no Settings top bar, so it sits on the Today screen). */
@Composable
fun AboutMark(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.about_label), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Info,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** Shown once before first use: Cycle keeps records and is not medical advice. */
@Composable
fun FirstRunNotice(onOk: () -> Unit) {
    EInkDialog(onDismiss = {}) {
        TextMMD(
            text = stringResource(R.string.about_medical_heading),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(4.dp))
        TextMMD(
            text = stringResource(R.string.about_medical_body),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Normal,
        )
        Spacer(Modifier.height(14.dp))
        HouseButton(
            label = stringResource(R.string.first_run_ok),
            onClick = onOk,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
