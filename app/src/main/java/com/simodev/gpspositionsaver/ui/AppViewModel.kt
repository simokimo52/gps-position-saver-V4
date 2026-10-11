package com.simodev.gpspositionsaver.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simodev.gpspositionsaver.GpsSaverApp
import com.simodev.gpspositionsaver.data.model.AppSettings
import com.simodev.gpspositionsaver.data.model.CoordinateFormat
import com.simodev.gpspositionsaver.data.model.Group
import com.simodev.gpspositionsaver.data.model.LocationState
import com.simodev.gpspositionsaver.data.model.Place
import com.simodev.gpspositionsaver.data.model.ShareSession
import com.simodev.gpspositionsaver.data.model.Track
import com.simodev.gpspositionsaver.data.model.TrackPoint
import com.simodev.gpspositionsaver.data.model.UnitSystem
import com.simodev.gpspositionsaver.geo.GeoUtils
import com.simodev.gpspositionsaver.geo.TrackComputer
import com.simodev.gpspositionsaver.io.ExportCodecs
import com.simodev.gpspositionsaver.io.ExportFormat
import com.simodev.gpspositionsaver.io.ImportResult
import com.simodev.gpspositionsaver.location.GeocoderClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

data class AppUiState(
    val places: List<Place> = emptyList(),
    val groups: List<Group> = emptyList(),
    val sessions: List<ShareSession> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val activeSession: ShareSession? = null,
)

