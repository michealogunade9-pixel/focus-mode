package com.example.focuslock.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.focuslock.data.local.dao.BlockedAppDao
import com.example.focuslock.data.local.dao.FocusScheduleDao
import com.example.focuslock.data.local.dao.FocusSessionDao
import com.example.focuslock.data.local.entity.BlockedAppEntity
import com.example.focuslock.data.local.entity.FocusScheduleEntity
import com.example.focuslock.data.local.entity.FocusSessionEntity

@Database(
    entities = [
        FocusSessionEntity::class,
        BlockedAppEntity::class,
        FocusScheduleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FocusDatabase : RoomDatabase() {
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun focusScheduleDao(): FocusScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: FocusDatabase? = null

        fun getInstance(context: Context): FocusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FocusDatabase::class.java,
                    "focuslock_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
