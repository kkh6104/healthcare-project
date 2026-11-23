# 구현 요청 (claude-2): 3단계-2 하루 요약 계산 + DB → 화면 데이터

## 공통
- 설계: tasks/step3-design.md, DB API: tasks/step3-db-done.md (data/ 패키지의 엔티티/DAO 사용, 수정 금지)
- **새 파일만 만든다. 기존 파일 수정 금지** (다른 에이전트 동시 작업 중, 연결은 다음 단계). 
- 시간대 ZoneId.systemDefault(). 날짜 문자열 "yyyy-MM-dd".
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug` (동시 빌드 lock 시 30초 후 재시도)

## 새 파일 1: app/src/main/java/com/gh/healthapp/data/DailySummaryBuilder.kt
- `class DailySummaryBuilder(private val dao: HealthDao)`
  - `suspend fun rebuild(dates: Set<LocalDate>, stepsByDate: Map<LocalDate, Long>)` — 각 날짜마다 DB 원본으로 계산해 dao.upsertSummaries:
    - steps: stepsByDate에 있으면 그 값, 없으면 기존 summary의 steps 유지
    - hrAvg/hrMin/hrMax: 그날 0시~다음날 0시 HEART_RATE 행들의 평균/최소/최대 (없으면 기존 값 유지가 아니라 null)
    - sleepMinutes: SLEEP_SESSION 중 **endTime이 그날**인 행들의 value 합 (없으면 null)
    - weightKg, bodyFatPct, boneMassKg, leanMassKg, bodyWaterKg: 그날 가장 늦은 측정 (없으면 null)
    - updatedAt = 현재 ms
  - 계산은 internal 순수 함수로 분리(행 목록 → DailySummaryEntity).
  - HEART_RATE 기간 조회는 recordsBetween(type, 0시 ms, 다음날 0시 ms). SLEEP은 endTime 기준이므로 (그날 0시 - 1일) ~ 다음날 0시로 넉넉히 읽은 뒤 endTime으로 거르기.

## 새 파일 2: app/src/main/java/com/gh/healthapp/data/LocalHealthReader.kt
기존 카드(Composable)를 그대로 쓰기 위해, DB 데이터를 **기존 화면용 데이터 클래스**로 바꿔 준다.
- `class LocalHealthReader(private val dao: HealthDao)`
  - `suspend fun stepsDaily(days: Int = 7): List<Pair<LocalDate, Long>>` — daily_summary 기반, 오늘부터 최신순, 없으면 0
  - `suspend fun heartRateToday(now: Instant = Instant.now()): HeartRateSummary` — 오늘 HEART_RATE 행으로 avg/min/max(Long 반올림)와 최근 샘플(start<=now)
  - `suspend fun sleep(days: Int = 7): SleepSummary` — SLEEP_SESSION 행으로 기존 SleepSummary/SleepNight 구성 (extraJson의 단계 분 사용, start/end Instant, asleep=value분). lastNight = 최근 36시간 안에 끝난 가장 늦은 것.
  - `suspend fun weight(days: Int = 30): WeightSummary` — 기존 internal `buildWeightSummary(entries)` 재사용
  - `suspend fun body(metric: BodyMetric, days: Int = 30): MetricSummary` — 기존 internal `buildMetricSummary(metric, entries)` 재사용 (BodyMetric ↔ RecordType 매핑)
  - `fun lastSyncFlow(): Flow<Long?>` — sync_state의 syncedUntil 최댓값
- 기존 데이터 클래스(HeartRateSummary, SleepSummary, SleepNight, WeightSummary, WeightEntry, MetricSummary, MetricEntry, BodyMetric)의 정의를 읽고 그대로 사용 (수정 금지).

## 완료
- assembleDebug 성공, tasks/step3-local-done.md 에 요약 + 공개 API
