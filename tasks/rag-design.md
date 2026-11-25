# 하이브리드 RAG 설계

## A. 내 건강 숫자 = 함수 호출 (기존) — 도구 사용 개선
- 실측: 지금까지 모든 질문에서 tools= 비어 있음(최근 7일 컨텍스트만으로 답함).
- 시스템 지시에 명시: "오늘 날짜는 {yyyy-MM-dd(요일)}. 최근 7일 밖의 기간, 비교(지난주/지난달/작년), 추세, 특정 날짜 질문은 **반드시 도구로 조회한 뒤** 답한다. 컨텍스트에 없는 숫자를 추측하지 않는다."
- 최근 7일 컨텍스트에 "이 요약은 {시작}~{끝} 기간만 포함" 명시.
- toolConfig: functionCallingConfig mode AUTO 유지(ANY는 강제라 일반 대화가 어색해짐).
- 도구 결과가 비면 "해당 기간 데이터 없음"을 명시적으로 반환.

## B. 건강 지식 = 벡터 검색 RAG
- 지식 문서: app/src/main/assets/kb/*.md (한국어). 각 파일 맨 위 front matter: title, source(기관·문서명), source_url(있으면), updated. 본문은 ## 소제목 단위.
- 청크: ## 소제목 단위(길면 600자 기준 분할, 100자 겹침). 각 청크에 문서 title + 소제목 접두.
- 임베딩: Gemini `gemini-embedding-001` (`:embedContent`, taskType RETRIEVAL_DOCUMENT / 질문은 RETRIEVAL_QUERY, outputDimensionality 768). 문서 임베딩은 **앱에서 1회 계산 후 DB 저장**, kb 파일 해시가 바뀌면 재계산. 임베딩 실패(503 등) 시 재시도 백오프, 끝내 실패하면 RAG 없이 답변(기능 저하만, 오류 아님).
- 저장: DB v4 + MIGRATION_3_4: kb_chunk(id PK auto, docId TEXT, title TEXT, heading TEXT, text TEXT, source TEXT, sourceUrl TEXT?, vector BLOB(float32 LE), contentHash TEXT). destructive 금지.
- 검색: 질문 임베딩 1회 → 기기 안에서 코사인 유사도 → 상위 4개 중 유사도 ≥ 0.55(임계값 상수) → 프롬프트에 "참고 자료" 블록으로 추가: [1] {title} – {heading} ({source}) \n {text}.
- 시스템 지시: "참고 자료를 근거로 조언할 땐 문장 끝에 [1]처럼 번호를 붙인다. 참고 자료에 없는 의학 정보는 일반 상식 수준으로만, 확신이 없으면 그렇다고 말한다."
- 답변과 함께 사용된 출처 목록 저장: chat_message에 `sourcesJson TEXT?` 컬럼 추가(같은 MIGRATION_3_4). UI는 답변 아래 "근거: [1] 대한수면학회 …" 칩, 누르면 해당 청크 내용 바텀시트.
- 개인정보: 질문 텍스트는 원래 Gemini로 가므로 임베딩 전송도 같은 범위. 지식 문서는 공개 자료.

## C. 지식 문서 목록 (각 파일 내용 요건은 kb-content 명세 참고)
sleep_adult.md, sleep_hygiene.md, activity_guideline.md, steps.md, heart_rate.md, weight_bmi.md, body_composition.md, measurement_limits.md, walking.md

## 공유 계약
```kotlin
// com.gh.healthapp.chat
data class KbSource(val index: Int, val title: String, val heading: String, val source: String, val sourceUrl: String?, val text: String)
// ChatMessage에 추가
val sources: List<KbSource> = emptyList()
// KnowledgeBase
object KnowledgeBase { suspend fun ensureIndexed(context: Context): Result<Int>; suspend fun status(context: Context): KbStatus }
data class KbStatus(val chunks: Int, val indexed: Boolean, val lastError: String?)
```
