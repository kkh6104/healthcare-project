package com.gh.healthapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WalkDao {
    @Insert
    suspend fun insertSession(s: WalkSessionEntity): Long

    @Update
    suspend fun updateSession(s: WalkSessionEntity)

    @Insert
    suspend fun insertPoints(list: List<WalkPointEntity>)

    /** 끝난(DONE) 기록만, 최신순 */
    @Query("SELECT * FROM walk_session WHERE status = 'DONE' ORDER BY startTime DESC")
    fun sessionsFlow(): Flow<List<WalkSessionEntity>>

    @Query("SELECT * FROM walk_session WHERE id = :id")
    suspend fun session(id: Long): WalkSessionEntity?

    @Query("SELECT * FROM walk_point WHERE sessionId = :sessionId ORDER BY time ASC")
    suspend fun points(sessionId: Long): List<WalkPointEntity>

    /** 아직 끝나지 않은(RUNNING/PAUSED) 가장 최근 기록 */
    @Query("SELECT * FROM walk_session WHERE status IN ('RUNNING', 'PAUSED') ORDER BY id DESC LIMIT 1")
    suspend fun runningSession(): WalkSessionEntity?

    /** 기록 삭제 (경로 점은 CASCADE로 함께 삭제) */
    @Query("DELETE FROM walk_session WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("SELECT * FROM walk_session WHERE status = 'DONE' AND startTime >= :from AND startTime < :to")
    suspend fun doneBetween(from: Long, to: Long): List<WalkSessionEntity>
}
