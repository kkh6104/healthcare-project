# fix-weight-loop 완료
- HcLog.kt에 HealthConnectClient.readAllPages(name, maxPages = 5, request) 추가. 페이지마다 hcCall로 감싸고, 토큰이 null/빈 문자열이거나 직전과 같으면 종료, maxPages 도달 시 Log.w("HCDEBUG", "[name] maxPages reached").
- WeightRepository의 do-while 루프를 readAllPages로 교체 (조회 종료 시각은 한 번만 계산).
- assembleDebug 성공.

## pageToken 사용처
- WeightRepository.kt 한 곳뿐이었고 교체함. Sleep/진단/심박에는 pageToken 루프 없음(그대로).
