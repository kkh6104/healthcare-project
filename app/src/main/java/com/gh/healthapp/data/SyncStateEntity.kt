package com.gh.healthapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val type: String,
    val syncedUntil: Long,
)
