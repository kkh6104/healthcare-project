# 검수 (최소, 코드 수정 금지)
명세: tasks/step2c-body.md, tasks/step2c-logging.md, tasks/step2c-integrate.md
1. 체성분 레코드 조회가 모두 readAllPages(maxPages 5)를 쓰고 직접 pageToken 루프가 없는지
2. 플래그: diag만 true, debug/release false. 체성분 권한이 diag 매니페스트와 (플래그 true일 때) PERMISSIONS에만 들어가는지
3. 순차 로딩: 몸무게 뒤 4종이 하나씩, 한도 초과 시 즉시 중단+skipped+쿨다운
4. 로그: refresh start/end(reason, calls), section start/끝, hcCall begin, range 로그가 있는지
5. assembleDebug, assembleDiag 성공
결과: tasks/review-step2c-result.md 첫 줄 PASS/FAIL, 8줄 이내. FAIL 항목은 실제로 생기는 상황 한 줄 포함.
