package com.simodev.gpspositionsaver.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simodev.gpspositionsaver.data.model.AppSettings
import com.simodev.gpspositionsaver.data.model.Group
import com.simodev.gpspositionsaver.data.model.Place
import com.simodev.gpspositionsaver.geo.GeoUtils
import com.simodev.gpspositionsaver.io.ExportFormat
import com.simodev.gpspositionsaver.io.ImportParser
import com.simodev.gpspositionsaver.platform.FileTransfer
import com.simodev.gpspositionsaver.platform.IntentHelpers
import com.simodev.gpspositionsaver.ui.AppViewModel
import com.simodev.gpspositionsaver.ui.components.ConfirmDialog
import com.simodev.gpspositionsaver.ui.components.OsmMapView
import com.simodev.gpspositionsaver.ui.components.parseHexColor
import com.simodev.gpspositionsaver.ui.components.GroupChips
import com.simodev.gpspositionsaver.data.model.MapLayerType
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

private enum class SortOption(val label: String) {
    DISTANCE_ASC("Distance ↑"),
    DISTANCE_DESC("Distance ↓"),
    DATE_DESC("Newest"),
    DATE_ASC("Oldest"),
    NAME_ASC("Name A-Z"),
    NAME_DESC("Name Z-A"),
}

@Composable
fun PlacesScreen(
    vm: AppViewModel,
    places: List<Place>,
    groups: List<Group>,
    settings: AppSettings,
    currentLat: Double,
    currentLng: Double,
    onOpenPlace: (Place) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var groupFilter by remember { mutableStateOf<Long?>(null) }
    var sort by remember { mutableStateOf(SortOption.DATE_DESC) }
    var sortMenu by remember { mutableStateOf(false) }
    var overflowMenu by remember { mutableStateOf(false) }
    var showMap by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<ExportFormat?>(null) }
    var confirmDelete by remember { mutableStateOf<Place?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        val format = pendingExport
        if (uri != null && format != null) {
            scope.launch {
                val content = vm.buildExport(format)
                val ok = FileTransfer.writeText(context, uri, content)
                vm.showToast(if (ok) "Exported ${format.label}" else "Export failed")
            }
        }
        pendingExport = null
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = FileTransfer.readText(context, uri)
                if (text == null) {
                    vm.showToast("Could not read file")
                } else {
                    val result = ImportParser.parse(text)
                    if (result.places.isEmpty()) {
                        vm.showToast("No places found in file")
                    } else {
                        vm.importFrom(result)
                    }
                }
            }
        }
    }

    val filtered = remember(places, query, groupFilter, sort, currentLat, currentLng) {
        places
            .filter { groupFilter == null || it.groupId == groupFilter }
            .filter { p ->
                if (query.isBlank()) true
                else listOfNotNull(p.name, p.address, p.note).any {
                    it.contains(query, ignoreCase = true)
                }
            }
            .let { list ->
                when (sort) {
                    SortOption.DISTANCE_ASC -> list.sortedBy {
                        GeoUtils.calculateDistance(currentLat, currentLng, it.latitude, it.longitude)
                    }
                    SortOption.DISTANCE_DESC -> list.sortedByDescending {
                        GeoUtils.calculateDistance(currentLat, currentLng, it.latitude, it.longitude)
                    }
                    SortOption.DATE_DESC -> list.sortedByDescending { it.createdAt }
                    SortOption.DATE_ASC -> list.sortedBy { it.createdAt }
                    SortOption.NAME_ASC -> list.sortedBy { it.name.lowercase() }
                    SortOption.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search places...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(50),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { showMap = !showMap }) {
                Icon(
                    if (showMap) Icons.Default.Search else Icons.Default.Navigation,
                    contentDescription = "Toggle map",
                )
            }
            Box {
                IconButton(onClick = { overflowMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = overflowMenu, onDismissRequest = { overflowMenu = false }) {
                    ExportFormat.entries.forEach { format ->
                        DropdownMenuItem(
                            text = { Text("Export ${format.label}") },
                            onClick = {
                                overflowMenu = false
                                pendingExport = format
                                exportLauncher.launch(FileTransfer.defaultFileName(format))
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Import backup...") },
                        onClick = {
                            overflowMenu = false
                            importLauncher.launch(arrayOf("*/*"))
                        },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                TextButton(onClick = { sortMenu = true }) {
                    Text(sort.label)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = { sort = option; sortMenu = false },
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "${filtered.size} of ${places.size}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            GroupChips(groups = groups, selectedId = groupFilter, onSelect = { groupFilter = it })
        }

        if (showMap) {
            OsmMapView(
                currentLat = currentLat,
                currentLng = currentLng,
                currentAccuracy = 0.0,
                places = filtered,
                groups = groups,
                layerType = MapLayerType.STANDARD,
                onOpenPlace = onOpenPlace,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            )
        } else if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (places.isEmpty()) "No places yet. Save your first position!"
                    else "No places match your filters.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filtered, key = { it.id }) { place ->
                    PlaceCard(
                        place = place,
                        group = groups.firstOrNull { it.id == place.groupId },
                        settings = settings,
                        currentLat = currentLat,
                        currentLng = currentLng,
                        onOpen = { onOpenPlace(place) },
                        onToggleFavorite = { vm.toggleFavorite(place) },
                        onShare = {
                            IntentHelpers.shareText(
                                context,
                                GeoUtils.createShareText(place, settings.coordinateFormat),
                                place.name,
                            )
                        },
                        onNavigate = {
                            IntentHelpers.openMap(context, place.latitude, place.longitude, place.name)
                        },
                        onDelete = { confirmDelete = place },
                    )
                }
            }
        }
    }

    confirmDelete?.let { place ->
        ConfirmDialog(
            title = "Delete place?",
            message = "\"${place.name}\" will be permanently removed.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                vm.deletePlace(place.id)
                confirmDelete = null
                vm.showToast("Deleted")
            },
            onDismiss = { confirmDelete = null },
        )
    }
}

@Composable
private fun PlaceCard(
    place: Place,
    group: Group?,
    settings: AppSettings,
    currentLat: Double,
    currentLng: Double,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onNavigate: () -> Unit,
    onDelete: () -> Unit,
) {
    val distance = GeoUtils.calculateDistance(currentLat, currentLng, place.latitude, place.longitude)
    val bearing = GeoUtils.calculateBearing(currentLat, currentLng, place.latitude, place.longitude)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                group?.let {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(parseHexColor(it.colorHex), RoundedCornerShape(50)),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    place.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (place.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (place.isFavorite) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                GeoUtils.formatCoordinates(place.latitude, place.longitude, settings.coordinateFormat),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            place.address?.let {
                Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            place.note?.let { Text(it, fontSize = 12.sp) }

            Spacer(Modifier.height(4.dp))
            Text(
                "${GeoUtils.formatDistance(distance, settings.unitSystem)} away • " +
                    "${GeoUtils.bearingToCardinal(bearing)} (${bearing.toInt()}°)",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
            )

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onNavigate) {
                    Icon(Icons.Default.Navigation, contentDescription = "Navigate")
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
