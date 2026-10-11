package com.simodev.gpspositionsaver.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.simodev.gpspositionsaver.data.model.Group
import com.simodev.gpspositionsaver.data.model.Place
import com.simodev.gpspositionsaver.data.model.ShareSession

@Database(
    entities = [Place::class, Group::class, ShareSession::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
    abstract fun groupDao(): GroupDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gps_position_saver.db",
                ).build().also { INSTANCE = it }
            }
    }
}
