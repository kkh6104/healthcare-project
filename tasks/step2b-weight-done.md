# step2b-weight 완료
- WeightRepository.kt, WeightSection.kt 두 파일만 새로 추가 (기존 파일 수정 없음).
- Health Connect WeightRecord를 최근 N일(기본 30) 읽어 날짜별 가장 늦은 측정 1건으로 요약, 1주 이상 이전 기록 대비 변화량 계산.
- 계산 로직은 internal 순수 함수 buildWeightSummary로 분리.
- WeightCard: 최신 몸무게/측정 시각/1주 변화/최근 7개 목록, 기록 없으면 안내 문구.
- assembleDebug 성공. 권한/ViewModel/화면 연결은 미구현(다음 단계).

## 공개 API
- data class WeightEntry(date: LocalDate, time: Instant, kg: Double)
- data class WeightSummary(latest: WeightEntry?, changeKg: Double?, recent: List<WeightEntry>)
- class WeightRepository(client: HealthConnectClient) { suspend fun summary(days: Int = 30): WeightSummary }
- internal fun buildWeightSummary(entries: List<WeightEntry>): WeightSummary
- @Composable fun WeightCard(summary: WeightSummary, modifier: Modifier = Modifier)
