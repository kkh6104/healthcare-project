# 구현 요청 (claude-1): 3단계-2 동기화 (Health Connect → DB)

## 공통
- 설계: tasks/step3-design.md, DB API: tasks/step3-db-done.md (data/ 패키지의 엔티티/DAO 사용, 수정 금지)
- **새 파일만 만든다. 기존 파일 수정 금지** (다른 에이전트 동시 작업 중, 연결은 다음 단계). 
- 시간대 ZoneId.systemDefault(). 날짜 문자열 "yyyy-MM-dd".
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug` (동시 빌드 lock 시 30초 후 재시도)

## 새 파일: app/src/main/java/com/gh/healthapp/data/SyncRepository.kt
- `data class SyncResult(val type: String, val fetched: Int, val affectedDates: Set<LocalDate>, val stepsByDate: Map<LocalDate, Long> = emptyMap())`
- `class SyncRepository(private val client: HealthConnectClient, private val dao: HealthDao)`
  - `suspend fun sync(type: String, now: Instant = Instant.now()): SyncResult` — 한 종류만. ViewModel이 종류별로 순차 호출.
  - 범위: dao.syncState(type) 없으면 (오늘-30일 0시) ~ now, 있으면 (syncedUntil - 1일) ~ now.
  - 레코드 조회는 반드시 기존 `client.readAllPages(name, maxPages = 5) { token -> ReadRecordsRequest(...) }` (HcLog.kt) 사용. name 예: "sync(steps)", "sync(heartRate)".
  - 매핑 (source = RecordSource.HEALTH_CONNECT, originPackage = metadata.dataOrigin.packageName, lastModified = metadata.lastModifiedTime ms, zoneOffsetSec = 기록의 startZoneOffset/zoneOffset?.totalSeconds):
    - STEPS: StepsRecord → value=count, unit="count", start/end, externalId=metadata.id
    - HEART_RATE: HeartRateRecord의 **각 sample을 1행**: start=end=sample.time, value=bpm, unit="bpm", externalId="${metadata.id}#${index}"
    - SLEEP_SESSION: value=잠든 분(기존 SleepRepository.kt의 internal `toSleepNight(record, zone)`의 asleep 사용), unit="min", extraJson = {"deep":분,"light":분,"rem":분,"awake":분} (null 단계는 생략, org.json.JSONObject 사용)
    - WEIGHT(kg), BODY_FAT(%: percentage.value), BONE_MASS/LEAN_BODY_MASS/BODY_WATER_MASS(kg): 순간 측정, start=end=time
  - 저장: dao.upsertRecords(...) 후 dao.upsertSyncState(SyncStateEntity(type, now ms)).
  - affectedDates: 받은 레코드의 날짜(기기 시간대). 수면은 endTime 날짜. 범위의 시작~오늘 날짜도 포함(데이터 0건인 날도 요약 재계산되도록).
  - STEPS일 때만 추가로 affectedDates 범위 전체에 대해 `aggregateGroupByPeriod`(Period.ofDays(1), LocalDateTime 범위)를 hcCall("sync(stepsDaily)")로 1회 호출해 stepsByDate 채우기 (원본 합산은 폰+워치 중복이라 사용 금지).
- 매핑은 HealthConnectClient 없이 테스트 가능한 internal 순수 함수로 분리.

## 완료
- assembleDebug 성공, tasks/step3-sync-done.md 에 요약 + 공개 API
