# 구현 요청 (codex-2): 걷기 화면 수정 + 설정 스위치
## 공통
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug testDebugUnitTest` (다른 에이전트와 동시 빌드 lock 시 30초 후 재시도). 외부 라이브러리 추가 금지. 한국어, MaterialTheme 색만.
- 기존 설계: tasks/walk-design.md, 이전 보정: tasks/fix-walk-jitter.md, tasks/fix-walk-jitter-done.md.

- **수정 허용: ui/walk/ 폴더만.** walk/ 패키지는 codex-1이 동시에 수정 중이니 건드리지 말 것.

## 1. 화면 겹침 2개 (실기기 스크린샷)
- **걷는 중 화면**: 거리 큰 글씨("0.08 km")가 지도 아래쪽에 겹침. 지도와 통계를 Column으로 명확히 분리(지도 weight 또는 고정 높이 비율, 통계 영역은 지도 밖). 지도는 clipToBounds.
- **걷기 기록 상세**: 날짜·시간·거리 텍스트가 지도 위에 겹쳐 그려짐. 지도(고정 높이, 예: 화면 높이 45% 또는 320dp) 아래에 정보 영역을 따로 배치하고 전체 verticalScroll.
- 원인을 tasks/walk-v2-ui-done.md 에 한 줄로.

## 2. 상태 표시 (codex-1이 추가하는 값 사용)
- codex-1이 `WalkStatus.AUTO_PAUSED`(또는 `WalkLiveState.autoPaused`)와 `WalkLiveState.inVehicle`을 추가 중. 걷는 중 화면 상단에 배지: 자동 일시정지면 "자동 일시정지됨 · 걸으면 다시 시작", 탈것이면 "이동 중 (걸음 없음) · 거리 제외".
- 자동 일시정지 중 버튼: "일시정지"→"재개"로 보이지 않게(자동이므로) 그대로 "일시정지" 유지.
## 3. 설정 스위치
- 걷기 탭 쉬는 화면(시작 버튼 아래)에 Switch 2개: "자동 일시정지", "이어폰 음성 안내" — codex-1의 `WalkSettings`(autoPause, voice) 사용.
## 4. 의존 순서
- codex-1의 API가 아직 없으면 컴파일 에러가 나므로, 1번(겹침)을 먼저 끝내고 빌드 확인 → 2·3번은 codex-1 완료(tasks/walk-v2-logic-done.md 생성) 후 그 API 이름 그대로 연결. 최대 15분, 1분 간격으로 파일 존재 확인.
## 완료
- assembleDebug 성공, tasks/walk-v2-ui-done.md 에 2~4줄
