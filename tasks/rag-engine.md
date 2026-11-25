# 구현 요청 (codex-1): 도구 사용 개선 + RAG 엔진
## 공통
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug testDebugUnitTest` (동시 빌드 lock 시 30초 후 재시도). 외부 라이브러리 추가 금지. 키·대화 내용 로그 금지.
- 설계: tasks/rag-design.md (반드시 먼저 읽기)

- 수정 허용: chat/ 패키지, data/ (DB v4, kb_chunk, chat_message.sourcesJson), 테스트. **ui/와 app/src/main/assets/kb/는 codex-2 담당이라 수정 금지**(읽기만).
## 1. 설계 A 전부 (시스템 지시에 오늘 날짜·요일, 7일 컨텍스트 범위 명시, 도구 강제 규칙, 빈 결과 명시)
## 2. 설계 B 엔진
- EmbeddingClient(embedContent, 768차원, 백오프 재시도 최대 3회, 키 로그 금지).
- KbChunker: front matter 파싱, ## 단위 청크, 600자/100자 겹침, 순수 함수.
- KnowledgeBase.ensureIndexed: assets/kb 전체 해시 비교 → 변경 시 재임베딩·저장(일괄, 중간 실패 시 이전 색인 유지). 앱 시작 후 백그라운드 1회 + 채팅 전송 시 미색인이면 시도(대기하지 않고 RAG 없이 진행).
- 검색: 코사인 유사도, 상위 4, 임계값 0.55, 결과 KbSource 목록. 사용된 소스를 ChatMessage.sources로 저장(sourcesJson).
- 로그: `kb index chunks=n ms=.. result=..`, `chat rag hits=n top=0.xx`, 기존 `chat send ... tools=...`.
- assets/kb가 비어 있거나 아직 없으면 색인 0개로 정상 동작.
## 3. DB v4 + MIGRATION_3_4 (Room 스키마 일치, 이전 마이그레이션 유지)
## 4. 테스트: 청크 분할, front matter, 코사인/상위 k/임계값, 벡터 BLOB 직렬화, 시스템 지시에 날짜 포함, sourcesJson 왕복
## 완료: tasks/rag-engine-done.md 요약 + 계약 확인
