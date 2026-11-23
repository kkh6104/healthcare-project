# 구현 요청 (claude-2): 체성분 4종 모듈

## 공통 규칙
- **새 파일 2개만 만든다. 기존 파일은 절대 수정하지 않는다.** (claude-1이 동시에 HcLog.kt/StepsViewModel.kt 등을 수정 중. 연결은 다음 단계에서 따로 함)
- Health Connect SDK 1.1.0. 시간대 ZoneId.systemDefault(), 한국어, Locale.KOREAN.
- 레코드 조회는 반드시 기존 `HealthConnectClient.readAllPages(name, maxPages = 5, request)` (HcLog.kt)를 사용. 직접 pageToken 루프 금지.
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug`. 동시 빌드 lock 오류 시 30초 후 재시도. 다른 에이전트 수정 때문에 컴파일이 깨지면 그 사실만 기록.

## 만들 파일
- app/src/main/java/com/gh/healthapp/BodyCompositionRepository.kt
- app/src/main/java/com/gh/healthapp/BodyCompositionSection.kt

## BodyCompositionRepository.kt
- `enum class BodyMetric(val label: String, val unit: String) { BODY_FAT("체지방률", "%"), BONE_MASS("골질량", "kg"), LEAN_BODY_MASS("제지방량", "kg"), BODY_WATER_MASS("체내 수분량", "kg") }`
- `data class MetricEntry(val date: LocalDate, val time: Instant, val value: Double)`
- `data class MetricSummary(val metric: BodyMetric, val latest: MetricEntry?, val changeFromWeekAgo: Double?, val recent: List<MetricEntry>)`
- `class BodyCompositionRepository(private val client: HealthConnectClient)`
  - `suspend fun summary(metric: BodyMetric, days: Int = 30): MetricSummary` — **한 번에 한 종류만** 조회 (순차 호출은 ViewModel에서).
  - 레코드/값: BodyFatRecord.percentage.value, BoneMassRecord.mass.inKilograms, LeanBodyMassRecord.mass.inKilograms, BodyWaterMassRecord.mass.inKilograms
  - readAllPages name: "readRecords(bodyFat)", "readRecords(boneMass)", "readRecords(leanBodyMass)", "readRecords(bodyWaterMass)"
  - 범위: (오늘 - days일) 0시 ~ 지금 (종료 시각은 한 번만 계산), pageSize 1000
  - 날짜별 가장 늦은 측정 1건, 최신 날짜 먼저. changeFromWeekAgo = latest - (latest 날짜로부터 7일 이상 이전 항목 중 가장 최근). 계산은 internal 순수 함수로 분리.
- `companion object { fun readPermission(metric: BodyMetric): String }` — 각 Record의 HealthPermission.getReadPermission(...)
- `companion object` 에 `fun manifestPermission(metric)` 불필요.

## BodyCompositionSection.kt
- `@Composable fun BodyCompositionCard(summaries: List<MetricSummary>, modifier: Modifier = Modifier)`
  - 제목 "체성분"
  - 지표별 한 줄: "체지방률   24.1 %   (11월 23일)   1주 전보다 -0.5" / 기록 없으면 "체지방률   기록 없음"
  - 값 소수 1자리, 변화는 부호 표시, 0.0이면 "변화 없음"
- 실패/건너뜀 표시는 연결 단계에서 처리하므로 이 카드는 성공 데이터만 받는다.

## 완료
- tasks/step2c-body-done.md 에 요약 3줄 + 공개 API 목록
