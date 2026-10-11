package com.simodev.gpspositionsaver

import android.content.Context
import com.simodev.gpspositionsaver.data.PlacesRepository
import com.simodev.gpspositionsaver.data.SettingsStore
import com.simodev.gpspositionsaver.data.db.AppDatabase
import com.simodev.gpspositionsaver.location.LocationRepository

/** Hand-rolled dependency container (KISS — no DI framework needed). */
class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.get(context)
    val repository: PlacesRepository = PlacesRepository(database)
    val settingsStore: SettingsStore = SettingsStore(context)
    val locationRepository: LocationRepository = LocationRepository(context)
}
