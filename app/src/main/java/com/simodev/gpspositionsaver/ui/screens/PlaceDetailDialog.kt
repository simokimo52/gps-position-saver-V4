package com.simodev.gpspositionsaver.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.simodev.gpspositionsaver.data.model.AppSettings
import com.simodev.gpspositionsaver.data.model.Group
import com.simodev.gpspositionsaver.data.model.Place
import com.simodev.gpspositionsaver.geo.GeoUtils
import com.simodev.gpspositionsaver.platform.IntentHelpers
import com.simodev.gpspositionsaver.platform.PhotoStore
import com.simodev.gpspositionsaver.ui.AppViewModel
import com.simodev.gpspositionsaver.ui.components.ConfirmDialog
import com.simodev.gpspositionsaver.ui.components.GroupChips
import com.simodev.gpspositionsaver.ui.components.NewGroupDialog
import kotlinx.coroutines.launch

@Composable
fun PlaceDetailDialog(
    vm: AppViewModel,
    place: Place,
    groups: List<Group>,
    settings: AppSettings,
    currentLat: Double,
    currentLng: Double,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember(place.id) { mutableStateOf(place.name) }
    var address by remember(place.id) { mutableStateOf(place.address ?: "") }
    var note by remember(place.id) { mutableStateOf(place.note ?: "") }
    var phone by remember(place.id) { mutableStateOf(place.phone ?: "") }
    var groupId by remember(place.id) { mutableStateOf(place.groupId) }
    var photoUri by remember(place.id) { mutableStateOf(place.photoUri) }
    var isFavorite by remember(place.id) { mutableStateOf(place.isFavorite) }

    var showDelete by remember { mutableStateOf(false) }
    var showNewGroup by remember { mutableStateOf(false) }

    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val saved = PhotoStore.importFromUri(context, it)
            if (saved != null) photoUri = saved
        }
    }

    val distance = GeoUtils.calculateDistance(currentLat, currentLng, place.latitude, place.longitude)
    val bearing = GeoUtils.calculateBearing(currentLat, currentLng, place.latitude, place.longitude)

    fun persist() {
        vm.updatePlace(
            place.copy(
                name = name.ifBlank { "Saved Position" },
                address = address.ifBlank { null },
                note = note.ifBlank { null },
                phone = phone.ifBlank { null },
                groupId = groupId,
                photoUri = photoUri,
                isFavorite = isFavorite,
            )
        )
    }

    AlertDialog(
        onDismissRequest = {
            persist()
            onClose()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Place Details", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                IconButton(onClick = {
                    isFavorite = !isFavorite
                }) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else Color.Gray,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Coordinates + copy
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        GeoUtils.formatCoordinates(place.latitude, place.longitude, settings.coordinateFormat),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        IntentHelpers.copyToClipboard(
                            context, "coordinates",
                            GeoUtils.formatCoordinates(place.latitude, place.longitude, settings.coordinateFormat),
                        )
                        vm.showToast("Coordinates copied")
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                }

                Text(
                    "Accuracy ${GeoUtils.formatAccuracy(place.accuracy, settings.unitSystem)} • " +
                        "${GeoUtils.formatDistance(distance, settings.unitSystem)} • " +
                        "${GeoUtils.bearingToCardinal(bearing)} (${bearing.toInt()}°)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                place.altitude?.let {
                    Text("Altitude: ${"%.1f".format(it)} m", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    trailingIcon = {
                        IconButton(onClick = {
                            vm.refreshAddress(place) { resolved ->
                                if (resolved != null) {
                                    address = resolved
                                    vm.showToast("Address updated")
                                } else {
                                    vm.showToast("Could not resolve address")
                                }
                            }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Re-geocode")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    trailingIcon = {
                        if (phone.isNotBlank()) {
                            IconButton(onClick = { IntentHelpers.dial(context, phone) }) {
                                Icon(Icons.Default.Call, contentDescription = "Call")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (groups.isNotEmpty()) {
                    Text("Category", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    GroupChips(groups = groups, selectedId = groupId, onSelect = { groupId = it })
                    TextButton(onClick = { showNewGroup = true }) { Text("+ New category") }
                }

                // Photo
                if (photoUri != null) {
                    Box {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Place photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                        IconButton(
                            onClick = { photoUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50)),
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove photo", tint = Color.White)
                        }
                    }
                }
                OutlinedButton(
                    onClick = { photoLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (photoUri == null) "Add photo" else "Change photo")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                persist()
                vm.showToast("Saved")
                onClose()
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                IconButton(onClick = { showDelete = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
                IconButton(onClick = {
                    IntentHelpers.shareText(
                        context,
                        GeoUtils.createShareText(place, settings.coordinateFormat),
                        place.name,
                    )
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
                IconButton(onClick = {
                    IntentHelpers.openMap(context, place.latitude, place.longitude, place.name)
                }) {
                    Icon(Icons.Default.Navigation, contentDescription = "Navigate")
                }
            }
        },
    )

    if (showDelete) {
        ConfirmDialog(
            title = "Delete place?",
            message = "\"$name\" will be permanently removed.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                vm.deletePlace(place.id)
                showDelete = false
                onClose()
            },
            onDismiss = { showDelete = false },
        )
    }

    if (showNewGroup) {
        NewGroupDialog(
            onDismiss = { showNewGroup = false },
            onCreate = { groupName, colorHex ->
                vm.createGroup(groupName, colorHex) { newId -> groupId = newId }
                showNewGroup = false
            },
        )
    }
}
