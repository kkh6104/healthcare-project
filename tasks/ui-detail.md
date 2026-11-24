# 구현 요청 (claude-1): 카드 터치 → 상세 화면
## 공통
- 데이터 동기화/가져오기 로직은 바꾸지 않는다. 외부 라이브러리 추가 금지. MaterialTheme 색만 사용(하드코딩 색 금지). 한국어, Locale.KOREAN.
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug` (동시 빌드 lock 시 30초 후 재시도)

- **ui/charts/ 폴더는 claude-2가 동시에 수정 중이니 건드리지 말 것.** 차트는 기존 API 그대로 사용. LineChart에 `minYSpan` 파라미터가 곧 추가되므로, 몸 탭/상세 화면의 체중 LineChart 호출에 `minYSpan = 6f`, 체지방률에 `10f`를 넣는 건 **마지막에** 빌드 확인하며 반영(아직 없으면 빌드 에러 → claude-2 완료 후 재시도, 최대 5분 대기).

## 구조
- MainActivity: 상세 화면 상태 `detail: DetailKind?`(rememberSaveable, enum으로 저장). null이 아니면 탭 대신 상세 화면 표시, TopAppBar 뒤로가기 아이콘 + 시스템 뒤로가기(BackHandler)로 복귀. 하단 탭바는 숨김.
- `enum class DetailKind { STEPS, HEART_RATE, SLEEP_LAST_NIGHT, SLEEP_HISTORY, WEIGHT, BODY_COMPOSITION }`
- 활동/수면/몸 탭의 각 카드에 `Modifier.clickable` + 카드 오른쪽 위 작은 "자세히 ›" 텍스트. 탭 시그니처에 `onOpenDetail: (DetailKind) -> Unit` 추가.

## 데이터 (새 파일 data/DetailReader.kt, LocalHealthReader 수정 금지 — 필요한 건 DAO에 쿼리 추가 가능)
- `suspend fun stepsDaily(days: Int): List<Pair<LocalDate, Long>>` (daily_summary)
- `suspend fun heartRateHourly(date: LocalDate): List<Triple<Int /*시*/, Double /*평균*/, Pair<Double,Double> /*최저,최고*/>>` — 그날 HEART_RATE 행을 시간별로(출처 우선순위: HC 행 있으면 HC만)
- `suspend fun hrDailyAvg(days: Int): List<Pair<LocalDate, Double?>>` (daily_summary)
- `suspend fun sleepNights(days: Int): List<SleepNight>` (기존 SleepNight 클래스, 최신순)
- `suspend fun metricHistory(type: String, days: Int): List<Pair<Instant, Double>>` (WEIGHT/BODY_FAT 등, 날짜별 마지막 측정, HC 우선)
- ViewModel에 상세 화면용 suspend 로딩 함수 또는 Composable 내 LaunchedEffect + manager 접근 중 단순한 쪽 선택. DB 읽기만, Health Connect 호출 금지.

## 상세 화면 (새 파일 ui/detail/*.kt)
- 상단에 기간 선택 칩: 7일 / 30일 / 90일 (기본 30일). 
1. STEPS: BarChart(기간, 7일은 요일 라벨, 30/90일은 라벨 듬성듬성 "11/1"), 평균·최고·목표(6000) 달성일 수, 아래 날짜별 목록(최신순).
2. HEART_RATE: 날짜 선택(◀ 오늘 ▶) + 그날 시간별 평균 LineChart(x=시) + 최저/최고/평균, 아래 기간 칩으로 일별 평균 LineChart.
3. SLEEP_LAST_NIGHT: 어젯밤 시작~끝, 총 시간, 단계 StackedBar + 단계별 시간 목록(깊은/얕은/렘/깸 각 N시간 M분과 %).
4. SLEEP_HISTORY: 기간 BarChart(시간 단위) + 평균, 가장 길었던/짧았던 날, 날짜별 목록(시작~끝, 시간, 단계 요약).
5. WEIGHT: 기간 LineChart + 최저/최고/평균/기간 내 변화 + 측정 목록(날짜, 값, 전 측정 대비 변화).
6. BODY_COMPOSITION: 지표 칩(체지방률/골질량/제지방량/체내 수분량) 선택 + 기간 LineChart + 측정 목록.
- 데이터 없으면 "기록이 없습니다".

## 완료
- assembleDebug assembleDiag 성공, tasks/ui-detail-done.md 에 3~5줄
