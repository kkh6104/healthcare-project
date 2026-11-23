# step2b-sleep 완료
- 새 파일 2개만 생성(기존 파일 수정 없음): SleepRepository.kt, SleepSection.kt.
- SleepRepository: SleepSessionRecord를 (오늘-days일 0시 ~ 지금) 범위로 읽고, 깬 날짜(endTime) 기준으로 집계. 계산은 HealthConnectClient와 분리된 internal 순수 함수.
- SleepCard: 지난밤 요약(시간/구간/단계) + 최근 7일 목록. 데이터 클래스만 받는 순수 UI.
- 빌드: assembleDebug BUILD SUCCESSFUL (Kotlin 컴파일은 동시 빌드로 UP-TO-DATE 처리됨)

## 공개 API
- data class SleepNight(wakeDate: LocalDate, start: Instant, end: Instant, asleep: Duration, deep: Duration?, light: Duration?, rem: Duration?, awake: Duration?)
- data class SleepSummary(lastNight: SleepNight?, recent: List<Pair<LocalDate, Duration>>)
- class SleepRepository(client: HealthConnectClient) { suspend fun summary(days: Int = 7): SleepSummary }
- internal fun toSleepNight(record: SleepSessionRecord, zone: ZoneId): SleepNight
- internal fun buildSleepSummary(sessions: List<SleepSessionRecord>, days: Int, now: Instant, zone: ZoneId): SleepSummary
- fun formatDuration(d: Duration): String
- @Composable fun SleepCard(summary: SleepSummary, modifier: Modifier = Modifier)

## 다음 단계 참고
- READ_SLEEP 권한(매니페스트 + PERMISSIONS)은 아직 추가하지 않음.
