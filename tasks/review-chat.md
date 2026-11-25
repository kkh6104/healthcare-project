# 검수 (중간, 최소 로직 검사) — 코드는 수정하지 말 것
명세: tasks/chat-design.md, chat-core.md, chat-ui.md. 변경은 `git diff` 와 새 파일로 확인.
1. **키 보안**: GEMINI_API_KEY가 로그/화면/예외 메시지/파일에 출력되지 않는지, 키 없을 때 크래시 없이 안내하는지
2. **전송 데이터**: 도구 결과·컨텍스트에 위치/좌표/경로/기기ID/패키지명이 없는지
3. **MIGRATION_2_3** 이 Room 스키마와 일치(컬럼 타입/NOT NULL/PK), 기존 MIGRATION_1_2 유지
4. 함수 호출 루프: functionResponse 형식이 Gemini v1beta 규격(contents에 role "user" 또는 "function", parts[{functionResponse:{name, response}}])과 맞는지, 최대 4회 제한, 무한 루프 없음
5. 동의 안 했을 때 전송 차단, 오류 분류(429/네트워크/키)
6. assembleDebug, testDebugUnitTest 성공
사소한 지적 금지. 결과: tasks/review-chat-result.md 첫 줄 PASS/FAIL, 8줄 이내. FAIL 항목은 실제로 생기는 상황 한 줄.
