# 구현 요청 (claude-2): 몸무게 데이터 모듈

## 공통 규칙
- **아래 '만들 파일'만 새로 만든다. 기존 파일(HealthConnectManager.kt, StepsViewModel.kt, MainActivity.kt, AndroidManifest.xml 등)은 절대 수정하지 않는다.** 다른 에이전트가 동시에 작업 중이며, 연결(권한·ViewModel·화면 배치)은 다음 단계에서 따로 한다.
- 패키지: com.gh.healthapp. Health Connect SDK 1.1.0 (androidx.health.connect:connect-client).
- Repository는 생성자로 `HealthConnectClient`를 받는다. 시간대는 ZoneId.systemDefault(), 문구는 한국어, 시각 포맷은 Locale.KOREAN.
- Composable은 데이터 클래스만 받아 그리는 순수 UI (Card + Column, MaterialTheme 타이포그래피 사용, 기존 걸음/심박 카드와 비슷한 모양).
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug`. 다른 에이전트와 동시에 빌드하다 lock/timeout 오류가 나면 30초 기다렸다 재시도.

## 만들 파일
- app/src/main/java/com/gh/healthapp/WeightRepository.kt
- app/src/main/java/com/gh/healthapp/WeightSection.kt

## WeightRepository.kt
- `data class WeightEntry(val date: LocalDate, val time: Instant, val kg: Double)`
- `data class WeightSummary(val latest: WeightEntry?, val changeKg: Double?, val recent: List<WeightEntry>)`
- `class WeightRepository(private val client: HealthConnectClient)`
  - `suspend fun summary(days: Int = 30): WeightSummary`
  - readRecords(WeightRecord) 범위: (오늘 - days일) 0시 ~ 지금. pageSize 1000.
  - kg = record.weight.inKilograms
  - recent: 날짜(기기 시간대)별로 그날 **가장 늦은 측정 1건**만, 최신 날짜 먼저.
  - latest: recent의 첫 항목 (없으면 null)
  - changeKg: latest와, latest 날짜로부터 7일 이상 이전 항목 중 가장 최근 것의 차이(latest - 그것). 없으면 null.
  - 계산 로직은 HealthConnectClient 호출과 분리된 internal 순수 함수로.

## WeightSection.kt
- `@Composable fun WeightCard(summary: WeightSummary, modifier: Modifier = Modifier)`
  - 제목 "몸무게"
  - latest 있으면: 큰 글씨 "72.4 kg" (소수 1자리) / 작은 글씨 측정 시각 "11월 23일 오전 7:10" / changeKg 있으면 "1주 전보다 -0.6 kg" (+/− 부호 표시, 0.0이면 "변화 없음")
  - 없으면 "최근 30일 몸무게 기록이 없습니다."
  - 아래에 최근 기록 최대 7개 줄 목록: "11월 23일 (월)   72.4 kg"

## 완료 조건
- assembleDebug 성공, tasks/step2b-weight-done.md 에 요약 3~5줄 + 공개 API(클래스/함수 시그니처) 목록.
