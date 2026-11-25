# 구현 요청 (codex-1): 스트리밍 도중 실패 대응 + 진단 로그
## 실기기 로그
- 21:42:09 lite 모델, firstTokenMs=1252, ms=5483, result=other → 글자가 나오다 멈추고 오류. 재전송 시 정상.
- 21:41 첫 질문: firstTokenMs=16367, 최종 lite (기본 모델 시도에서 시간 소모로 추정).
## 수정 (chat/ 패키지, 테스트만. 키·대화 내용 로그 금지)
1. 진단 로그: 실패 시 `chat fail kind=<parse|http|finish|timeout|network> code=<HTTP코드> finish=<finishReason> ex=<예외 클래스명>`. 모델 시도마다 `chat attempt model=.. ms=.. outcome=..`.
2. SSE 이벤트 처리 견고화: JSON 파싱 실패한 줄은 건너뛰고 계속(로그 kind=parse 카운트). 스트림 안의 `error` 객체 이벤트 처리. candidates 없는 이벤트(usageMetadata만 등) 무시.
3. finishReason이 STOP/없음이 아니면(MAX_TOKENS, SAFETY, RECITATION, OTHER 등) 지금까지 텍스트를 결과로 쓰고 뒤에 안내 문구: MAX_TOKENS "(답변이 길어 여기까지만 표시했어요)", 그 외 "(답변이 중간에 끊겼어요)". generationConfig.maxOutputTokens 2048 명시.
4. 스트림 도중 IOException/파싱 치명 오류 → **자동 1회 재시도**(같은 모델, 처음부터). 재시도도 실패 시 지금까지 받은 부분 텍스트가 있으면 "(답변이 중간에 끊겼어요)"를 붙여 **모델 답으로 저장(성공 처리)**, 없으면 기존 오류.
5. 테스트: 깨진 JSON 줄 섞인 SSE, finishReason MAX_TOKENS/SAFETY, 스트림 중단 후 부분 텍스트 저장.
assembleDebug testDebugUnitTest 성공 후 tasks/chat-stream-robust-done.md 1~3줄.
