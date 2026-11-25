# 챗봇 엔진 완료
- Gemini REST 함수 호출 루프, 최근 7일 요약과 5개 DB 조회 도구, 동의 및 오류 처리를 구현했다. 실제 API는 호출하지 않았다.
- DB v3 chat_message와 MIGRATION_2_3을 추가했으며 Room 생성 스키마와 대조했다.
- 공개 API: ChatRepository(context).messages/send/clear/hasApiKey, ChatMessage, ChatError, ChatException.error, ChatConsent.isGiven/set, GeminiConfig.MODEL/ENDPOINT.
- assembleDebug testDebugUnitTest 성공; 요청 JSON, 응답 및 오류 분류, 날짜 상한을 네트워크 없이 검증했다.
