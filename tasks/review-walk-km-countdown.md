# 검수 (최소, 코드 수정 금지): tasks/walk-km-countdown-logic.md, walk-km-countdown-ui.md 기준
1. 1km 안내: km당 1회, 중복·누락 없음, 일시정지/탈것 중 미안내, 이어폰·스위치 조건
2. 카운트다운: 취소 시 서비스 미시작·IDLE, 끝나면 1회만 start, 화면 회전/재구성 시 중복 시작 없음
3. 심박 관련 코드 없음
4. assembleDebug·testDebugUnitTest 성공
결과: tasks/review-walk-km-countdown-result.md 첫 줄 PASS/FAIL, 5줄 이내. FAIL은 실제 상황 한 줄.
