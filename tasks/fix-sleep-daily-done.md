# fix-sleep-daily 완료
- SleepRepository.kt: SleepDay(date, sessions, asleep, deep/light/rem/awake, firstStart, lastEnd) 추가, 수면일=세션 종료 날짜 기준 합산(groupSleepDays), 오늘 → 없으면 어제를 고르는 pickDisplayDay. SleepSummary에 day 필드(기본값 null) 추가. LocalHealthReader.sleep()이 day를 채움. DetailReader.sleepDays(days) 추가.
- 수면 탭 카드는 "오늘 수면"(없으면 "어제 수면"), 세션 2개 이상이면 "N회 나눠 잠 · 첫 시작~마지막 끝". 홈 타일도 같은 값("오늘 수면"/"어제 수면" 부제). SLEEP_LAST_NIGHT 상세는 "수면 상세"로 바뀌어 날짜 ◀ ▶(최대 90일 전), 합산 시간·단계 StackedBar·단계별 시간/% + 세션 목록(시작~끝, 시간, 단계 요약).
- 확인: 7일 그래프(recent)와 수면 기록 상세의 막대 그래프/통계는 이미 종료 날짜별 합산이라 그대로 유지. 다만 수면 기록 상세의 날짜별 목록은 세션 단위 그대로(하루 합산 아님). daily_summary.sleepMinutes도 endTime 날짜 합 규칙이라 일치. 동기화/가져오기 로직, BarChart.kt, StepsDetail.kt는 수정하지 않음.
- 빌드: assembleDebug BUILD SUCCESSFUL (실기기 미확인)
