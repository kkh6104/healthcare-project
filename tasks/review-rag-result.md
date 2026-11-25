PASS
1. MIGRATION_3_4: chat_message.sourcesJson TEXT(nullable)·kb_chunk 컬럼 타입/NOT NULL/PK가 엔티티와 일치, MIGRATION_1_2·2_3 유지
2. 검색 실패/임베딩 503은 빈 sources로 RAG 없이 진행, 색인은 @Transaction replaceAll로 원자적 교체, 임베딩 재시도 3회 제한(무한 없음)
3. 시스템 지시에 오늘 날짜, 7일 요약 범위, 범위 밖·비교·추세 질문은 도구 조회 규칙 포함
4. 로그는 kb/rag 개수·점수·시간·결과만, 키·대화 내용 없음 / assembleDebug·testDebugUnitTest BUILD SUCCESSFUL
5. 지식 문서 9개 내용은 kb-content.md 사실 목록 범위 안(새 수치·주장 없음)
URL 200: who.int/publications/i/item/9789240015128/, aasm.org/.../sleep-essential-health-AASM-2021.pdf
URL 403(curl -sI 기준, 봇 차단 가능성): heart.org/.../all-about-heart-rate-pulse, thelancet.com/article/S2468266721003029/pdf
URL 없음: body_composition, measurement_limits, sleep_hygiene, weight_bmi (source_url 비어 있음)
