# step3-db 완료
- Room 2.8.5 (runtime/ktx/compiler) + KSP 2.3.14 (com.google.devtools.ksp) 추가. Kotlin 2.2.10 / AGP 9.5.2에서 그대로 빌드됨. exportSchema = false.
- 수정한 gradle 파일: gradle/libs.versions.toml, build.gradle.kts(루트), app/build.gradle.kts. 앱 동작 변경 없음.
- assembleDebug assembleDiag 성공.

## 새 파일 (app/src/main/java/com/gh/healthapp/data/)
HealthRecordEntity.kt (+ RecordType, RecordSource), DailySummaryEntity.kt, SyncStateEntity.kt, HealthDao.kt, HealthDatabase.kt

## HealthDao
- @Insert(onConflict = REPLACE) suspend fun upsertRecords(list: List<HealthRecordEntity>)  // @Upsert는 PK 기준이라 UNIQUE(source,type,externalId) 충돌 시 예외 → REPLACE로 externalId 기준 덮어쓰기
- suspend fun recordsBetween(type: String, from: Long, to: Long): List<HealthRecordEntity>
- fun latestRecordFlow(type: String): Flow<HealthRecordEntity?>
- @Upsert suspend fun upsertSummaries(list: List<DailySummaryEntity>)
- fun summariesFlow(fromDate: String): Flow<List<DailySummaryEntity>>
- suspend fun summary(date: String): DailySummaryEntity?
- suspend fun syncState(type: String): SyncStateEntity?
- @Upsert suspend fun upsertSyncState(s: SyncStateEntity)
- fun syncStatesFlow(): Flow<List<SyncStateEntity>>

## HealthDatabase
@Database(version = 1), abstract fun dao(): HealthDao, HealthDatabase.get(context) 싱글턴, 파일명 "health.db"
