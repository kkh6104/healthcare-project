package com.gh.healthapp.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "health_record",
    indices = [
        Index(value = ["source", "type", "externalId"], unique = true),
        Index(value = ["type", "startTime"]),
    ],
)
data class HealthRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val startTime: Long,
    val endTime: Long,
    val zoneOffsetSec: Int?,
    val value: Double,
    val unit: String,
    val source: String,
    val originPackage: String?,
    val externalId: String,
    val lastModified: Long?,
    val extraJson: String?,
)

object RecordType {
    const val STEPS = "STEPS"
    const val HEART_RATE = "HEART_RATE"
    const val SLEEP_SESSION = "SLEEP_SESSION"
    const val WEIGHT = "WEIGHT"
    const val BODY_FAT = "BODY_FAT"
    const val BONE_MASS = "BONE_MASS"
    const val LEAN_BODY_MASS = "LEAN_BODY_MASS"
    const val BODY_WATER_MASS = "BODY_WATER_MASS"
}

object RecordSource {
    const val HEALTH_CONNECT = "health_connect"
    const val SAMSUNG_CSV = "samsung_csv"
}
