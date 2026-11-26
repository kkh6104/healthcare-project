PASS
- 원인 설명은 코드와 일치(이전 catch가 예외를 로그 없이 삼킴, top은 0.6 필터 후 값) — 단 진짜 원인은 실기기 "chat rag embed fail ex=/code=" 로그로 확인 필요
- 질문·색인 모두 768차원 고정, 불일치 시 코사인 미계산+dim mismatch 로그+hash 제거→재색인, 임베딩 실패 시 RAG 없이 진행+ex·code 로그, top은 필터 전 최고값, "omni" 제외(테스트 포함), 질문·키 로그 없음
- assembleDebug·testDebugUnitTest BUILD SUCCESSFUL(UP-TO-DATE), 임베딩 실패 경로는 단위 테스트 없음(코드상 보장)
