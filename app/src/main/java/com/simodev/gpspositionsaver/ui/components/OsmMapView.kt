package com.simodev.gpspositionsaver.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.simodev.gpspositionsaver.data.model.Group
import com.simodev.gpspositionsaver.data.model.MapLayerType
import com.simodev.gpspositionsaver.data.model.Place
import com.simodev.gpspositionsaver.data.model.Track
import com.simodev.gpspositionsaver.data.model.TrackPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

private val SATELLITE_SOURCE = XYTileSource(
    "EsriWorldImagery", 0, 19, 256, ".jpg",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"),
    "© Esri World Imagery",
)

private fun parseColorInt(hex: String, fallback: Int): Int =
    runCatching { Color.parseColor(hex) }.getOrDefault(fallback)

private fun pinDrawable(color: Int): Drawable = GradientDrawable().apply {
    shape = GradientDrawable.OVAL
    setColor(color)
    setStroke(6, Color.WHITE)
    setSize(56, 56)
}

/** Compose wrapper around osmdroid's MapView: markers, layers, current position, tracks. */
@Composable
fun OsmMapView(
    currentLat: Double,
    currentLng: Double,
    currentAccuracy: Double,
    places: List<Place>,
    groups: List<Group>,
    layerType: MapLayerType,
    onOpenPlace: (Place) -> Unit,
    modifier: Modifier = Modifier,
    fitRequest: Int = 0,
    tracks: List<Track> = emptyList(),
    trackPoints: Map<Long, List<TrackPoint>> = emptyMap(),
    showTrackOverlay: Boolean = true,
) {
    val context = LocalContext.current

    remember {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE),
        )
        Configuration.getInstance().userAgentValue = context.packageName
        true
    }

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            setTileSource(TileSourceFactory.MAPNIK)
            controller.setZoom(16.0)
            controller.setCenter(GeoPoint(currentLat, currentLng))
        }
    }

    // Swap base tile layer when the layer type changes.
    LaunchedEffect(layerType) {
        mapView.setTileSource(
            if (layerType == MapLayerType.SATELLITE) SATELLITE_SOURCE else TileSourceFactory.MAPNIK
        )
        mapView.invalidate()
    }

    // Rebuild markers whenever the place set, groups, position, or track data changes.
    // Note: `trackPoints` may be a SnapshotStateMap mutated in place, so key on its
    // content-derived copy to guarantee the effect re-runs when points arrive.
    LaunchedEffect(
        places, groups, currentLat, currentLng, fitRequest, tracks, trackPoints.toMap(),
        showTrackOverlay,
    ) {
        mapView.overlays.clear()
        val defaultPin = Color.parseColor("#1E88E5")
        val groupColors = groups.associate { it.id to parseColorInt(it.colorHex, defaultPin) }

        places.forEach { place ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(place.latitude, place.longitude)
                title = place.name
                snippet = place.address ?: place.note
                icon = pinDrawable(groupColors[place.groupId] ?: defaultPin)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setInfoWindow(null)
                setOnMarkerClickListener { _, _ ->
                    onOpenPlace(place)
                    true
                }
            }
            mapView.overlays.add(marker)
        }

        if (currentLat != 0.0 || currentLng != 0.0) {
            mapView.overlays.add(
                Marker(mapView).apply {
                    position = GeoPoint(currentLat, currentLng)
                    title = "You are here"
                    icon = pinDrawable(Color.parseColor("#0D47A1"))
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setInfoWindow(null)
                }
            )
        }

        // Draw saved-track polylines (above places, below markers).
        if (showTrackOverlay) {
            tracks.forEach { track ->
                val pts = trackPoints[track.id]
                if (!pts.isNullOrEmpty()) {
                    mapView.overlays.add(
                        Polyline(mapView).apply {
                            setPoints(pts.map { GeoPoint(it.latitude, it.longitude) })
                            color = Color.parseColor("#E53935")
                            width = 6f
                        }
                    )
                }
            }
        }

        if (fitRequest > 0) {
            val markerPoints = places.map { GeoPoint(it.latitude, it.longitude) }.toMutableList()
            if (currentLat != 0.0 || currentLng != 0.0) markerPoints += GeoPoint(currentLat, currentLng)
            trackPoints.values.flatten().forEach { markerPoints += GeoPoint(it.latitude, it.longitude) }
            if (markerPoints.isNotEmpty()) {
                runCatching {
                    mapView.zoomToBoundingBox(
                        BoundingBox.fromGeoPoints(markerPoints),
                        true,
                        96,
                    )
                }
            }
        } else {
            mapView.controller.setCenter(GeoPoint(currentLat, currentLng))
        }
        mapView.invalidate()
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
