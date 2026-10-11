package com.simodev.gpspositionsaver.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.simodev.gpspositionsaver.data.model.Group
import com.simodev.gpspositionsaver.data.model.MapLayerType
import com.simodev.gpspositionsaver.data.model.Place
import com.simodev.gpspositionsaver.ui.AppViewModel
import com.simodev.gpspositionsaver.ui.components.OsmMapView

@Composable
fun MapScreen(
    vm: AppViewModel,
    currentLat: Double,
    currentLng: Double,
    currentAccuracy: Double,
    places: List<Place>,
    groups: List<Group>,
    onOpenPlace: (Place) -> Unit,
    tracks: List<com.simodev.gpspositionsaver.data.model.Track> = emptyList(),
    trackPoints: Map<Long, List<com.simodev.gpspositionsaver.data.model.TrackPoint>> = emptyMap(),
) {
    var layer by remember { mutableStateOf(MapLayerType.STANDARD) }
    var layerMenu by remember { mutableStateOf(false) }
    var fitRequest by remember { mutableIntStateOf(0) }
    var showTracks by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        OsmMapView(
            currentLat = currentLat,
            currentLng = currentLng,
            currentAccuracy = currentAccuracy,
            places = places,
            groups = groups,
            layerType = layer,
            onOpenPlace = onOpenPlace,
            fitRequest = fitRequest,
            tracks = if (showTracks) tracks else emptyList(),
            trackPoints = trackPoints,
            showTrackOverlay = showTracks,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Box {
                SmallFloatingActionButton(
                    onClick = { layerMenu = true },
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Map layer")
                }
                DropdownMenu(expanded = layerMenu, onDismissRequest = { layerMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Standard (OSM)") },
                        onClick = { layer = MapLayerType.STANDARD; layerMenu = false },
                        trailingIcon = {
                            if (layer == MapLayerType.STANDARD) Icon(Icons.Default.Check, null)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Satellite (Esri)") },
                        onClick = { layer = MapLayerType.SATELLITE; layerMenu = false },
                        trailingIcon = {
                            if (layer == MapLayerType.SATELLITE) Icon(Icons.Default.Check, null)
                        },
                    )
                }
            }

            Box(modifier = Modifier.padding(top = 12.dp)) {
                SmallFloatingActionButton(
                    onClick = { fitRequest++ },
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Fit markers")
                }
            }

            if (tracks.isNotEmpty()) {
                Box(modifier = Modifier.padding(top = 12.dp)) {
                    SmallFloatingActionButton(
                        onClick = { showTracks = !showTracks },
                        containerColor = if (showTracks) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                    ) {
                        Icon(
                            if (showTracks) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle tracks",
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = { vm.refreshLocation() },
                modifier = Modifier.padding(top = 12.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Center on GPS")
            }
        }

        if (places.isEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text("No places yet — save one to see it here", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
