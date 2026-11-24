# fix-sleep-history 완료
- SleepDetail.kt의 SleepHistoryDetail을 DetailReader.sleepDays(days)의 SleepDay 기준으로 변경: 목록은 날짜당 1줄(합산 시간, 2회 이상이면 "N회 ·" 표시, 첫 시작~마지막 끝, 단계 합 요약), 평균은 기록 있는 날의 합산 평균, 가장 길었던/짧았던 날도 날짜 합산 기준. 그래프는 그대로.
- 수정한 파일은 수면 상세 파일(ui/detail/SleepDetail.kt) 하나뿐. 빌드: assembleDebug BUILD SUCCESSFUL (실기기 미확인)
