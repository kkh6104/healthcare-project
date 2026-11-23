# 검수 (최소, 코드 수정 금지): tasks/diag-ratelimit.md 기준
1. 모든 Health Connect 호출이 hcCall로 감싸졌는지 (Sleep/Weight Repository 포함, 누락 호출 있으면 파일:줄)
2. 섹션 순차 로딩, 일반 실패 시 다음 섹션 계속, 한도 초과 시 즉시 중단+쿨다운
3. assembleDebug 성공
사소한 지적 금지. 결과: tasks/review-diag-ratelimit-result.md 첫 줄 PASS/FAIL, 6줄 이내.
