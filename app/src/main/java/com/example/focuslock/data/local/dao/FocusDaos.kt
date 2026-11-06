package com.example.focuslock.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.focuslock.data.local.entity.BlockedAppEntity
import com.example.focuslock.data.local.entity.FocusScheduleEntity
import com.example.focuslock.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 1")
    fun getActiveSessionFlow(): Flow<FocusSessionEntity?>

    @Query("SELECT * FROM focus_sessions WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 1")
    fun getActiveSessionSync(): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions ORDER BY startTimeMillis DESC")
    fun getAllSessionsFlow(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): FocusSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Update
    suspend fun updateSession(session: FocusSessionEntity)

    @Query("UPDATE focus_sessions SET blocksPreventedCount = blocksPreventedCount + 1 WHERE id = :sessionId")
    fun incrementBlocksPrevented(sessionId: Long)

    @Query("UPDATE focus_sessions SET status = :newStatus, endTimeMillis = :endTime WHERE status = 'ACTIVE'")
    suspend fun endAllActiveSessions(newStatus: String, endTime: Long)
}

@Dao
interface BlockedAppDao {
    @Query("SELECT * FROM blocked_apps ORDER BY appName ASC")
    fun getAllBlockedAppsFlow(): Flow<List<BlockedAppEntity>>

    @Query("SELECT packageName FROM blocked_apps WHERE isCurrentlyBlocked = 1")
    fun getBlockedPackageNamesSync(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(app: BlockedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<BlockedAppEntity>)

    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM blocked_apps")
    suspend fun clearAll()
}

@Dao
interface FocusScheduleDao {
    @Query("SELECT * FROM focus_schedules ORDER BY startHour ASC, startMinute ASC")
    fun getAllSchedulesFlow(): Flow<List<FocusScheduleEntity>>

    @Query("SELECT * FROM focus_schedules WHERE isEnabled = 1")
    fun getEnabledSchedulesSync(): List<FocusScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: FocusScheduleEntity): Long

    @Update
    suspend fun update(schedule: FocusScheduleEntity)

    @Query("DELETE FROM focus_schedules WHERE id = :id")
    suspend fun deleteById(id: Long)
}
