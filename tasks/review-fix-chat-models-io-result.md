PASS
- send/resend 전체가 withContext(Dispatchers.IO), ModelCatalog.current도 IO에서 조회·캐시 저장 → 메인 스레드 네트워크 없음(임베딩·생성 호출도 IO 안)
- 로그 "chat models fetched n=.." / "chat models fetch fail ex=클래스명"만(키 없음), assembleDebug·testDebugUnitTest BUILD SUCCESSFUL
