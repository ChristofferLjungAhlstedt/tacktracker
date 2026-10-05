package com.blindsail.tacktracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.blindsail.tacktracker.R
import com.blindsail.tacktracker.logic.Confidence
import com.blindsail.tacktracker.sensors.HeadingSource
import com.blindsail.tacktracker.settings.MountOrientation

private const val MS_TO_KNOTS = 1.943844

private fun deg(value: Double?): String = value?.let { "%.0f°".format(it) } ?: "--"
private fun signedDeg(value: Double?): String = value?.let { "%+.0f°".format(it) } ?: "--"

/** Debug screen showing raw and fused heading. Every control is at least 64 dp tall. */
@Composable
fun DebugScreen(
    viewModel: DebugViewModel,
    hasLocationPermission: Boolean,
    onRequestPermission: () -> Unit,
) {
    val ui by viewModel.ui.collectAsState()
    val h = ui.heading

    Column(
        modifier = Modifier
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.debug_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )

        if (!hasLocationPermission) {
            Text(stringResource(R.string.permission_explain), style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
            ) { Text(stringResource(R.string.grant_permission)) }
        }
        if (ui.gpsError && hasLocationPermission) Text(stringResource(R.string.error_gps))
        if (ui.compassMissing) Text(stringResource(R.string.error_compass))

        val source = when (h?.source) {
            HeadingSource.COG -> stringResource(R.string.source_cog)
            HeadingSource.COMPASS -> stringResource(R.string.source_compass)
            else -> stringResource(R.string.source_none)
        }
        val confidence = when (h?.confidence) {
            Confidence.HIGH -> stringResource(R.string.confidence_high)
            Confidence.MEDIUM -> stringResource(R.string.confidence_medium)
            else -> stringResource(R.string.confidence_low)
        }
        val body = MaterialTheme.typography.titleLarge
        Text(stringResource(R.string.debug_fused, deg(h?.heading)), style = body)
        Text(stringResource(R.string.debug_source, source), style = body)
        Text(stringResource(R.string.debug_confidence, confidence), style = body)
        Text(stringResource(R.string.debug_cog, deg(h?.cog)), style = body)
        Text(
            stringResource(R.string.debug_speed, h?.let { "%.1f".format(it.speed * MS_TO_KNOTS) } ?: "--"),
            style = body,
        )
        Text(stringResource(R.string.debug_compass, deg(h?.compassAzimuth)), style = body)
        Text(stringResource(R.string.debug_offset, signedDeg(h?.compassOffset)), style = body)

        Text(
            stringResource(R.string.mounting_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        MountingButton(R.string.mounting_flat_bow, MountOrientation.FLAT_TOP_TO_BOW, ui.mounting, viewModel)
        MountingButton(R.string.mounting_upright, MountOrientation.PORTRAIT_UPRIGHT_FACING_FORWARD, ui.mounting, viewModel)
        MountingButton(R.string.mounting_flat_stern, MountOrientation.FLAT_TOP_TO_STERN, ui.mounting, viewModel)
    }
}

@Composable
private fun MountingButton(
    label: Int,
    option: MountOrientation,
    current: MountOrientation,
    viewModel: DebugViewModel,
) {
    val isSelected = option == current
    val text = stringResource(label)
    val shown = if (isSelected) "✓ $text" else text
    val modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 64.dp)
        .semantics { selected = isSelected }
    if (isSelected) {
        Button(onClick = { viewModel.setMounting(option) }, modifier = modifier) { Text(shown) }
    } else {
        OutlinedButton(onClick = { viewModel.setMounting(option) }, modifier = modifier) { Text(shown) }
    }
}
