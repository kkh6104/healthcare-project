# 걷기 v2 로직 완료
- 자동 일시정지·재개, 탈것 감지와 거리/활동시간 제외, 정확도별 거리 기준 및 이어폰 음성 안내를 추가했다.
- 공개 API: WalkStatus.AUTO_PAUSED, WalkLiveState.inVehicle, WalkSettings(context).state/getAutoPause/getVoice/setAutoPause/setVoice.
- assembleDebug testDebugUnitTest 성공. 센서·음성·실제 이동 동작은 실기기 검증 필요.
