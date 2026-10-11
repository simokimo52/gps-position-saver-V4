package com.simodev.gpspositionsaver.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simodev.gpspositionsaver.data.model.AppSettings
import com.simodev.gpspositionsaver.data.model.MapLayerType
import com.simodev.gpspositionsaver.data.model.Track
import com.simodev.gpspositionsaver.data.model.TrackPoint
import com.simodev.gpspositionsaver.data.model.UnitSystem
import com.simodev.gpspositionsaver.ui.TrackRecorderState
import com.simodev.gpspositionsaver.geo.GeoUtils
import com.simodev.gpspositionsaver.platform.FileTransfer
import com.simodev.gpspositionsaver.ui.AppViewModel
import com.simodev.gpspositionsaver.ui.components.ConfirmDialog
import com.simodev.gpspositionsaver.ui.components.OsmMapView
import com.simodev.gpspositionsaver.ui.components.SectionCard
import kotlinx.coroutines.launch

@Composable
fun TrackScreen(
    vm: AppViewModel,
    settings: AppSettings,
    tracks: List<Track>,
    trackPoints: Map<Long, List<TrackPoint>>,
) {
    val recorder by vm.recorderState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showMap by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Track?>(null) }
    var pendingExport by remember { mutableStateOf<Long?>(null) }

    // Drives the live elapsed-time readout while recording (state only changes on new points).
    var nowTick by remember { mutableStateOf(0L) }
    LaunchedEffect(recorder.isRecording, recorder.isPaused) {
        while (recorder.isRecording && !recorder.isPaused) {
            nowTick = System.currentTimeMillis()
            kotlinx.coroutines.delay(500)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/gpx+xml")
    ) { uri ->
        val trackId = pendingExport
        if (uri != null && trackId != null) {
            scope.launch {
                val gpx = vm.trackGpx(trackId)
                if (gpx != null) {
                    FileTransfer.writeText(context, uri, gpx)
                    vm.showToast("GPX exported")
                } else {
                    vm.showToast("Nothing to export")
                }
            }
        }
        pendingExport = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Recorder card
            SectionCard {
                RecorderHeader(recorder)
                if (recorder.isRecording) {
                    RecorderStats(recorder, settings, nowTick)
                }
                Spacer(Modifier.height(12.dp))
                RecorderControls(
                    recorder = recorder,
                    onStart = vm::startTrackRecording,
                    onPause = vm::pauseTrackRecording,
                    onResume = vm::resumeTrackRecording,
                    onStop = vm::stopTrackRecording,
                    onCancel = vm::cancelTrackRecording,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Saved Tracks (${tracks.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { showMap = !showMap }) {
                    Text(if (showMap) "Hide map" else "Show map")
                }
            }

            if (showMap) {
                // Show saved tracks; overlay the in-progress recording as a live pseudo-track.
                val liveId = -1L
                val previewTracks = if (recorder.isRecording && recorder.points.isNotEmpty()) {
                    tracks + Track(
                        id = liveId,
                        name = "Recording…",
                        startTime = recorder.points.first().timestamp,
                        startLatitude = recorder.points.first().latitude,
                        startLongitude = recorder.points.first().longitude,
                    )
                } else {
                    tracks
                }
                val previewPoints = if (recorder.isRecording && recorder.points.isNotEmpty()) {
                    trackPoints + (liveId to recorder.points)
                } else {
                    trackPoints
                }
                MapPreview(
                    tracks = previewTracks,
                    trackPoints = previewPoints,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .padding(horizontal = 16.dp),
                )
            }

            if (tracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No tracks yet.\nPress Record below to capture your first route.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(tracks, key = { it.id }) { track ->
                        TrackCard(
                            track = track,
                            settings = settings,
                            pointCount = trackPoints[track.id]?.size ?: 0,
                            onDelete = { confirmDelete = track },
                            onRename = { vm.renameTrack(track, it) },
                            onExport = {
                                pendingExport = track.id
                                exportLauncher.launch("${track.name}.gpx")
                            },
                        )
                    }
                }
            }
        }
    }

    confirmDelete?.let { track ->
        ConfirmDialog(
            title = "Delete track?",
            message = "\"${track.name}\" and its points will be removed.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                vm.deleteTrack(track.id)
                confirmDelete = null
            },
            onDismiss = { confirmDelete = null },
        )
    }
}

