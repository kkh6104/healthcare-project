# 구현 요청 (codex-1): 완성 후 표시 + 백오프 재시도 + 예비 모델
## 공통
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug testDebugUnitTest` (동시 빌드 lock 시 30초 후 재시도). 키·대화 내용 로그 금지.
## 실기기 로그 (원인: Gemini 서버 503, 스트림 도중에도 발생)
- flash-latest 503 ×2 → flash-lite 스트림 도중 503 ×2 → busy. 재전송: lite 도중 503 → 다음 시도 성공.
- 사용자 결정: **답이 다 완성되면 한 번에 표시**(부분 텍스트가 나왔다 사라지는 것 금지).

- 수정 허용: chat/ 패키지, 테스트. ui/ 는 codex-2 담당.
## 계약 변경 (codex-2 사용)
```kotlin
// ChatRepository: streaming 제거(또는 항상 null) 대신
data class ChatProgress(val attempt: Int, val maxAttempts: Int, val retrying: Boolean, val startedAt: Long)
val progress: StateFlow<ChatProgress?>   // 전송 중이면 값, 아니면 null
```
## 1. 버퍼링: 스트리밍으로 받되 화면엔 내보내지 않음. 최종 텍스트(finishReason 처리 포함)가 완성되면 DB 저장. 스트림 도중 실패 시 버린 부분은 절대 저장/표시하지 않음(이전의 "부분 텍스트 저장" 동작 제거).
## 2. 재시도: 질문당 **전체 최대 5회 시도**. 대기 1s, 2s, 4s, 4s (지터 ±20%). 5xx/타임아웃/스트림 도중 중단이 재시도 대상. 401/403/400(키)·네트워크 없음(UnknownHost)은 즉시 실패.
## 3. 모델 순서: GeminiConfig.MODELS = ["gemini-flash-latest", "gemini-flash-lite-latest", "gemini-2.5-flash"]. 
   - 시도 배치 예: 1 primary(쿨다운이면 건너뜀) → 2 lite → 3 2.5-flash → 4 lite → 5 2.5-flash. 모델별 붐빔 쿨다운 5분(기존 primary 쿨다운을 모델별로 일반화).
   - 2.5-flash는 thinkingConfig {"thinkingBudget":512} (3 계열은 thinkingLevel low). 400 thinking 오류 시 해당 요청만 thinkingConfig 없이 재시도(시도 횟수에 미포함, 1회).
   - 시작 시 ListModels로 존재 확인은 불필요. 404(NOT_FOUND) 모델은 이번 앱 실행 동안 목록에서 제외 + 로그.
## 4. progress: 시도 시작마다 attempt 갱신, 대기 중엔 retrying=true. 완료/실패 시 null.
## 5. 로그: 기존 attempt/fail 로그 유지 + `chat send attempts=n`.
## 6. 테스트: 스트림 중단 후 재시도 성공 시 첫 부분 텍스트가 결과에 섞이지 않음, 5회 제한, 404 모델 제외, 백오프 순서.
## 완료: tasks/chat-buffered-done.md 1~3줄 + 계약 변경
