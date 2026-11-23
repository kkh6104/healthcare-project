# 구현 요청 (claude-1): 수면 데이터 모듈

## 공통 규칙
- **아래 '만들 파일'만 새로 만든다. 기존 파일(HealthConnectManager.kt, StepsViewModel.kt, MainActivity.kt, AndroidManifest.xml 등)은 절대 수정하지 않는다.** 다른 에이전트가 동시에 작업 중이며, 연결(권한·ViewModel·화면 배치)은 다음 단계에서 따로 한다.
- 패키지: com.gh.healthapp. Health Connect SDK 1.1.0 (androidx.health.connect:connect-client).
- Repository는 생성자로 `HealthConnectClient`를 받는다. 시간대는 ZoneId.systemDefault(), 문구는 한국어, 시각 포맷은 Locale.KOREAN.
- Composable은 데이터 클래스만 받아 그리는 순수 UI (Card + Column, MaterialTheme 타이포그래피 사용, 기존 걸음/심박 카드와 비슷한 모양).
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug`. 다른 에이전트와 동시에 빌드하다 lock/timeout 오류가 나면 30초 기다렸다 재시도.

## 만들 파일
- app/src/main/java/com/gh/healthapp/SleepRepository.kt
- app/src/main/java/com/gh/healthapp/SleepSection.kt

## SleepRepository.kt
- `data class SleepNight(val wakeDate: LocalDate, val start: Instant, val end: Instant, val asleep: Duration, val deep: Duration?, val light: Duration?, val rem: Duration?, val awake: Duration?)`
- `data class SleepSummary(val lastNight: SleepNight?, val recent: List<Pair<LocalDate, Duration>>)`  // recent: 최근 7일, 최신 날짜 먼저, 기록 없으면 Duration.ZERO
- `class SleepRepository(private val client: HealthConnectClient)`
  - `suspend fun summary(days: Int = 7): SleepSummary`
  - readRecords(SleepSessionRecord) 범위: (오늘 - days일) 0시 ~ 지금. pageSize 1000.
  - 각 세션의 날짜 = endTime을 기기 시간대로 바꾼 LocalDate (깬 날짜 기준).
  - asleep = (end - start) 에서 stages 중 STAGE_TYPE_AWAKE, STAGE_TYPE_OUT_OF_BED, STAGE_TYPE_AWAKE_IN_BED 구간 합을 뺀 값. stages가 비어 있으면 (end - start).
  - deep/light/rem/awake = 해당 stage 구간 합. stages가 비어 있으면 모두 null. (light은 STAGE_TYPE_LIGHT, deep은 STAGE_TYPE_DEEP, rem은 STAGE_TYPE_REM, awake는 위 세 깨어있음 타입 합)
  - 같은 날짜에 세션이 여러 개(낮잠 등)면 recent의 그날 값은 asleep 합계.
  - lastNight = endTime이 가장 늦은 세션 1개 (최근 36시간 안에 끝난 것만, 없으면 null).
  - 위 계산 로직은 HealthConnectClient 호출과 분리된 internal 순수 함수로 두어 나중에 테스트 가능하게.

## SleepSection.kt
- `@Composable fun SleepCard(summary: SleepSummary, modifier: Modifier = Modifier)`
  - 제목 "수면"
  - lastNight 있으면: 큰 글씨 "7시간 12분" / 작은 글씨 "오후 11:40 ~ 오전 7:05" / 단계 있으면 "깊은 수면 1시간 10분 · 얕은 수면 4시간 · 렘 1시간 30분 · 깸 20분"
  - 없으면 "최근 수면 기록이 없습니다."
  - 아래에 최근 7일 줄 목록: "11월 23일 (월)   7시간 12분" (0이면 "-")
- Duration → "N시간 M분" 포맷 함수 포함 (0시간이면 "M분").

## 완료 조건
- assembleDebug 성공, tasks/step2b-sleep-done.md 에 요약 3~5줄 + 공개 API(클래스/함수 시그니처) 목록.
