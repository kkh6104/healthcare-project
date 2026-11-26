# 구현 요청 (codex-2): 카운트다운 화면 + 1km 안내 스위치
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug testDebugUnitTest` (동시 빌드 lock 시 30초 후 재시도). 심박수 관련 기능은 넣지 않는다(사용자 결정).
- 수정 허용: ui/walk/ 만. walk/ 는 codex-1이 동시에 수정 중. codex-1의 API(startWithCountdown, cancelCountdown, WalkLiveState.countdown, WalkSettings.kmAnnounce)는 tasks/walk-km-countdown-logic-done.md 생성 후 그 이름대로 연결(최대 20분, 1분 간격).
- "걷기 시작" 버튼 → 권한 확인 통과 시 startWithCountdown.
- countdown 값이 있으면 전체 화면 오버레이: 아주 큰 숫자(3→2→1, 바뀔 때 scale/fade 애니메이션), 아래 "취소" 버튼(cancelCountdown). 시스템 뒤로가기도 취소.
- 설정 스위치 추가: "1km마다 음성 안내" (kmAnnounce). 기존 "자동 일시정지", "이어폰 음성 안내" 아래.
- 완료: assembleDebug 성공, tasks/walk-km-countdown-ui-done.md 1~2줄
