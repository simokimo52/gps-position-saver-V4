package com.simodev.gpspositionsaver.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.simodev.gpspositionsaver.data.model.AppSettings
import com.simodev.gpspositionsaver.data.model.CoordinateFormat
import com.simodev.gpspositionsaver.data.model.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/** Preferences-backed settings persistence (replaces localStorage `gps_saver_settings`). */
class SettingsStore(private val context: Context) {

    private object Keys {
        val COORD_FORMAT = stringPreferencesKey("coordinate_format")
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val DEFAULT_GROUP_ID = longPreferencesKey("default_group_id")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            coordinateFormat = prefs[Keys.COORD_FORMAT]
                ?.let { runCatching { CoordinateFormat.valueOf(it) }.getOrNull() }
                ?: CoordinateFormat.DECIMAL_DEGREES,
            unitSystem = prefs[Keys.UNIT_SYSTEM]
                ?.let { runCatching { UnitSystem.valueOf(it) }.getOrNull() }
                ?: UnitSystem.METRIC,
            defaultGroupId = prefs[Keys.DEFAULT_GROUP_ID],
        )
    }

    suspend fun save(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.COORD_FORMAT] = settings.coordinateFormat.name
            prefs[Keys.UNIT_SYSTEM] = settings.unitSystem.name
            settings.defaultGroupId?.let { prefs[Keys.DEFAULT_GROUP_ID] = it }
                ?: prefs.remove(Keys.DEFAULT_GROUP_ID)
        }
    }
}
