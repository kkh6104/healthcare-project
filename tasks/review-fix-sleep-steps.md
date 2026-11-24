# 검수 (최소, 코드 수정 금지): tasks/fix-steps-chart.md, tasks/fix-sleep-daily.md 기준. git diff로 확인.
1. BarChart가 30/90개 막대에서 폭 0/음수 없이 그려지는지
2. 수면일 = 종료 시각 날짜, 같은 날 세션 합산(시간·단계), 오늘 없으면 어제 표시
3. 동기화/가져오기 로직 변경 없음
4. assembleDebug 성공
결과: tasks/review-fix-sleep-steps-result.md 첫 줄 PASS/FAIL, 5줄 이내. FAIL은 실제 상황 한 줄.
