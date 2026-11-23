package com.gh.healthapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_summary")
data class DailySummaryEntity(
    /** "yyyy-MM-dd" */
    @PrimaryKey val date: String,
    val steps: Long?,
    val hrAvg: Double?,
    val hrMin: Double?,
    val hrMax: Double?,
    val sleepMinutes: Long?,
    val weightKg: Double?,
    val bodyFatPct: Double?,
    val boneMassKg: Double?,
    val leanMassKg: Double?,
    val bodyWaterKg: Double?,
    val updatedAt: Long,
)
