package com.simodev.gpspositionsaver

import android.content.Context
import android.location.Location
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.text.SimpleDateFormat
import java.util.*

data class Category(val id: Int, val name: String, val colorHex: String)

data class Place(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val note: String?,
    val categoryId: Int,
    val timestamp: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(location: Location?) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var places by remember { mutableStateOf(listOf<Place>()) }

    val categories = listOf(
        Category(1, "Home", "#4CAF50"),
        Category(2, "Work", "#1E88E5"),
        Category(3, "Outdoor", "#FB8C00"),
        Category(4, "Parking", "#8E24AA")
    )
    var selectedCategory by remember { mutableIntStateOf(1) }
    var noteInput by remember { mutableStateOf("") }

    val currentLat = location?.latitude ?: 0.0
    val currentLng = location?.longitude ?: 0.0
    val currentAcc = location?.accuracy ?: 0f
    val isGpsActive = location != null
    val lastUpdateStr = if (location != null) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(location.time))
    } else {
        "Waiting..."
    }

    val savePlace = {
        if (!isGpsActive) return@savePlace
        val newPlace = Place(
            id = System.currentTimeMillis(),
            name = "Waypoint (${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())})",
            latitude = currentLat,
            longitude = currentLng,
            accuracy = currentAcc,
            note = noteInput.ifBlank { null },
            categoryId = selectedCategory,
            timestamp = System.currentTimeMillis()
        )
        places = listOf(newPlace) + places
        noteInput = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("GPS Position Saver", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (isGpsActive) Color(0xFF4CAF50) else Color(0xFFF44336), shape = RoundedCornerShape(50))
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D47A1),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Place, contentDescription = "Save") },
                    label = { Text("Save") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Places") },
                    label = { Text("Places (${places.size})") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                    label = { Text("Map") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                0 -> SaveTab(
                    lat = currentLat,
                    lng = currentLng,
                    accuracy = currentAcc,
                    isGpsActive = isGpsActive,
                    lastUpdateStr = lastUpdateStr,
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it },
                    noteInput = noteInput,
                    onNoteChange = { noteInput = it },
                    onSave = savePlace
                )
                1 -> PlacesTab(places = places, categories = categories, onDelete = { id ->
                    places = places.filter { it.id != id }
                })
                2 -> MapTab(currentLat = currentLat, currentLng = currentLng)
                3 -> SettingsTab(placeCount = places.size)
            }
        }
    }
}

@Composable
fun SaveTab(
    lat: Double,
    lng: Double,
    accuracy: Float,
    isGpsActive: Boolean,
    lastUpdateStr: String,
    categories: List<Category>,
    selectedCategory: Int,
    onSelectCategory: (Int) -> Unit,
    noteInput: String,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FF))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val badgeColor = if (isGpsActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    val badgeTextColor = if (isGpsActive) Color(0xFF2E7D32) else Color(0xFFC62828)
                    val badgeText = if (isGpsActive) "GPS Active" else "Waiting for GPS"
                    Badge(containerColor = badgeColor) {
                        Text(badgeText, color = badgeTextColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp))
                    }
                    Text("±${String.format("%.1f", accuracy)}m", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF0D47A1))
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text("Lat: ${String.format("%.6f", lat)}", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Color.DarkGray)
                Text("Lng: ${String.format("%.6f", lng)}", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Color.DarkGray)
                Text("Last Fix: $lastUpdateStr", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Gray)
            }
        }

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Select Category", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectCategory(cat.id) },
                        label = { Text(cat.name, fontSize = 12.sp) }
                    )
                }
            }
        }

        OutlinedTextField(
            value = noteInput,
            onValueChange = onNoteChange,
            placeholder = { Text("Add optional note or landmark name...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
            enabled = isGpsActive
        ) {
            Icon(Icons.Default.Place, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Current Position", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun PlacesTab(places: List<Place>, categories: List<Category>, onDelete: (Long) -> Unit) {
    if (places.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No saved places yet. Tap Save!", color = Color.Gray, fontWeight = FontWeight.Medium)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FF))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(places) { place ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(place.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E293B))
                            Text(
                                "${place.latitude}, ${place.longitude}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            if (!place.note.isNullOrBlank()) {
                                Text(place.note!!, fontSize = 12.sp, color = Color.DarkGray)
                            }
                        }

                        IconButton(onClick = { onDelete(place.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MapTab(currentLat: Double, currentLng: Double) {
    val context = LocalContext.current
    remember {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(16.0)
                    controller.setCenter(GeoPoint(currentLat, currentLng))
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun SettingsTab(placeCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FF))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("App Storage & Privacy", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Saved Waypoints: $placeCount", fontSize = 12.sp, color = Color.Gray)
                Text("100% Pure Native Kotlin Jetpack Compose App", fontSize = 12.sp, color = Color(0xFF0D47A1))
            }
        }
    }
}
