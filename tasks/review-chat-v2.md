# 검수 (중간, 최소, 코드 수정 금지)
명세: tasks/chat-speed.md, tasks/chat-ux.md. git diff로 확인.
1. SSE 파싱이 청크 경계·빈 줄에서 텍스트 누락/중복 없는지, 스트림 중 오류 시 streaming=null로 정리되는지
2. 타임아웃→폴백, 5분 쿨다운, thinking 400 재시도가 무한 반복 없이 끝나는지
3. resend가 새 사용자 메시지를 중복 생성하지 않는지
4. 키패드: imePadding 이중 적용 없이 입력창이 키패드 위에 오는지(코드상), 복사 동작
5. 키·대화 내용 로그 없음, assembleDebug·testDebugUnitTest 성공
결과: tasks/review-chat-v2-result.md 첫 줄 PASS/FAIL, 6줄 이내. FAIL은 실제 상황 한 줄.
