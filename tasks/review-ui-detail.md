# 검수 (중간, 최소, 코드 수정 금지)
명세: tasks/ui-fix3.md, tasks/ui-detail.md. git diff로 변경 확인 가능 (`git diff`).
1. 동기화/가져오기 로직 변경 없음 (git diff로 StepsViewModel/SyncRepository/SamsungCsvParser 등 확인)
2. 상세 화면이 DB만 읽고 Health Connect 호출 없음
3. 상세 진입/뒤로가기(아이콘+시스템) 동작, 빈 데이터·90일에서 크래시 가능성(0 나누기, 빈 리스트 max 등)
4. assembleDebug, assembleDiag 성공
디자인 취향 지적 금지. 결과: tasks/review-ui-detail-result.md 첫 줄 PASS/FAIL, 6줄 이내. FAIL은 실제 상황 한 줄.
