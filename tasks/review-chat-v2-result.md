PASS
- SSE: readLine 기반이라 청크 경계 무관, 빈 줄마다 flush·EOF 시 마지막 이벤트 flush·[DONE] 무시(누락/중복 없음), 실패 시 finally에서 streaming=null
- 타임아웃은 재시도 없이 다음 모델, 5xx 1회 재시도, 기본 모델 붐빔 시 5분 쿨다운, thinking 400은 요청당 1회만 재시도 → 모두 유한(모델 2개×2회, 도구 왕복 5회)
- resend는 insert 없이 기존 id 사용, 이미 답이 있으면 거부 → 사용자 메시지 중복 없음
- 키패드: Scaffold에서 consumeWindowInsets(innerPadding) 후 채팅 루트 imePadding 1회만 적용(이중 적용 없음), 길게 눌러 복사+토스트 동작
- 로그는 model/tools/ms/firstTokenMs/stream/result만(키·내용 없음), assembleDebug·testDebugUnitTest BUILD SUCCESSFUL
