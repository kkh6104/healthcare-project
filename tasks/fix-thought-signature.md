# 구현 요청 (codex-1): 함수 호출 thought_signature 누락 400 수정 (긴급)
## 실기기 로그
- 도구 호출 1차 요청 ok(get_period_stats) → functionResponse 보낸 2차 요청이 매번 HTTP 400 → result=other.
## PC 재현 (확정)
- 응답 parts = [{functionCall, thoughtSignature}]. model 턴을 다시 보낼 때 functionCall만 남기면 400:
  "Function call is missing a thought_signature in functionCall parts..."
- **모델이 준 parts를 그대로(thoughtSignature 포함, 순서 유지) model 턴으로 다시 보내면 200.**
## 수정 (chat/ 패키지, 테스트)
1. 모델 응답의 content.parts JSON을 **원본 그대로 보존**해 대화 이력의 model 턴으로 재전송(텍스트·functionCall·thoughtSignature·기타 필드 모두). 필드를 골라 재구성하지 말 것.
2. 병렬 함수 호출(parts에 functionCall 여러 개)도 각각 functionResponse로, 순서 맞춰 하나의 user 턴에.
3. 스트리밍(SSE)로 받은 응답에서 functionCall/thoughtSignature가 여러 청크에 나뉘어 올 수 있으면 합쳐서 보존(도구 단계는 generateContent 비스트리밍 사용으로 단순화해도 됨).
4. 400 응답 시 로그에 error.status와 message 앞 120자(키·사용자 내용이 없는 서버 메시지이므로 허용): `chat fail kind=http code=400 status=.. msg=..`.
5. 저장된 과거 대화(10턴)를 다시 보낼 때는 텍스트 턴만 보내므로 문제 없음 확인.
6. 테스트: thoughtSignature 포함 응답 → 다음 요청 JSON에 동일 parts가 그대로 포함되는지, 병렬 호출.
assembleDebug testDebugUnitTest 성공 후 tasks/fix-thought-signature-done.md 1~3줄.
