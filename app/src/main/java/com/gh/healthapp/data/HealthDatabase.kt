package com.gh.healthapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [HealthRecordEntity::class, DailySummaryEntity::class, SyncStateEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun dao(): HealthDao

    companion object {
        @Volatile private var instance: HealthDatabase? = null

        fun get(context: Context): HealthDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HealthDatabase::class.java,
                    "health.db",
                ).build().also { instance = it }
            }
    }
}
