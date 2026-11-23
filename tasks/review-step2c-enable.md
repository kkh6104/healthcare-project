# 검수 (최소, 코드 수정 금지): tasks/step2c-enable.md 기준
1. BODY_COMPOSITION 플래그/분기가 남아 있지 않고, 체성분 권한 4개가 main 매니페스트와 PERMISSIONS에 있는지 (grep BODY_COMPOSITION, READ_BODY_FAT 등)
2. diag 빌드타입은 유지되는지
3. refreshIfStale이 진행 중 작업이 있으면 건너뛰고, refresh()는 기존처럼 취소 후 시작하는지
4. assembleDebug, assembleDiag 성공
결과: tasks/review-step2c-enable-result.md 첫 줄 PASS/FAIL, 6줄 이내. FAIL 항목은 실제로 생기는 상황 한 줄 포함.
