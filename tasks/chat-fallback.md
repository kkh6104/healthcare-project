# 구현 요청 (codex-1): Gemini 503 대비 재시도·모델 폴백
실측: gemini-flash-latest → HTTP 503 UNAVAILABLE("high demand") 연속. gemini-flash-lite-latest → 정상 응답.
- GeminiConfig: `MODELS = listOf("gemini-flash-latest", "gemini-flash-lite-latest")`.
- GeminiClient: 503/500/502/504 이면 같은 모델 1회 재시도(1.5초 대기) → 그래도 실패면 다음 모델로. 한 질문(도구 루프 포함) 안에서 폴백한 모델은 그 질문 끝까지 유지. 모든 모델 실패 시 ChatError 새 항목 `Busy`("AI 서버가 붐벼요. 잠시 후 다시 시도해 주세요").
- 400 + API_KEY_INVALID 는 Other("API 키가 올바르지 않습니다")로.
- 로그에 사용 모델 이름 추가(`model=...`). 키·내용은 로그 금지.
- ChatError.Busy 추가 시 ui/chat 의 오류 표시(when 분기)도 함께 수정(이번엔 ui/chat 수정 허용, codex-2는 작업 없음).
- 테스트: 503→폴백, 모두 실패→Busy, API_KEY_INVALID 분류.
- assembleDebug testDebugUnitTest 성공 후 tasks/chat-fallback-done.md 1~3줄.
