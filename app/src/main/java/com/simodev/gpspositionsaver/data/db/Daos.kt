package com.simodev.gpspositionsaver.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.simodev.gpspositionsaver.data.model.Group
import com.simodev.gpspositionsaver.data.model.Place
import com.simodev.gpspositionsaver.data.model.ShareSession
import com.simodev.gpspositionsaver.data.model.Track
import com.simodev.gpspositionsaver.data.model.TrackPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Query("SELECT * FROM places ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Place>>

    @Query("SELECT * FROM places ORDER BY createdAt DESC")
    suspend fun getAll(): List<Place>

    @Query("SELECT * FROM places WHERE id = :id")
    suspend fun getById(id: Long): Place?

    @Insert
    suspend fun insert(place: Place): Long

    @Insert
    suspend fun insertAll(places: List<Place>): List<Long>

    @Update
    suspend fun update(place: Place)

    @Query("DELETE FROM places WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM places")
    suspend fun deleteAll()

    @Query("UPDATE places SET groupId = NULL WHERE groupId = :groupId")
    suspend fun clearGroup(groupId: Long)
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<Group>>

    @Query("SELECT * FROM groups ORDER BY createdAt ASC")
    suspend fun getAll(): List<Group>

    @Query("SELECT COUNT(*) FROM groups")
    suspend fun count(): Int

    @Insert
    suspend fun insert(group: Group): Long

    @Insert
    suspend fun insertAll(groups: List<Group>)

    @Update
    suspend fun update(group: Group)

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY startTime DESC")
    fun observeAll(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getById(id: Long): Track?

    @Query("SELECT * FROM tracks ORDER BY startTime DESC")
    suspend fun getAll(): List<Track>

    @Insert
    suspend fun insert(track: Track): Long

    @Update
    suspend fun update(track: Track)

    @Query("DELETE FROM tracks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tracks")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun count(): Int
}

@Dao
interface TrackPointDao {
    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    fun observeByTrack(trackId: Long): Flow<List<TrackPoint>>

    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    suspend fun getByTrack(trackId: Long): List<TrackPoint>

    @Insert
    suspend fun insertAll(points: List<TrackPoint>)

    @Query("DELETE FROM track_points WHERE trackId = :trackId")
    suspend fun deleteByTrack(trackId: Long)

    @Query("DELETE FROM track_points")
    suspend fun deleteAll()
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun observeAll(): Flow<List<ShareSession>>

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    suspend fun getAll(): List<ShareSession>

    @Insert
    suspend fun insert(session: ShareSession)

    @Query("UPDATE sessions SET isActive = 0")
    suspend fun deactivateAll()

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}
