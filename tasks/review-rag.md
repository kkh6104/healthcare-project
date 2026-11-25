# 검수 (중간, 최소) — 코드는 수정하지 말 것
명세: tasks/rag-design.md, rag-engine.md, kb-content.md. git diff + 새 파일 확인.
1. **MIGRATION_3_4** Room 스키마 일치(kb_chunk, chat_message.sourcesJson), 이전 마이그레이션 유지
2. 색인 실패/임베딩 503 시 채팅이 막히지 않고 RAG 없이 진행, 색인 원자적 교체, 무한 재시도 없음
3. 시스템 지시에 오늘 날짜·7일 범위·도구 사용 규칙 포함
4. 키·대화 내용 로그 없음
5. **지식 문서(assets/kb) 사실 검증**: kb-content.md 사실 목록을 벗어난 수치·주장 없는지. 각 source_url을 `curl -sI -L <url>` 로 확인해 열리지 않으면(4xx/5xx/DNS) 목록으로 보고 (수정은 하지 말 것)
6. assembleDebug·testDebugUnitTest 성공
결과: tasks/review-rag-result.md 첫 줄 PASS/FAIL, 10줄 이내. FAIL은 실제 상황 한 줄. URL 결과는 별도 줄로.
