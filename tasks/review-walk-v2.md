# 검수 (중간, 최소 로직 검사) — 코드는 수정하지 말 것
명세: tasks/walk-v2-logic.md, tasks/walk-v2-ui.md. 변경은 `git diff` 로 확인.
1. 자동 일시정지/재개 조건(10초 걸음 0 + 이동<5m / 3초 3걸음 또는 8m)과 그 동안 activeMs·거리 미증가
2. 탈것 감지(20초 걸음<5 + 이동≥15m → 거리·시간 제외, 해제 시 앵커 재설정으로 탈것 거리 미포함)
3. 음성: 이어폰 연결 시에만, 오디오 포커스 반납, 15초 중복 방지, TTS 실패 시 크래시 없음, 서비스 종료 시 shutdown
4. 지도/정보 영역 겹침 해소(지도 밖에 통계 배치)
5. DB 스키마 변경 없음, assembleDebug·testDebugUnitTest 성공
사소한 지적 금지. 결과: tasks/review-walk-v2-result.md 첫 줄 PASS/FAIL, 8줄 이내. FAIL 항목은 실제로 생기는 상황 한 줄.