@Composable
private fun RecorderHeader(recorder: TrackRecorderState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        if (recorder.isRecording && !recorder.isPaused) Color(0xFFE53935)
                        else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(50),
                    )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    recorder.isRecording && !recorder.isPaused -> "Recording"
                    recorder.isRecording && recorder.isPaused -> "Paused"
                    else -> "Idle"
                },
                fontWeight = FontWeight.Bold,
                color = if (recorder.isRecording) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            "${recorder.points.size} pts",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecorderStats(
    recorder: TrackRecorderState,
    settings: AppSettings,
    nowTick: Long,
) {
    // Reading nowTick makes Compose recompose the elapsed readout every tick.
    @Suppress("UNUSED_EXPRESSION")
    nowTick

    Spacer(Modifier.height(10.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        StatBlock("Time", formatElapsed(recorder.elapsedMs))
        StatBlock("Distance", GeoUtils.formatDistance(recorder.currentDistance, settings.unitSystem))
        StatBlock("Speed", formatSpeed(recorder.currentMaxSpeedMs, settings.unitSystem))
    }
}

@Composable
private fun StatBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun RecorderControls(
    recorder: TrackRecorderState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!recorder.isRecording) {
            Button(onClick = onStart, modifier = Modifier.weight(1f).height(48.dp)) {
                Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("Start Recording")
            }
        } else if (recorder.isPaused) {
            Button(onClick = onResume, modifier = Modifier.weight(1f).height(48.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Resume")
            }
            TextButton(onClick = onStop) { Text("Stop") }
        } else {
            Button(onClick = onStop, modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Stop & Save")
            }
            IconButton(onClick = onPause) {
                Icon(Icons.Default.Pause, contentDescription = "Pause")
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Delete, contentDescription = "Discard", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun TrackCard(
    track: Track,
    settings: AppSettings,
    pointCount: Int,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
    onExport: () -> Unit,
) {
    var renaming by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(track.name) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(track.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "${formatDateTime(track.startTime)} • $pointCount pts",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onExport) {
                    Icon(Icons.Default.Share, contentDescription = "Export GPX", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { onDelete() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatInline("Distance", GeoUtils.formatDistance(track.distanceMeters, settings.unitSystem))
                StatInline("Time", formatElapsed(track.durationMs))
                StatInline("Max speed", formatSpeed(track.maxSpeedMs, settings.unitSystem))
            }
            if (renaming) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { renaming = false }) { Text("Cancel") }
                    TextButton(onClick = { onRename(name); renaming = false }) { Text("Save") }
                }
            } else {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { renaming = true }) { Text("Rename") }
            }
        }
    }
}

@Composable
private fun StatInline(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun MapPreview(
    tracks: List<Track>,
    trackPoints: Map<Long, List<TrackPoint>>,
    modifier: Modifier = Modifier,
) {
    OsmMapView(
        currentLat = 0.0,
        currentLng = 0.0,
        currentAccuracy = 0.0,
        places = emptyList(),
        groups = emptyList(),
        layerType = MapLayerType.STANDARD,
        onOpenPlace = {},
        tracks = tracks,
        trackPoints = trackPoints,
        showTrackOverlay = true,
        fitRequest = if (tracks.isNotEmpty()) 1 else 0,
        modifier = modifier,
    )
}

private fun formatElapsed(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun formatSpeed(ms: Double?, unitSystem: UnitSystem): String {
    if (ms == null) return "—"
    return if (unitSystem == UnitSystem.IMPERIAL) {
        "%.1f mph".format(ms * 2.23694)
    } else {
        "%.1f km/h".format(ms * 3.6)
    }
}

private fun formatDateTime(epoch: Long): String {
    val fmt = java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.US)
    return fmt.format(java.util.Date(epoch))
}