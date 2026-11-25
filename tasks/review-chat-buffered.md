# 검수 (최소, 코드 수정 금지): tasks/chat-buffered.md, chat-buffered-ui.md 기준
1. 부분 텍스트가 화면/DB에 절대 나오지 않음(실패한 스트림 내용이 최종 답에 섞이지 않음)
2. 전체 5회 제한, 백오프 대기, 모델별 쿨다운, 404 제외 — 무한 반복 없음
3. 키 오류·네트워크 없음은 즉시 실패
4. UI가 progress로 진행 표시, streaming 참조 제거, 재전송·복사 유지
5. 키·내용 로그 없음, assembleDebug·testDebugUnitTest 성공
결과: tasks/review-chat-buffered-result.md 첫 줄 PASS/FAIL, 6줄 이내. FAIL은 실제 상황 한 줄.
