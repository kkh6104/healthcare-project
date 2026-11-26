FAIL
- 모델 목록 자동 확인이 실제로 동작 안 함: ChatTab의 rememberCoroutineScope(Main)에서 send→sendLocked→cachedModels가 withContext(IO) 없이 HttpURLConnection 호출 → NetworkOnMainThreadException → catch로 매번 고정 목록 사용, 캐시도 저장 안 됨(필터·정렬 함수 자체는 맞음)
- 그 외 OK: 실패 시 고정 목록 동작, 성공 기반 순서는 최대 5회·쿨다운과 충돌 없음, k=3·0.6·400자·7일 CSV·60일 초과 주 단위·6턴·maxOutputTokens 1024 반영
- 키·내용 로그 없음(model order만), assembleDebug·testDebugUnitTest BUILD SUCCESSFUL
