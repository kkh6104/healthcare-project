# step3-local 완료
- data/ 패키지에 새 파일 2개만 추가(기존 파일 수정 없음). assembleDebug 성공.
- DailySummaryBuilder: 날짜별로 DB 원본에서 하루 요약 계산 후 upsert. 걸음은 stepsByDate 우선, 없으면 기존 값 유지. 심박은 그날 평균/최소/최대, 수면은 endTime이 그날인 행의 합, 체성분은 그날 가장 늦은 측정. 계산은 internal 순수 함수 buildDailySummary.
- LocalHealthReader: DB를 기존 화면용 데이터 클래스로 변환(buildWeightSummary/buildMetricSummary 재사용).
- 수면 extraJson은 {"deep","light","rem","awake"}(분 단위) 형식을 가정함(step3-sync 명세와 동일). 심박 최근값은 DB 행 기준(원본 샘플).

## 공개 API
- class DailySummaryBuilder(dao: HealthDao) { suspend fun rebuild(dates: Set<LocalDate>, stepsByDate: Map<LocalDate, Long>) }
- internal fun buildDailySummary(date, zone, steps, hrRows, sleepRows, weightRows, bodyFatRows, boneRows, leanRows, waterRows, nowMs): DailySummaryEntity
- class LocalHealthReader(dao: HealthDao) {
  suspend fun stepsDaily(days: Int = 7): List<Pair<LocalDate, Long>>
  suspend fun heartRateToday(now: Instant = Instant.now()): HeartRateSummary
  suspend fun sleep(days: Int = 7): SleepSummary
  suspend fun weight(days: Int = 30): WeightSummary
  suspend fun body(metric: BodyMetric, days: Int = 30): MetricSummary
  fun lastSyncFlow(): Flow<Long?> }
