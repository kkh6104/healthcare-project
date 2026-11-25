# 구현 요청 (codex-1): 챗봇 엔진 — Gemini 연결·도구·DB
## 공통
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug testDebugUnitTest` (다른 에이전트와 동시 빌드 lock 시 30초 후 재시도). 한국어, MaterialTheme 색만.
- 설계 문서: tasks/chat-design.md (반드시 먼저 읽기)

- **수정 허용: gradle 빌드 파일(BuildConfig 키), data/ (DB v3, ChatDao, 도구용 쿼리 추가), 새 패키지 chat/ (ui 제외).** ui/ 폴더와 MainActivity는 codex-2가 동시에 수정 중이니 건드리지 말 것.
## 1. 키
- app/build.gradle.kts: local.properties의 GEMINI_API_KEY 읽어 `buildConfigField("String","GEMINI_API_KEY", "\"...\"")`, `buildFeatures { buildConfig = true }`. 없으면 "". local.properties는 읽기만(수정 금지). 키를 출력/로그하지 말 것.
## 2. DB v3
- ChatMessageEntity(chat_message), ChatDao, HealthDatabase version 3 + MIGRATION_2_3 (Room 스키마와 정확히 일치), 기존 MIGRATION_1_2 유지.
## 3. chat 패키지
- GeminiConfig(MODEL, ENDPOINT), GeminiClient(HttpURLConnection, org.json, 타임아웃 30초, 429→RateLimited, 401/403→Other("API 키 오류"), IOException→Network). 요청 JSON: systemInstruction, contents(최근 10턴 + 이번 질문), tools[{functionDeclarations}], generationConfig(temperature 0.6).
- 함수 호출 루프: 응답 parts에 functionCall이 있으면 HealthTools로 실행 → role "user"(또는 API 규격의 "function"/functionResponse 형식, **현재 v1beta 규격을 정확히 따를 것**) functionResponse 추가 → 재요청, 최대 4회. 텍스트 parts를 이어 최종 답.
- 첫 요청에 "최근 7일 요약"(get_daily_summary 결과 JSON)을 사용자 메시지 앞에 컨텍스트로 붙임.
- HealthTools: 설계 문서의 5개 도구를 DB 읽기로 구현(기존 DAO/DailySummary/SleepDay 그룹 로직/WalkDao 재사용). 날짜 범위 상한 강제, 잘못된 날짜면 {"error":...} 반환. **위치/경로/기기ID/패키지명은 결과에 넣지 않음.**
- ChatRepository/ChatConsent/ChatError/ChatException: 공유 계약대로. send는 사용자 메시지를 먼저 저장 → 성공 시 모델 답 저장. 동의 안 했으면 Other("데이터 전송 동의가 필요합니다").
- 로그(HCDEBUG): `chat send tools=<호출한 도구 이름들> ms=<시간> result=ok|<error>` — **질문/답 내용·키는 로그 금지.**
## 4. 테스트 (JVM)
- 요청 JSON 생성(시스템 지시·도구 선언 포함), 응답 파싱(텍스트/functionCall/오류), 도구 날짜 범위 검증 — 네트워크 없이.
## 완료
- assembleDebug testDebugUnitTest 성공. 실제 API 호출은 하지 말 것(키가 아직 없을 수 있음). tasks/chat-core-done.md 에 요약 + 공개 API
