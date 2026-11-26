FAIL
- 시작 안내가 실제로 나오지 않음: startSession에서 WalkVoice 생성 직후 say() 호출 → TTS 초기화(비동기) 전이라 ready=false로 매번 "skipped(not ready)", 이어폰 연결해도 "걷기를 시작합니다" 무음
- 거리 10m 단위가 반올림이 아닌 버림: 995m→"990미터"(명세는 반올림, 999m는 1000m/1킬로미터여야 함), 테스트도 990으로 고정
- 그 외 OK: 종료는 DB 저장 후 최대 6초 대기→stopSelf/onDestroy shutdown, 이어폰 없음·스위치 꺼짐은 대기 없음, restore 마감 무음, 시간 포맷 경계, assembleDebug·testDebugUnitTest BUILD SUCCESSFUL
