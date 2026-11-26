PASS
1. 1km: lastAnnouncedKm 기준 km당 1회(2km 동시 통과 시 마지막 km만), RUNNING만 안내(일시정지·자동일시정지·탈것 제외), kmAnnounce+voice+이어폰 조건, 15초 규칙과 분리
2. 카운트다운: WalkTracker 싱글턴 코루틴이라 회전/재구성 무관, 재탭은 기존 취소 후 재시작, 취소 시 start 안 함·IDLE, 끝나면 start 1회
3. 심박 코드 추가 없음 / assembleDebug·testDebugUnitTest BUILD SUCCESSFUL
참고(UI 미완, walk-km-countdown-ui-done.md 없음): 전체 화면 오버레이·애니메이션·뒤로가기 취소·"1km마다 음성 안내" 스위치 아직 없음
