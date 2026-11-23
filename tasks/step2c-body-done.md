# step2c-body 완료
- BodyCompositionRepository.kt, BodyCompositionSection.kt 두 파일만 새로 추가 (기존 파일 수정 없음). assembleDebug 성공.
- 체지방률/골질량/제지방량/체내 수분량을 한 번에 한 종류씩 readAllPages로 조회, 날짜별 마지막 측정 1건과 1주 전 대비 변화 계산 (internal 순수 함수 buildMetricSummary).
- BodyCompositionCard는 성공 데이터만 받아 지표별 한 줄로 표시. 권한/ViewModel 연결은 미구현.

## 공개 API
- enum class BodyMetric(label: String, unit: String) { BODY_FAT, BONE_MASS, LEAN_BODY_MASS, BODY_WATER_MASS }
- data class MetricEntry(date: LocalDate, time: Instant, value: Double)
- data class MetricSummary(metric: BodyMetric, latest: MetricEntry?, changeFromWeekAgo: Double?, recent: List<MetricEntry>)
- class BodyCompositionRepository(client: HealthConnectClient) { suspend fun summary(metric: BodyMetric, days: Int = 30): MetricSummary; companion { fun readPermission(metric: BodyMetric): String } }
- internal fun buildMetricSummary(metric: BodyMetric, entries: List<MetricEntry>): MetricSummary
- @Composable fun BodyCompositionCard(summaries: List<MetricSummary>, modifier: Modifier = Modifier)