/** State of the in-progress track recorder. */
data class TrackRecorderState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val startedAt: Long = 0L,
    /** Recorded time in ms excluding pauses (snapshot; live getter adds the current segment). */
    val accumulatedMs: Long = 0L,
    /** Wall-clock start of the current (unpaused) segment. */
    val segmentStartMs: Long = 0L,
    val points: List<TrackPoint> = emptyList(),
    val currentDistance: Double = 0.0,
    val currentMaxSpeedMs: Double? = null,
) {
    /** Elapsed recorded time: live while recording, frozen after pause/stop. */
    val elapsedMs: Long
        get() {
            if (!isRecording) return accumulatedMs
            return if (isPaused) accumulatedMs
            else accumulatedMs + (System.currentTimeMillis() - segmentStartMs)
        }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as GpsSaverApp).container
    private val repo = container.repository
    private val settingsStore = container.settingsStore
    private val locationRepo = container.locationRepository

    private val _location = MutableStateFlow(
        LocationState(0.0, 0.0, 0.0, null, System.currentTimeMillis(), true, "Waiting for GPS...")
    )
    val location: StateFlow<LocationState> = _location

    private val _hasPermission = MutableStateFlow(locationRepo.hasPermission())
    val hasPermission: StateFlow<Boolean> = _hasPermission

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating

    private val _locatingStatus = MutableStateFlow("Connecting to GPS...")
    val locatingStatus: StateFlow<String> = _locatingStatus

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast

    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }

    val uiState: StateFlow<AppUiState> = combine(
        repo.places, repo.groups, repo.sessions, repo.tracks, settingsStore.settings,
    ) { places, groups, sessions, tracks, settings ->
        AppUiState(
            places = places,
            groups = groups,
            sessions = sessions,
            tracks = tracks,
            settings = settings,
        )
    }
        .combine(ticker) { state, now ->
            state.copy(
                activeSession = state.sessions.firstOrNull { it.isActive && it.expiresAt > now },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    // ---- Track recorder ----

    private val _recorder = MutableStateFlow(TrackRecorderState())
    val recorderState: StateFlow<TrackRecorderState> = _recorder

    /** Points accumulated during the active recording. Written to the DB on stop. */
    private val pendingPoints = mutableListOf<TrackPoint>()

    init {
        viewModelScope.launch {
            repo.seedIfEmpty()
            // Keep existing sessions from expiring silently across launches.
            repo.stopAllSessions()
        }
        observeLocationWhenPermitted()
    }

    private fun observeLocationWhenPermitted() {
        viewModelScope.launch {
            _hasPermission
                .flatMapLatest { granted ->
                    if (granted) locationRepo.locationUpdates() else flowOf()
                }
                .collect { loc ->
                    _location.value = loc
                    if (_recorder.value.isRecording && !_recorder.value.isPaused) {
                        appendTrackPoint(loc)
                    }
                }
        }
    }

    private fun appendTrackPoint(loc: LocationState) {
        val now = System.currentTimeMillis()
        val last = pendingPoints.lastOrNull()

        // Sample at most once per second and skip sub-meter jitter.
        if (last != null && now - last.timestamp < 1_000) return
        val speedMs = if (last != null) {
            val dtSec = (now - last.timestamp).coerceAtLeast(1) / 1000.0
            val d = GeoUtils.calculateDistance(
                last.latitude, last.longitude, loc.latitude, loc.longitude,
            )
            if (d < 2.0) return // skip sub-meter jitter
            d / dtSec
        } else {
            null
        }

        pendingPoints += TrackPoint(
            trackId = 0L, // assigned when the track row is created on stop
            latitude = loc.latitude,
            longitude = loc.longitude,
            altitude = loc.altitude,
            accuracy = loc.accuracy,
            speedMs = speedMs,
            timestamp = now,
        )
        publishRecorderState()
    }

    private fun publishRecorderState() {
        val current = _recorder.value
        val stats = TrackComputer.computeStats(pendingPoints)
        _recorder.value = current.copy(
            points = pendingPoints.toList(),
            currentDistance = stats.distanceMeters,
            currentMaxSpeedMs = stats.maxSpeedMs,
        )
    }

    fun startTrackRecording() {
        if (_recorder.value.isRecording) return
        pendingPoints.clear()
        val now = System.currentTimeMillis()
        _recorder.value = TrackRecorderState(
            isRecording = true,
            isPaused = false,
            startedAt = now,
            accumulatedMs = 0L,
            segmentStartMs = now,
        )
        // Seed with the current fix so the track always has a start point.
        val loc = _location.value
        if (!loc.isSimulated && loc.latitude != 0.0) appendTrackPoint(loc)
    }

    fun pauseTrackRecording() {
        val current = _recorder.value
        if (!current.isRecording || current.isPaused) return
        val now = System.currentTimeMillis()
        _recorder.value = current.copy(
            isPaused = true,
            accumulatedMs = current.accumulatedMs + (now - current.segmentStartMs),
        )
    }

    fun resumeTrackRecording() {
        val current = _recorder.value
        if (!current.isRecording || !current.isPaused) return
        val now = System.currentTimeMillis()
        _recorder.value = current.copy(
            isPaused = false,
            segmentStartMs = now,
        )
    }

    fun stopTrackRecording() {
        if (!_recorder.value.isRecording) return
        val current = _recorder.value
        val points = pendingPoints.toList()
        pendingPoints.clear()
        _recorder.value = TrackRecorderState()
        if (points.isEmpty()) return

        val recordedMs = current.accumulatedMs +
            if (current.isPaused) 0L else (System.currentTimeMillis() - current.segmentStartMs)

        viewModelScope.launch {
            val stats = TrackComputer.computeStats(points)
            val trackId = repo.addTrack(
                Track(
                    id = 0,
                    name = defaultTrackName(),
                    startTime = points.first().timestamp,
                    endTime = points.last().timestamp,
                    distanceMeters = stats.distanceMeters,
                    durationMs = recordedMs,
                    maxSpeedMs = stats.maxSpeedMs,
                    avgSpeedMs = stats.avgSpeedMs,
                    startLatitude = points.first().latitude,
                    startLongitude = points.first().longitude,
                )
            )
            repo.addTrackPoints(points.map { it.copy(trackId = trackId) })
            showToast("Track saved")
        }
    }

    fun cancelTrackRecording() {
        pendingPoints.clear()
        _recorder.value = TrackRecorderState()
    }

    fun deleteTrack(id: Long) {
        viewModelScope.launch {
            repo.deleteTrack(id)
            showToast("Track deleted")
        }
    }

    fun renameTrack(track: Track, newName: String) {
        val name = newName.trim().ifBlank { return }
        viewModelScope.launch {
            repo.updateTrack(track.copy(name = name))
            showToast("Track renamed")
        }
    }

    private fun defaultTrackName(): String {
        val fmt = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.US)
        return "Track ${fmt.format(java.util.Date())}"
    }

    fun pointsForTrack(trackId: Long): Flow<List<TrackPoint>> = repo.pointsForTrack(trackId)

    suspend fun trackGpx(trackId: Long): String? {
        val track = repo.trackById(trackId) ?: return null
        val points = repo.pointsForTrackSync(trackId)
        return ExportCodecs.generateTrackGpx(track, points)
    }

    fun onPermissionResult(granted: Boolean) {
        _hasPermission.value = granted
        if (!granted) {
            _errorMessage.value =
                "Location permission was denied. Enable it in Settings > Apps > GPS Position Saver > Permissions."
        } else {
            _errorMessage.value = null
            refreshLocation()
        }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            _isLocating.value = true
            _locatingStatus.value = "Acquiring GPS satellites..."
            val loc = locationRepo.currentLocation()
            _isLocating.value = false
            if (loc != null) {
                _location.value = loc
                _errorMessage.value = null
            } else {
                _errorMessage.value =
                    "GPS position unavailable. Ensure Location Services are enabled."
            }
        }
    }

    fun acquireGpsLock(onAcquired: (LocationState) -> Unit = {}) {
        viewModelScope.launch {
            _isLocating.value = true
            _locatingStatus.value = "Connecting to GPS satellites..."
            val statusJob = launch {
                var elapsed = 0
                while (true) {
                    delay(1000)
                    elapsed++
                    _locatingStatus.value = when {
                        elapsed <= 2 -> "Connecting to GPS satellites..."
                        elapsed <= 5 -> "Calculating precise coordinates..."
                        else -> "Refining position accuracy..."
                    }
                }
            }
            val loc = locationRepo.currentLocation()
            statusJob.cancel()
            _isLocating.value = false
            if (loc != null) {
                _location.value = loc
                _errorMessage.value = null
                onAcquired(loc)
            } else {
                _errorMessage.value =
                    "Could not obtain a GPS fix yet. Move to an open area and retry."
            }
        }
    }

    fun savePlace(
        name: String?,
        latitude: Double,
        longitude: Double,
        accuracy: Double,
        altitude: Double?,
        address: String?,
        note: String?,
        groupId: Long?,
        phone: String? = null,
        photoUri: String? = null,
        onSaved: (Long) -> Unit = {},
    ) {
        viewModelScope.launch {
            val id = repo.addPlace(
                Place(
                    id = 0,
                    name = name?.ifBlank { null } ?: "Saved Position",
                    latitude = latitude,
                    longitude = longitude,
                    accuracy = accuracy,
                    altitude = altitude,
                    address = address,
                    note = note,
                    phone = phone,
                    photoUri = photoUri,
                    groupId = groupId,
                    isFavorite = false,
                )
            )
            onSaved(id)
        }
    }

    fun updatePlace(place: Place) = viewModelScope.launch { repo.updatePlace(place) }

    fun deletePlace(id: Long) = viewModelScope.launch { repo.deletePlace(id) }

    fun toggleFavorite(place: Place) =
        viewModelScope.launch { repo.updatePlace(place.copy(isFavorite = !place.isFavorite)) }

    fun refreshAddress(place: Place, onResolved: (String?) -> Unit) {
        viewModelScope.launch {
            val address = GeocoderClient.reverseGeocode(place.latitude, place.longitude)
            address?.let { repo.updatePlace(place.copy(address = it)) }
            onResolved(address)
        }
    }

    fun createGroup(name: String, colorHex: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch { onCreated(repo.addGroup(name, colorHex)) }
    }

    fun deleteGroup(id: Long) = viewModelScope.launch { repo.deleteGroup(id) }

    fun updateSettings(settings: AppSettings) =
        viewModelScope.launch { settingsStore.save(settings) }

    fun setCoordinateFormat(format: CoordinateFormat) = viewModelScope.launch {
        val current = uiState.value.settings
        settingsStore.save(current.copy(coordinateFormat = format))
    }

    fun setUnitSystem(units: UnitSystem) = viewModelScope.launch {
        val current = uiState.value.settings
        settingsStore.save(current.copy(unitSystem = units))
    }

    fun setDefaultGroup(groupId: Long?) = viewModelScope.launch {
        val current = uiState.value.settings
        settingsStore.save(current.copy(defaultGroupId = groupId))
    }

    fun clearAllData() {
        viewModelScope.launch {
            repo.clearAllData()
            showToast("All saved data cleared")
        }
    }

    fun startLiveSession(durationMinutes: Int) {
        val loc = _location.value
        val sessionId = randomToken()
        val start = System.currentTimeMillis()
        val expires = start + durationMinutes * 60_000L
        val url = "https://www.openstreetmap.org/?mlat=${loc.latitude}&mlon=${loc.longitude}#live=$sessionId"
        viewModelScope.launch {
            repo.addSession(
                ShareSession(
                    sessionId = sessionId,
                    startTime = start,
                    durationMinutes = durationMinutes,
                    expiresAt = expires,
                    isActive = true,
                    shareUrl = url,
                    lastLatitude = loc.latitude,
                    lastLongitude = loc.longitude,
                    lastAccuracy = loc.accuracy,
                    lastUpdated = start,
                )
            )
            showToast("Live sharing started for $durationMinutes min")
        }
    }

    fun stopLiveSession() {
        viewModelScope.launch {
            repo.stopAllSessions()
            showToast("Live sharing stopped")
        }
    }

    fun buildExport(format: ExportFormat): String {
        val state = uiState.value
        return when (format) {
            ExportFormat.GPX -> ExportCodecs.generateGpx(state.places)
            ExportFormat.KML -> ExportCodecs.generateKml(state.places)
            ExportFormat.CSV -> ExportCodecs.generateCsv(state.places, state.groups)
            ExportFormat.JSON -> ExportCodecs.generateJson(state.places, state.groups)
        }
    }

    fun importFrom(result: ImportResult, onDone: (Int) -> Unit = {}) {
        viewModelScope.launch {
            result.places.forEach { repo.addPlace(it.copy(id = 0)) }
            onDone(result.places.size)
            if (result.places.isNotEmpty()) showToast("Imported ${result.places.size} places")
        }
    }

    fun hostingUrl(): String {
        val loc = _location.value
        return "https://www.openstreetmap.org/?mlat=${loc.latitude}&mlon=${loc.longitude}" +
            "#map=17/${loc.latitude}/${loc.longitude}"
    }

    fun showToast(message: String) { _toast.value = message }

    fun clearToast() { _toast.value = null }

    fun clearError() { _errorMessage.value = null }

    private fun randomToken(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..12).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
}
