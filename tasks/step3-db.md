# 구현 요청 (claude-2): 3단계-1 Room 데이터베이스 뼈대

설계: tasks/step3-design.md (반드시 먼저 읽기)

## 1. Gradle
- Room(androidx.room: runtime, ktx, compiler) **최신 안정 버전**과 KSP 플러그인(com.google.devtools.ksp)을 gradle/libs.versions.toml + build.gradle.kts(루트/앱)에 추가.
- 버전은 maven 메타데이터로 확인할 것: Room = https://dl.google.com/android/maven2/androidx/room/room-runtime/maven-metadata.xml , KSP = Gradle Plugin Portal / Maven Central의 com.google.devtools.ksp. **현재 프로젝트 Kotlin 2.2.10, AGP 9.5.2(내장 Kotlin, kotlin-android 플러그인 없음)와 호환되는 버전**을 고른다. 안 맞으면 빌드 에러를 보고 조정.
- Room schema export는 끄거나(exportSchema = false) app/schemas로 지정.

## 2. 새 파일 (패키지 com.gh.healthapp.data)
- app/src/main/java/com/gh/healthapp/data/HealthRecordEntity.kt
  - @Entity(tableName = "health_record", indices = [Index(value = ["source","type","externalId"], unique = true), Index(value = ["type","startTime"])])
  - 필드: id: Long (PK autoGenerate), type: String, startTime: Long, endTime: Long, zoneOffsetSec: Int?, value: Double, unit: String, source: String, originPackage: String?, externalId: String, lastModified: Long?, extraJson: String?
  - `object RecordType { const val STEPS="STEPS"; HEART_RATE; SLEEP_SESSION; WEIGHT; BODY_FAT; BONE_MASS; LEAN_BODY_MASS; BODY_WATER_MASS }`, `object RecordSource { const val HEALTH_CONNECT="health_connect"; const val SAMSUNG_CSV="samsung_csv" }`
- data/DailySummaryEntity.kt: @Entity("daily_summary") date: String (PK, "yyyy-MM-dd"), steps: Long?, hrAvg: Double?, hrMin: Double?, hrMax: Double?, sleepMinutes: Long?, weightKg: Double?, bodyFatPct: Double?, boneMassKg: Double?, leanMassKg: Double?, bodyWaterKg: Double?, updatedAt: Long
- data/SyncStateEntity.kt: @Entity("sync_state") type: String (PK), syncedUntil: Long
- data/HealthDao.kt (@Dao):
  - `@Upsert suspend fun upsertRecords(list: List<HealthRecordEntity>)` — 주의: UNIQUE(source,type,externalId) 충돌 시 덮어써야 함. @Upsert는 PK 기준이므로, **externalId 기준 덮어쓰기가 되도록** 구현 방법을 선택 (예: @Insert(onConflict = REPLACE) 사용 + 주석으로 이유 설명).
  - `suspend fun recordsBetween(type: String, from: Long, to: Long): List<HealthRecordEntity>` (startTime 기준, 오름차순)
  - `fun latestRecordFlow(type: String): Flow<HealthRecordEntity?>`
  - `@Upsert suspend fun upsertSummaries(list: List<DailySummaryEntity>)`
  - `fun summariesFlow(fromDate: String): Flow<List<DailySummaryEntity>>` (date >= fromDate, 최신 날짜 먼저)
  - `suspend fun summary(date: String): DailySummaryEntity?`
  - `suspend fun syncState(type: String): SyncStateEntity?`, `@Upsert suspend fun upsertSyncState(s: SyncStateEntity)`
  - `fun syncStatesFlow(): Flow<List<SyncStateEntity>>`
- data/HealthDatabase.kt: @Database(entities = 3개, version = 1), `companion object { fun get(context): HealthDatabase }` 싱글턴, 파일명 "health.db".

## 규칙
- 기존 파일은 gradle 관련 파일 외에 수정하지 않는다. 앱 동작은 아직 바뀌지 않는다(연결은 다음 단계).
- 간단한 JVM 단위 테스트는 불필요.

## 완료
- PowerShell: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug assembleDiag` 성공
- tasks/step3-db-done.md 에 선택한 Room/KSP 버전, 파일 목록, DAO 시그니처
