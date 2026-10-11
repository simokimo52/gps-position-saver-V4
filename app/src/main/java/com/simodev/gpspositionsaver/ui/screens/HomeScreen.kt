package com.simodev.gpspositionsaver.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simodev.gpspositionsaver.data.model.AppSettings
import com.simodev.gpspositionsaver.data.model.Group
import com.simodev.gpspositionsaver.data.model.LocationState
import com.simodev.gpspositionsaver.data.model.Place
import com.simodev.gpspositionsaver.geo.GeoUtils
import com.simodev.gpspositionsaver.location.GeocoderClient
import com.simodev.gpspositionsaver.ui.AppViewModel
import com.simodev.gpspositionsaver.ui.components.GroupChips
import com.simodev.gpspositionsaver.ui.components.SectionCard
import com.simodev.gpspositionsaver.ui.theme.GpsSuccess
import com.simodev.gpspositionsaver.ui.theme.GpsSuccessBg
import com.simodev.gpspositionsaver.ui.theme.GpsWarning
import com.simodev.gpspositionsaver.ui.theme.GpsWarningBg

@Composable
fun HomeScreen(
    vm: AppViewModel,
    settings: AppSettings,
    groups: List<Group>,
    location: LocationState,
    isLocating: Boolean,
    locatingStatus: String,
    errorMessage: String?,
    hasPermission: Boolean,
    savedPlaces: List<Place>,
    onOpenPlace: (Place) -> Unit,
    onGoLive: () -> Unit,
) {
    val toast by vm.toast.collectAsStateWithLifecycle()

    var selectedGroupId by remember { mutableStateOf(settings.defaultGroupId) }
    var quickNote by remember { mutableStateOf("") }
    var resolvedAddress by remember { mutableStateOf<String?>(null) }
    var lastSaved by remember { mutableStateOf<Place?>(null) }
    var showManual by remember { mutableStateOf(false) }
    var minimized by remember { mutableStateOf(false) }

    LaunchedEffect(location.latitude, location.longitude, location.isSimulated) {
        if (!location.isSimulated) {
            resolvedAddress = GeocoderClient.reverseGeocode(location.latitude, location.longitude)
        }
    }

    val canSave = hasPermission && !location.isSimulated && location.latitude != 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Save Position", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Row {
                IconButton(onClick = { showManual = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Manual coordinates")
                }
                IconButton(onClick = { vm.refreshLocation() }) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Refresh GPS")
                }
            }
        }

        // GPS status card
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusBadge(
                    text = when {
                        isLocating -> "Locating..."
                        location.isSimulated -> "Waiting for GPS"
                        else -> "GPS Active"
                    },
                    active = !location.isSimulated && !isLocating,
                )
                if (!minimized) {
                    Text(
                        GeoUtils.formatAccuracy(location.accuracy, settings.unitSystem),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = { minimized = !minimized }) {
                    Icon(
                        if (minimized) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = "Toggle details",
                    )
                }
            }

            if (isLocating) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(locatingStatus, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (!minimized) {
                Spacer(Modifier.height(10.dp))
                MonoLine("Lat: ${"%.6f".format(location.latitude)}")
                MonoLine("Lng: ${"%.6f".format(location.longitude)}")
                MonoLine("Coords: ${GeoUtils.formatCoordinates(location.latitude, location.longitude, settings.coordinateFormat)}")
                location.altitude?.let { MonoLine("Altitude: ${"%.1f".format(it)} m") }
                resolvedAddress?.let { MonoLine("Address: $it") }
            }
        }

        if (errorMessage != null) {
            SectionCard {
                Text(
                    errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                )
            }
        }

        // Category picker
        if (groups.isNotEmpty()) {
            Text("Category", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            GroupChips(
                groups = groups,
                selectedId = selectedGroupId,
                onSelect = { selectedGroupId = it },
                includeAll = false,
            )
        }

        OutlinedTextField(
            value = quickNote,
            onValueChange = { quickNote = it },
            label = { Text("Note (optional)") },
            placeholder = { Text("Landmark, parking level, gate code...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        )

        Button(
            onClick = {
                vm.savePlace(
                    name = null,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    altitude = location.altitude,
                    address = resolvedAddress,
                    note = quickNote.ifBlank { null },
                    groupId = selectedGroupId,
                    onSaved = { id ->
                        lastSaved = Place(
                            id = id, name = "Saved Position", latitude = location.latitude,
                            longitude = location.longitude, accuracy = location.accuracy,
                            altitude = location.altitude, address = resolvedAddress,
                            note = quickNote.ifBlank { null }, groupId = selectedGroupId,
                            isFavorite = false, createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                        )
                        quickNote = ""
                        vm.showToast("Position saved")
                    },
                )
            },
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Default.Place, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Save Current Position", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        OutlinedButton(
            onClick = onGoLive,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Default.Radio, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Start Live Sharing")
        }

        // Most-recent save confirmation
        lastSaved?.let { saved ->
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GpsSuccess)
                    Spacer(Modifier.width(8.dp))
                    Text("Saved", fontWeight = FontWeight.Bold, color = GpsSuccess)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    GeoUtils.formatCoordinates(saved.latitude, saved.longitude, settings.coordinateFormat),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { onOpenPlace(saved) }) { Text("Add details (note, photo, phone)") }
            }
        }

        if (savedPlaces.isNotEmpty()) {
            Text(
                "${savedPlaces.size} place(s) saved",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showManual) {
        ManualCoordinateDialog(
            onDismiss = { showManual = false },
            onSubmit = { lat, lng, name ->
                vm.savePlace(
                    name = name,
                    latitude = lat,
                    longitude = lng,
                    accuracy = 0.0,
                    altitude = null,
                    address = null,
                    note = null,
                    groupId = selectedGroupId,
                )
                showManual = false
                vm.showToast("Manual position saved")
            },
        )
    }
}

@Composable
private fun StatusBadge(text: String, active: Boolean) {
    val bg = if (active) GpsSuccessBg else GpsWarningBg
    val fg = if (active) GpsSuccess else GpsWarning
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text, color = fg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun MonoLine(text: String) {
    Text(
        text,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 1.dp),
    )
}

@Composable
private fun ManualCoordinateDialog(
    onDismiss: () -> Unit,
    onSubmit: (lat: Double, lng: Double, name: String?) -> Unit,
) {
    var lat by remember { mutableStateOf("") }
    var lng by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    val parsedLat = lat.toDoubleOrNull()
    val parsedLng = lng.toDoubleOrNull()
    val valid = parsedLat != null && parsedLng != null &&
        parsedLat in -90.0..90.0 && parsedLng in -180.0..180.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Coordinates") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (optional)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = lat,
                    onValueChange = { lat = it },
                    label = { Text("Latitude (-90..90)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = lng,
                    onValueChange = { lng = it },
                    label = { Text("Longitude (-180..180)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = { onSubmit(parsedLat!!, parsedLng!!, name.ifBlank { null }) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
