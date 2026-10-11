package com.simodev.gpspositionsaver.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.simodev.gpspositionsaver.data.model.Group
import com.simodev.gpspositionsaver.data.model.Place
import com.simodev.gpspositionsaver.data.model.ShareSession
import com.simodev.gpspositionsaver.data.model.Track
import com.simodev.gpspositionsaver.data.model.TrackPoint

@Database(
    entities = [Place::class, Group::class, ShareSession::class, Track::class, TrackPoint::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
    abstract fun groupDao(): GroupDao
    abstract fun sessionDao(): SessionDao
    abstract fun trackDao(): TrackDao
    abstract fun trackPointDao(): TrackPointDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** v1 → v2: add the `tracks` and `track_points` tables. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `tracks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `startTime` INTEGER NOT NULL,
                        `endTime` INTEGER,
                        `distanceMeters` REAL NOT NULL,
                        `durationMs` INTEGER NOT NULL,
                        `maxSpeedMs` REAL,
                        `avgSpeedMs` REAL,
                        `startLatitude` REAL NOT NULL,
                        `startLongitude` REAL NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `track_points` (
                        `trackId` INTEGER NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `altitude` REAL,
                        `accuracy` REAL,
                        `speedMs` REAL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`trackId`, `timestamp`)
                    )"""
                )
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gps_position_saver.db",
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
    }
}
