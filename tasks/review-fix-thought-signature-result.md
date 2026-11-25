PASS
- 모델 parts 원본(text·functionCall·thoughtSignature·기타 필드, 순서 유지)을 model 턴으로 재전송, SSE로 나뉜 서명/인자 조각은 ModelParts가 합쳐 보존
- 병렬 functionCall은 순서대로 functionResponse(id 포함)를 하나의 user 턴에 담음, 과거 대화는 텍스트 턴만 전송
- 400 로그 status/msg 120자는 키·요청 텍스트·질문을 [redacted] 처리, 관련 테스트 3개 존재, assembleDebug·testDebugUnitTest BUILD SUCCESSFUL
