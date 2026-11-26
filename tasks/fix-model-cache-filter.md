# 구현 요청 (codex-1): 캐시된 모델 목록에도 필터 적용
실기기 로그: `chat model order=[gemini-flash-lite-latest, gemini-flash-latest, gemini-omni-flash-preview, gemini-omni-1.2-flash]` — omni 제외 필터는 새로 조회할 때만 적용, 이전에 저장된 캐시는 그대로 사용 중.
- SharedPreferences 캐시에서 읽은 목록에도 동일한 후보 필터·정렬 함수를 적용. 필터 버전 상수를 캐시에 저장하고 버전이 다르면 캐시 무시 후 재조회.
- 테스트: omni 포함 캐시 → 필터된 순서.
chat/ 와 테스트만. assembleDebug testDebugUnitTest 성공 후 tasks/fix-model-cache-filter-done.md 1줄.
