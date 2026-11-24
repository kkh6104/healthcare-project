package com.gh.healthapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 걷기 기록 1회. status: "RUNNING" / "PAUSED" / "DONE" */
@Entity(tableName = "walk_session")
data class WalkSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long?,
    val distanceM: Double,
    val activeMs: Long,
    val steps: Int,
    val status: String,
)

/** 걷기 경로의 점. 세션이 지워지면 함께 지워진다 */
@Entity(
    tableName = "walk_point",
    foreignKeys = [
        ForeignKey(
            entity = WalkSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["sessionId"])],
)
data class WalkPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val time: Long,
    val lat: Double,
    val lon: Double,
    val accuracy: Float,
    val altitude: Double?,
)

object WalkStatusValue {
    const val RUNNING = "RUNNING"
    const val PAUSED = "PAUSED"
    const val DONE = "DONE"
}
