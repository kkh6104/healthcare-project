# 검수 (최소, 코드 수정 금지): tasks/fix-weight-loop.md 기준
1. readAllPages가 null/빈 토큰, 같은 토큰 반복, maxPages 세 조건 모두에서 종료하는지
2. 프로젝트 전체에서 pageToken 루프가 readAllPages 외에 남아 있지 않은지 (rg pageToken)
3. assembleDebug 성공
결과: tasks/review-fix-weight-loop-result.md 첫 줄 PASS/FAIL, 5줄 이내. FAIL이면 실제로 무한 반복/과다 호출이 가능한지 한 줄로 근거.
