# 검수 (중간, 최소 로직 검사, 코드 수정 금지)
명세: tasks/step3-design.md, step3-db.md, step3-sync.md, step3-local.md, step3-integrate.md
핵심만:
1. 동기화 범위(첫 30일 / 이후 syncedUntil-1일)와 upsert가 externalId 기준으로 중복 없이 덮어쓰는지 (REPLACE + UNIQUE 인덱스)
2. 레코드 조회가 전부 readAllPages(maxPages 5)인지, 직접 pageToken 루프 없는지
3. 순차 동기화, 한도 초과 시 즉시 중단+쿨다운, 실패해도 성공분 요약 재계산
4. 화면이 DB에서만 읽고, 앱 시작 시 DB 내용 즉시 표시
5. 걸음 하루 합계가 원본 합산이 아니라 aggregateGroupByPeriod 결과인지
6. assembleDebug, assembleDiag 성공
사소한 지적 금지. 결과: tasks/review-step3-result.md 첫 줄 PASS/FAIL, 10줄 이내. FAIL 항목은 실제로 생기는 상황 한 줄.
