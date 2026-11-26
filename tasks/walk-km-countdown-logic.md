# 구현 요청 (codex-1): 1km 음성 안내 + 카운트다운 엔진
- 빌드: PowerShell `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug testDebugUnitTest` (동시 빌드 lock 시 30초 후 재시도). 심박수 관련 기능은 넣지 않는다(사용자 결정).
- 수정 허용: walk/ 패키지, 테스트. ui/ 는 codex-2 담당.
## 1. 1km마다 안내
- 누적 거리(distanceM, 탈것·자동 일시정지 제외된 값)가 1000m의 배수를 처음 넘을 때마다: "{n}킬로미터, {시간}, {걸음}걸음" 예: "1킬로미터, 11분 59초, 1,420걸음" (시간은 activeMs, 기존 시간 포맷 함수 재사용, 걸음은 숫자 그대로 읽어도 됨).
- 같은 km 중복 안내 금지(마지막 안내 km 기억). 재개/자동 재개 직후에도 중복 없음.
- 이어폰·voice 스위치 규칙 동일. 15초 중복 방지 규칙과는 별개(서로 막지 않게).
- 설정 추가: WalkSettings.kmAnnounce (기본 true) + get/set/Flow.
- 순수 함수: 다음 안내할 km 판단, 문구 생성 + 테스트(0.99→1.01km에서 1회, 2km 동시 통과 시 마지막 km만, 일시정지 중 미안내).
## 2. 카운트다운 엔진
- WalkTracker에 `fun startWithCountdown(context, seconds: Int = 3)`, `fun cancelCountdown()`, 상태 `WalkLiveState.countdown: Int?`(3,2,1 동안 값, 아니면 null). WalkStatus에 COUNTDOWN 추가 또는 countdown 필드로(택1, 계약은 done 파일에 명시).
- 카운트다운은 앱 프로세스에서 1초 간격(코루틴), 끝나면 기존 start(context) 호출. 취소 시 IDLE.
- 음성: 이어폰·voice 조건이면 카운트다운 동안 "3", "2", "1"을 각 초에 말하고, 시작 시 기존 "걷기를 시작합니다". (TTS 준비 대기열 활용, 카운트다운 시작 시 TTS 미리 초기화해 3을 놓치지 않게. 놓치면 그냥 다음 숫자부터)
- 카운트다운 중 권한 확인은 UI에서 이미 끝났다고 가정, 서비스는 기존 권한 방어 유지.
## 완료: tasks/walk-km-countdown-logic-done.md 에 요약 + 공개 API(startWithCountdown/cancelCountdown/countdown 필드/kmAnnounce)
