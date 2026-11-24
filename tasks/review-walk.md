# 검수 (중간, 최소, 코드 수정 금지)
명세: tasks/walk-design.md, walk-core.md, walk-map.md, walk-ui.md. git diff로 확인.
1. **MIGRATION_1_2가 Room이 기대하는 스키마와 일치하는지** (엔티티 컬럼 타입/NOT NULL/기본값/인덱스/FK 대조) — 불일치면 기존 사용자 앱이 시작 시 크래시하므로 가장 중요
2. 포그라운드 서비스: Android 10(API 29)~14+에서 startForeground 타입/권한 조합이 맞는지, 위치 권한 없이 서비스 시작 시 크래시 없는지
3. 점 필터·거리·일시정지 처리, 종료 시 세션 DONE + walkDistanceM 갱신
4. 기존 동기화/가져오기 로직 변경 없음
5. assembleDebug, assembleDiag, testDebugUnitTest 성공
사소한 지적 금지. 결과: tasks/review-walk-result.md 첫 줄 PASS/FAIL, 8줄 이내. FAIL은 실제 상황 한 줄.
