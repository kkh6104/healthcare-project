# 구현 요청 (claude-1): 수면을 하루 합산으로

## 현재 문제 (실기기)
수면 카드("어젯밤")와 홈 타일, 수면 상세가 **마지막 세션 1개**(예: 58분짜리)만 보여 줌. 밤잠이 여러 세션으로 나뉘어 기록되기 때문.

## 규칙 (사용자 결정)
- 수면 세션의 "수면일" = **세션 종료 시각의 날짜**(기기 시간대). 즉 자정(0시)을 넘긴 세션은 다음날 수면으로 집계. 자정을 넘기지 않은 세션은 그날.
- 화면의 기본 단위는 **하루 합산**: 같은 수면일의 모든 세션 합.

## 수정
- 새 데이터 클래스 `SleepDay(date: LocalDate, sessions: List<SleepNight>, asleep: Duration, deep/light/rem/awake: Duration?, firstStart: Instant, lastEnd: Instant)` (단계는 세션들의 합, 단계 정보가 있는 세션만 합산하고 하나도 없으면 null).
- "어젯밤" 카드 → **"오늘 수면"** (오늘 수면일 합산). 오늘 기록이 없으면 어제 수면일을 "어제 수면"으로 표시. 세션이 2개 이상이면 "N회 나눠 잠" 표시 + 첫 시작~마지막 끝.
- 홈 타일 수면 = 같은 값.
- SLEEP_LAST_NIGHT 상세 → "수면 상세": 날짜 ◀ ▶ 이동, 그날 합산 시간·단계 StackedBar·단계별 시간 + **세션 목록**(시작~끝, 시간, 단계 요약).
- 7일/기간 그래프와 목록은 이미 날짜 합산인지 확인, 아니면 SleepDay 기준으로.
- daily_summary.sleepMinutes 규칙(endTime 날짜 합)은 이미 같은 규칙이니 유지, 일치하는지만 확인.
- 수정 허용: SleepRepository.kt(LocalHealthReader가 쓰는 부분), data/LocalHealthReader.kt, data/DetailReader.kt, ui/SleepTab.kt, ui/HomeTab.kt, ui/detail/SleepLastNightDetail.kt, ui/detail/SleepHistoryDetail.kt, StepsViewModel.kt(Loaded 필드 필요 시). **ui/charts/BarChart.kt, ui/detail/StepsDetail.kt는 claude-2 수정 중이니 금지.** 동기화/가져오기 로직 변경 금지.
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug` (lock 시 30초 후 재시도)
- 완료: tasks/fix-sleep-daily-done.md 에 3~4줄
