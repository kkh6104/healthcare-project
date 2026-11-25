# 구현 요청 (codex-2): 챗봇 화면 — AI 탭·동의·채팅
## 공통
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug testDebugUnitTest` (다른 에이전트와 동시 빌드 lock 시 30초 후 재시도). 한국어, MaterialTheme 색만.
- 설계 문서: tasks/chat-design.md (반드시 먼저 읽기)

- **수정 허용: MainActivity.kt(탭 추가), 새 폴더 ui/chat/, ui/SettingsScreen.kt(동의 철회 항목).** data/, chat/ 패키지, gradle 파일은 codex-1이 동시에 수정 중이니 건드리지 말 것.
- 공유 계약(설계 문서)의 ChatRepository/ChatConsent/ChatMessage/ChatError를 사용. 아직 없으면 화면 코드를 먼저 작성하고, tasks/chat-core-done.md가 생길 때까지(최대 20분, 1분 간격) 기다렸다가 빌드·연결.
## 1. 탭: 오늘/활동/걷기/수면/몸/**AI** (6개, 아이콘 Icons.Default 중 적당한 것). 기존 인덱스 매핑·상세 화면 유지.
## 2. ui/chat/ChatTab.kt
- 동의 안 했으면 ConsentScreen: 보내는 것(날짜별 걸음·수면·심박·체중·체성분 요약 숫자, 걷기 거리·시간), 보내지 않는 것(위치·경로, 이름, 기기 정보), Gemini 무료 등급은 구글이 입력을 서비스 개선에 사용할 수 있음, "동의하고 시작" 버튼.
- 키 없으면 상단 안내 카드: "API 키가 설정되지 않았습니다 (local.properties의 GEMINI_API_KEY)".
- 채팅: LazyColumn 말풍선(사용자 오른쪽 primaryContainer, AI 왼쪽 surfaceVariant), 자동 스크롤, 입력창+전송(빈 입력/전송 중 비활성), 전송 중 AI 말풍선 자리에 "생각하는 중…", 오류는 errorContainer 말풍선(NoKey/Network/RateLimited "요청이 많아요. 잠시 후 다시 시도해 주세요"/Other).
- 대화가 비었을 때 추천 질문 칩: "이번 주 수면 어때?", "지난달이랑 걸음 비교해줘", "체중 변화 추세 알려줘", "요즘 건강 조언 해줘".
- 상단 메뉴 "대화 지우기"(확인 다이얼로그).
- AI 답변의 **굵게**와 "- " 목록만 간단 렌더링(AnnotatedString), 나머지는 일반 텍스트.
## 3. 설정 화면: "AI 데이터 전송 동의" 스위치(끄면 철회).
## 완료: assembleDebug 성공, tasks/chat-ui-done.md 2~4줄
