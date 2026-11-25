# 채팅 속도 개선 완료
- SSE 스트리밍과 생각 설정 재시도, 타임아웃 즉시 폴백 및 기본 모델 5분 쿨다운을 구현했다.
- 공개 계약: ChatRepository.streaming: StateFlow<String?> 및 resend(userMessageId): Result<Unit>; 완료·실패 시 스트림을 초기화한다.
- SSE·타임아웃·쿨다운·생각 설정 테스트와 assembleDebug testDebugUnitTest 성공. 실제 API 호출은 수행하지 않았다.
