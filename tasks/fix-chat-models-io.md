# 구현 요청 (codex-1): 모델 목록 조회 메인 스레드 네트워크 수정
검수: tasks/review-chat-free-speed-result.md — cachedModels가 withContext(IO) 없이 HttpURLConnection 호출 → NetworkOnMainThreadException → 매번 고정 목록, 캐시 미저장.
- 모델 목록 조회와 캐시 저장을 withContext(Dispatchers.IO)로. ChatRepository.send/resend 진입부 전체를 IO에서 실행하도록 보장(다른 네트워크 호출도 같은 문제 없는지 점검).
- 실패 로그에 예외 클래스명 추가: `chat models fetch fail ex=..`, 성공 시 `chat models fetched n=..`.
- 테스트: 가능하면 디스패처 주입으로 IO 사용 확인(어려우면 생략하고 코드상 보장).
chat/ 와 테스트만. assembleDebug testDebugUnitTest 성공 후 tasks/fix-chat-models-io-done.md 1~2줄.
