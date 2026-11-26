# 검수 (최소, 코드 수정 금지): tasks/chat-free-speed.md 기준
1. 모델 목록 조회 실패 시 고정 목록으로 동작, 하루 1회 캐시, 부적합 모델(image/tts/embedding 등) 제외
2. 성공 기반 순서가 쿨다운과 충돌 없이 유한하게 동작(최대 시도 5회 유지)
3. 보내는 양 축소(k=3, 0.6, 400자, 7일 CSV, 60일 초과 주 단위, 6턴, maxOutputTokens 1024)
4. 키·내용 로그 없음, assembleDebug testDebugUnitTest 성공
결과: tasks/review-chat-free-speed-result.md 첫 줄 PASS/FAIL, 5줄 이내. FAIL은 실제 상황 한 줄.
