PASS
1. 자동 일시정지/재개: MotionMonitor 10초 걸음0+이동<5m / 3초 3걸음 또는 8m, AUTO_PAUSED 중 runningSince=0·거리 미누적 확인
2. 탈것: 20초 걸음<5+≥15m → 감지 구간 거리·시간 소급 제외, 해제 시 resetAnchor·history 초기화 확인
3. 음성: 이어폰 기기 검사, 포커스 onDone/onError/실패 시 반납, 15초 중복 방지, 초기화 실패 로그만, onDestroy shutdown 확인
4. 겹침: 지도 weight(1f)+clipToBounds, 통계는 지도 밖 Column / 상세는 320dp 지도 아래 정보+verticalScroll
5. DB 스키마/data 변경 없음, assembleDebug·testDebugUnitTest BUILD SUCCESSFUL
