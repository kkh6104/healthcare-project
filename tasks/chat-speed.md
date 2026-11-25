# 구현 요청 (codex-1): 챗봇 속도·안정성 — 스트리밍, 생각 단계, 타임아웃 폴백
## 공통
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug testDebugUnitTest` (동시 빌드 lock 시 30초 후 재시도). 외부 라이브러리 추가 금지. 키·대화 내용 로그 금지.
- 설계: tasks/chat-design.md, 이전: tasks/chat-core-done.md, tasks/chat-fallback-done.md
## 실측 (실기기 로그)
- Q1: 8.9초, gemini-flash-lite-latest(기본 모델 503 → 재시도 → 폴백), tools 없음, 성공
- Q2: 33초, gemini-flash-latest, result=network — readTimeout 30초 초과(503 아님, 느린 생성). 폴백 안 됨, 답 없음

- **수정 허용: chat/ 패키지, 테스트.** ui/ 는 codex-2가 동시에 수정 중이니 건드리지 말 것.
## 공유 계약 변경 (codex-2가 사용)
```kotlin
// ChatRepository에 추가
val streaming: StateFlow<String?>   // 생성 중인 답변의 지금까지 텍스트, 없으면 null
suspend fun resend(userMessageId: Long): Result<Unit>  // 실패한 사용자 메시지를 다시 보냄(새 사용자 메시지를 만들지 않고 그 메시지로 재요청)
```
- send/resend 성공 시 최종 답을 DB 저장 후 streaming=null. 실패 시 streaming=null.
## 1. 스트리밍
- 최종 텍스트 생성 요청은 `:streamGenerateContent?alt=sse` 사용, SSE 줄("data: {...}") 파싱, 텍스트 part를 이어 붙여 streaming에 반영(100ms 이내 간격으로 갱신 묶어도 됨).
- 도구 호출(functionCall)이 나오는 왕복 단계는 기존 generateContent 그대로 써도 되고, 스트림에서 functionCall이 오면 처리해도 됨(택1, 단순한 쪽). 
## 2. 생각 단계 낮추기
- generationConfig에 `thinkingConfig`: Gemini 3 계열은 `{"thinkingLevel":"low"}`, 2.5 계열은 `{"thinkingBudget":512}`. 모델 응답의 modelVersion 또는 모델 이름으로 판단하기 어렵다면 thinkingLevel low를 보내고, 400(INVALID_ARGUMENT, thinking 관련)이 오면 thinkingConfig 없이 1회 재시도.
## 3. 타임아웃·폴백
- connectTimeout 15초, readTimeout 60초(스트리밍은 데이터가 올 때마다 갱신되는 읽기라 문제 없음).
- **SocketTimeoutException도 5xx와 같은 "붐빔"으로 처리** → 다음 모델로 폴백(재시도 없이 바로).
- 기본 모델이 최근 5분 안에 붐빔(5xx/타임아웃)이었으면 이번 질문은 처음부터 lite 모델로 시작(메모리 변수, 로그 `model skip primary (cooldown)`).
- 네트워크 자체 실패(UnknownHost 등)만 Network.
## 4. 로그: 기존 + `firstTokenMs=`(첫 글자 도착 시간), `stream=true`
## 5. 테스트: SSE 파싱(여러 청크, 빈 줄, [DONE] 없는 종료), 타임아웃→폴백, 쿨다운 시 lite 시작, thinking 400 재시도
## 완료: tasks/chat-speed-done.md 에 요약 + 계약 변경 확인
