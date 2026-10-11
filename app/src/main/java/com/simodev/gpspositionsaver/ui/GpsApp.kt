package com.simodev.gpspositionsaver.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simodev.gpspositionsaver.data.model.Place
import com.simodev.gpspositionsaver.data.model.ScreenTab
import com.simodev.gpspositionsaver.ui.screens.HomeScreen
import com.simodev.gpspositionsaver.ui.screens.LiveShareScreen
import com.simodev.gpspositionsaver.ui.screens.MapScreen
import com.simodev.gpspositionsaver.ui.screens.PlaceDetailDialog
import com.simodev.gpspositionsaver.ui.screens.PlacesScreen
import com.simodev.gpspositionsaver.ui.screens.SettingsScreen

private data class NavItem(val tab: ScreenTab, val label: String, val icon: ImageVector)

@Composable
fun GpsApp(vm: AppViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val location by vm.location.collectAsStateWithLifecycle()
    val hasPermission by vm.hasPermission.collectAsStateWithLifecycle()
    val isLocating by vm.isLocating.collectAsStateWithLifecycle()
    val locatingStatus by vm.locatingStatus.collectAsStateWithLifecycle()
    val errorMessage by vm.errorMessage.collectAsStateWithLifecycle()
    val toast by vm.toast.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(ScreenTab.HOME) }
    var selectedPlace by remember { mutableStateOf<Place?>(null) }
    val snackbar = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        vm.onPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }
    }

    LaunchedEffect(toast) {
        toast?.let {
            snackbar.showSnackbar(it)
            vm.clearToast()
        }
    }

    val items = listOf(
        NavItem(ScreenTab.HOME, "Save", Icons.Default.Bookmark),
        NavItem(ScreenTab.PLACES, "Places", Icons.Default.List),
        NavItem(ScreenTab.MAP, "Map", Icons.Default.Map),
        NavItem(ScreenTab.LIVE, "Live", Icons.Default.Radio),
        NavItem(ScreenTab.SETTINGS, "Settings", Icons.Default.Settings),
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item.tab,
                        onClick = { tab = item.tab },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                            )
                        },
                        label = {
                            val count = if (item.tab == ScreenTab.PLACES) " (${state.places.size})" else ""
                            Text("${item.label}$count")
                        },
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                ScreenTab.HOME -> HomeScreen(
                    vm = vm,
                    settings = state.settings,
                    groups = state.groups,
                    location = location,
                    isLocating = isLocating,
                    locatingStatus = locatingStatus,
                    errorMessage = errorMessage,
                    hasPermission = hasPermission,
                    savedPlaces = state.places,
                    onOpenPlace = { selectedPlace = it },
                    onGoLive = { tab = ScreenTab.LIVE },
                )

                ScreenTab.PLACES -> PlacesScreen(
                    vm = vm,
                    places = state.places,
                    groups = state.groups,
                    settings = state.settings,
                    currentLat = location.latitude,
                    currentLng = location.longitude,
                    onOpenPlace = { selectedPlace = it },
                )

                ScreenTab.MAP -> MapScreen(
                    vm = vm,
                    currentLat = location.latitude,
                    currentLng = location.longitude,
                    currentAccuracy = location.accuracy,
                    places = state.places,
                    groups = state.groups,
                    onOpenPlace = { selectedPlace = it },
                )

                ScreenTab.LIVE -> LiveShareScreen(
                    vm = vm,
                    location = location,
                    activeSession = state.activeSession,
                    sessions = state.sessions,
                )

                ScreenTab.SETTINGS -> SettingsScreen(
                    vm = vm,
                    settings = state.settings,
                    groups = state.groups,
                    placeCount = state.places.size,
                )
            }
        }
    }

    selectedPlace?.let { place ->
        val live = state.places.firstOrNull { it.id == place.id } ?: place
        PlaceDetailDialog(
            vm = vm,
            place = live,
            groups = state.groups,
            settings = state.settings,
            currentLat = location.latitude,
            currentLng = location.longitude,
            onClose = { selectedPlace = null },
        )
    }
}
