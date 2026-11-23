# 검수 (최소, 코드 수정 금지): tasks/fix-ratelimit2.md 기준 핵심만 확인.
1. refresh에서 걸음 진단 호출이 빠지고 버튼으로만 조회되는지
2. 권한 확인 생략 + SecurityException 시 NeedPermission 복귀
3. 실패 시 보관 데이터 유지 + 배너, 보관 데이터 없을 때만 Error 화면
4. 한도 초과 15분 쿨다운 동안 refresh/refreshIfStale/버튼이 조회하지 않는지
5. assembleDebug 성공 여부
사소한 방어 코드 깊이 같은 건 지적하지 말 것. 결과: tasks/review-fix-ratelimit2-result.md 첫 줄 PASS/FAIL, 8줄 이내.
