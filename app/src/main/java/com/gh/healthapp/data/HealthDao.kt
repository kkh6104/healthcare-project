package com.gh.healthapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {

    // @Upsert는 PK(id) 충돌만 보고, id=0으로 들어오는 새 행은 UNIQUE(source,type,externalId)에 걸려 예외가 난다.
    // REPLACE는 UNIQUE 충돌 시 기존 행을 지우고 새로 넣으므로 externalId 기준 덮어쓰기가 된다.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecords(list: List<HealthRecordEntity>)

    @Query("SELECT * FROM health_record WHERE type = :type AND startTime >= :from AND startTime <= :to ORDER BY startTime ASC")
    suspend fun recordsBetween(type: String, from: Long, to: Long): List<HealthRecordEntity>

    @Query("SELECT * FROM health_record WHERE type = :type ORDER BY startTime DESC LIMIT 1")
    fun latestRecordFlow(type: String): Flow<HealthRecordEntity?>

    /** 정리 대상: 지정 시각 이전의, 아직 1시간 요약("n" 없음)이 아닌 행 */
    @Query("SELECT * FROM health_record WHERE type = :type AND startTime < :before AND (extraJson IS NULL OR extraJson NOT LIKE '%\"n\"%') ORDER BY startTime ASC")
    suspend fun rawRecordsBefore(type: String, before: Long): List<HealthRecordEntity>

    @Query("DELETE FROM health_record WHERE id IN (:ids)")
    suspend fun deleteRecordsByIds(ids: List<Long>)

    /** 묶음 행 upsert + 원본 삭제를 한 트랜잭션으로 */
    @Transaction
    suspend fun replaceRecords(upserts: List<HealthRecordEntity>, deleteIds: List<Long>) {
        // SQLite 변수 한도를 넘지 않도록 500개씩
        upserts.chunked(500).forEach { upsertRecords(it) }
        deleteIds.chunked(500).forEach { deleteRecordsByIds(it) }
    }

    @Query("SELECT MIN(startTime) FROM health_record")
    suspend fun minRecordTime(): Long?

    @Query("SELECT MAX(startTime) FROM health_record")
    suspend fun maxRecordTime(): Long?

    @Upsert
    suspend fun upsertSummaries(list: List<DailySummaryEntity>)

    @Query("SELECT * FROM daily_summary WHERE date >= :fromDate ORDER BY date DESC")
    fun summariesFlow(fromDate: String): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM daily_summary WHERE date = :date")
    suspend fun summary(date: String): DailySummaryEntity?

    @Query("SELECT * FROM sync_state WHERE type = :type")
    suspend fun syncState(type: String): SyncStateEntity?

    @Upsert
    suspend fun upsertSyncState(s: SyncStateEntity)

    @Query("SELECT * FROM sync_state")
    fun syncStatesFlow(): Flow<List<SyncStateEntity>>
}
