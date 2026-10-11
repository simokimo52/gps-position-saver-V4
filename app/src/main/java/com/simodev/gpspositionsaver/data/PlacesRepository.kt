package com.simodev.gpspositionsaver.data

import com.simodev.gpspositionsaver.data.db.AppDatabase
import com.simodev.gpspositionsaver.data.model.Group
import com.simodev.gpspositionsaver.data.model.Place
import com.simodev.gpspositionsaver.data.model.ShareSession
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for places, groups, and share sessions.
 * Replaces the localStorage `StorageService` from the web edition.
 */
class PlacesRepository(private val db: AppDatabase) {

    private val placeDao = db.placeDao()
    private val groupDao = db.groupDao()
    private val sessionDao = db.sessionDao()

    val places: Flow<List<Place>> = placeDao.observeAll()
    val groups: Flow<List<Group>> = groupDao.observeAll()
    val sessions: Flow<List<ShareSession>> = sessionDao.observeAll()

    suspend fun seedIfEmpty() {
        if (groupDao.count() > 0) return
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        val groups = listOf(
            Group(1, "Home", "#4CAF50", "home", now - day * 5),
            Group(2, "Work", "#1E88E5", "briefcase", now - day * 4),
            Group(3, "Outdoor", "#FB8C00", "mountain", now - day * 3),
            Group(4, "Parking", "#8E24AA", "car", now - day * 2),
            Group(5, "Travel", "#00ACC1", "compass", now - day),
        )
        groupDao.insertAll(groups)
        placeDao.insertAll(
            listOf(
                Place(
                    id = 0, name = "Eiffel Tower Viewpoint", latitude = 48.858370, longitude = 2.294481,
                    accuracy = 4.2, altitude = 35.0,
                    address = "Champ de Mars, 5 Avenue Anatole France, 75007 Paris, France",
                    note = "Best photo spot at sunset right across the gardens.",
                    phone = "+33 892 70 12 39", groupId = 5, isFavorite = true,
                    createdAt = now - day * 2, updatedAt = now - day * 2,
                ),
                Place(
                    id = 0, name = "Central Park North Meadow", latitude = 40.793200, longitude = -73.957500,
                    accuracy = 6.8, altitude = 24.5,
                    address = "Central Park, New York, NY 10029, USA",
                    note = "Quiet picnic spot shaded under large elm trees.",
                    phone = "+1 212-310-6600", groupId = 3, isFavorite = true,
                    createdAt = now - day, updatedAt = now - day,
                ),
                Place(
                    id = 0, name = "North Gate Underground Parking", latitude = 48.860611, longitude = 2.337644,
                    accuracy = 3.5, altitude = 28.0,
                    address = "Rue de Rivoli, 75001 Paris, France",
                    note = "Level -2, Pillar C14, ticket validated at exit.",
                    groupId = 4, isFavorite = false,
                    createdAt = now - 3_600_000L * 4, updatedAt = now - 3_600_000L * 4,
                ),
            )
        )
    }

    suspend fun addPlace(place: Place): Long = placeDao.insert(place)

    suspend fun updatePlace(place: Place) =
        placeDao.update(place.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deletePlace(id: Long) = placeDao.deleteById(id)

    suspend fun importPlaces(places: List<Place>) {
        if (places.isEmpty()) return
        placeDao.insertAll(places.map { it.copy(id = 0, createdAt = it.createdAt) })
    }

    suspend fun addGroup(name: String, colorHex: String = "#1E88E5", iconName: String = "pin"): Long =
        groupDao.insert(Group(0, name, colorHex, iconName))

    suspend fun deleteGroup(id: Long) {
        groupDao.deleteById(id)
        placeDao.clearGroup(id)
    }

    suspend fun addSession(session: ShareSession) = sessionDao.insert(session)

    suspend fun stopAllSessions() = sessionDao.deactivateAll()

    suspend fun clearAllData() {
        placeDao.deleteAll()
        sessionDao.deleteAll()
    }
}
