# step3-sync 완료
- 새 파일 1개만 생성(기존 파일 수정 없음): app/src/main/java/com/gh/healthapp/data/SyncRepository.kt
- sync(type): syncState 없으면 (오늘-30일 0시)~now, 있으면 (syncedUntil-1일)~now를 readAllPages(maxPages=5)로 조회 → 매핑 → dao.upsertRecords → dao.upsertSyncState. 걸음일 때만 hcCall("sync(stepsDaily)") aggregateGroupByPeriod로 날짜별 합계(stepsByDate, 데이터 없는 날은 0) 계산.
- affectedDates: 범위 시작~오늘 전체 + 받은 레코드 날짜(수면은 endTime 날짜). 매핑은 internal 순수 함수로 분리.
- 빌드: assembleDebug BUILD SUCCESSFUL (단위 테스트는 작성/실행하지 않음)

## 공개 API
- data class SyncResult(type: String, fetched: Int, affectedDates: Set<LocalDate>, stepsByDate: Map<LocalDate, Long> = emptyMap())
- class SyncRepository(client: HealthConnectClient, dao: HealthDao) { suspend fun sync(type: String, now: Instant = Instant.now()): SyncResult }
- companion: INITIAL_DAYS = 30, DATE_PATTERN = "yyyy-MM-dd"
- internal: affectedDates(...), mapSteps, mapHeartRate(1 sample = 1행, externalId="id#index"), mapSleep(record, zone), mapWeight, mapBodyFat, mapBoneMass, mapLeanBodyMass, mapBodyWaterMass, dateString
- 지원 type: RecordType의 8종 (그 외는 IllegalArgumentException)
