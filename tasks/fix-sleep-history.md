# 구현 요청 (claude-1): 수면 기록 상세도 하루 합산
검수 지적: 수면 기록 상세(SLEEP_HISTORY)의 날짜별 목록·평균·가장 길었던/짧았던 날이 세션 단위로 계산됨 (SleepDetail.kt 부근).
→ DetailReader.sleepDays(days)의 SleepDay 기준으로 바꾼다: 목록은 날짜당 1줄(합산 시간, 세션 수 "2회", 첫 시작~마지막 끝, 단계 요약), 평균은 기록 있는 날의 합산 평균, 최장/최단도 날짜 합산 기준. 그래프는 이미 합산이니 유지.
수정 허용: 수면 기록 상세 화면 파일만. 빌드 assembleDebug (JAVA_HOME = Android Studio jbr). 완료: tasks/fix-sleep-history-done.md 1~2줄.
