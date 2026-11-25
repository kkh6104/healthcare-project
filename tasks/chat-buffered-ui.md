# 구현 요청 (codex-2): 완성 후 표시 화면
## 공통
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug testDebugUnitTest` (동시 빌드 lock 시 30초 후 재시도). 키·대화 내용 로그 금지.
## 실기기 로그 (원인: Gemini 서버 503, 스트림 도중에도 발생)
- flash-latest 503 ×2 → flash-lite 스트림 도중 503 ×2 → busy. 재전송: lite 도중 503 → 다음 시도 성공.
- 사용자 결정: **답이 다 완성되면 한 번에 표시**(부분 텍스트가 나왔다 사라지는 것 금지).

- 수정 허용: ui/chat/ 만. codex-1이 `ChatRepository.progress: StateFlow<ChatProgress?>`(attempt, maxAttempts, retrying, startedAt)를 추가하고 streaming은 없어짐. tasks/chat-buffered-done.md 생길 때까지(최대 20분, 1분 간격) 기다렸다 연결.
- 전송 중 AI 말풍선 자리에 진행 표시만: 첫 시도 "생각하는 중… N초"(startedAt 기준 1초 갱신), retrying 또는 attempt≥2면 "서버가 붐벼서 다시 시도하는 중… (attempt/maxAttempts)". 작은 CircularProgressIndicator 함께.
- 답이 완성되면 말풍선이 한 번에 나타남(부드러운 fade-in 정도는 OK). 스트리밍 텍스트 표시 코드는 제거.
- 재전송/복사/키패드 처리 유지.
## 완료: assembleDebug 성공, tasks/chat-buffered-ui-done.md 1~2줄
