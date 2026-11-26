# 구현 요청 (codex-1): RAG 검색 0건(top=0.00) 원인 수정 + omni 모델 제외
## 실측
- 이전: `chat rag hits=4 top=0.70`. chat-free-speed/IO 수정 후: 매 질문 `chat rag hits=0 top=0.00`. kb index는 ok(27 chunks, 캐시 재사용).
- top=0.00 은 근거 없는 질문이라도 나오기 어려움 → 질문 임베딩 실패/벡터 비어 있음/차원 불일치/예외 삼킴 의심.
## 할 일 (chat/ 패키지, 테스트)
1. 원인 진단: 질문 임베딩 호출 결과·벡터 길이·색인 벡터 길이·예외를 로그로: `chat rag embed ok dim=.. ms=..` / `chat rag embed fail ex=.. code=..` / `chat rag index dim=.. n=..`. 질문 내용·키는 로그 금지.
2. 원인 수정. 의심 지점: (a) IO 이동 과정에서 임베딩 호출이 스킵/취소되는 흐름, (b) 질문 임베딩 taskType/outputDimensionality 누락으로 차원 불일치(768 vs 3072)→코사인 0 처리, (c) 0.6 임계값 변경 시 top 값 계산/로그 버그, (d) 예외를 삼키고 빈 벡터 반환.
   - 차원이 다르면 코사인 계산하지 말고 `dim mismatch` 로그 + 색인 무효화 후 재색인 트리거.
3. top 로그는 임계값 필터 **전** 최고 유사도를 출력.
4. 모델 후보에서 이름에 "omni"가 들어간 모델 제외(필터에 추가, 테스트 포함).
5. 테스트: 차원 불일치 처리, 임베딩 실패 시 RAG 없이 진행 + 로그, top 로그 값.
assembleDebug testDebugUnitTest 성공 후 tasks/fix-rag-zero-done.md 에 **원인 한 줄** + 수정 요약.
