# 구현 요청 (codex-2): 채팅 화면 — 키패드·재전송·복사·진행 표시
## 공통
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug testDebugUnitTest` (동시 빌드 lock 시 30초 후 재시도). 외부 라이브러리 추가 금지. 키·대화 내용 로그 금지.
- 설계: tasks/chat-design.md, 이전: tasks/chat-core-done.md, tasks/chat-fallback-done.md
## 실측 (실기기 로그)
- Q1: 8.9초, gemini-flash-lite-latest(기본 모델 503 → 재시도 → 폴백), tools 없음, 성공
- Q2: 33초, gemini-flash-latest, result=network — readTimeout 30초 초과(503 아님, 느린 생성). 폴백 안 됨, 답 없음

- **수정 허용: ui/chat/ 폴더, MainActivity.kt(인셋 처리 필요 시).** chat/ 패키지는 codex-1이 동시에 수정 중이니 건드리지 말 것.
- codex-1이 추가하는 `ChatRepository.streaming: StateFlow<String?>`, `resend(userMessageId)`를 사용. 아직 없으면 1번(키패드)부터 하고, tasks/chat-speed-done.md 생길 때까지(최대 20분, 1분 간격) 기다린 뒤 연결.
## 1. 키패드에 가려짐
- enableEdgeToEdge 환경이라 adjustResize만으로는 Compose가 안 줄어듦. 채팅 화면 루트에 `Modifier.imePadding()`(Scaffold innerPadding과 이중 적용 안 되게 consumeWindowInsets 처리), 입력창이 키패드 바로 위에 붙게. 키패드가 열리면 목록을 마지막 메시지로 스크롤. 하단 탭바는 키패드가 열려 있을 때 숨겨도 됨.
## 2. 스트리밍 표시: streaming이 null이 아니면 AI 말풍선에 그 텍스트를 실시간 표시(빈 문자열이면 "생각하는 중… N초", 1초마다 경과 시간 갱신).
## 3. 재전송: 마지막 사용자 메시지에 대한 답이 실패하면(오류 말풍선) 그 아래 "다시 보내기" 버튼 → resend(id). 전송 중엔 비활성.
## 4. 복사: 말풍선 길게 누르기 → 작은 메뉴(DropdownMenu) "복사" → ClipboardManager로 복사 + "복사했어요" 스낵바/토스트. 사용자·AI 말풍선 모두.
## 완료: assembleDebug 성공, tasks/chat-ux-done.md 2~4줄
