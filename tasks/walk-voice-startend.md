# 구현 요청 (codex-1): 걷기 시작·종료 음성 안내
- 수정 허용: walk/ 패키지(WalkVoice, WalkService 등), 테스트.
- 기존 규칙 그대로: **이어폰 연결 시에만**, WalkSettings.voice 스위치 따름, 오디오 포커스 duck, TTS 실패 시 조용히 무시.
## 문구
- 시작(사용자가 시작 버튼, 서비스 시작 시 1회): "걷기를 시작합니다"
- 종료(사용자 종료 시): "걷기를 종료했습니다. {거리}, {시간}"
  - 거리: 1km 이상 "1.74킬로미터"(소수 둘째 자리, 끝 0은 생략 가능), 1km 미만 "850미터"(10m 단위 반올림)
  - 시간: activeMs 기준 "22분 48초", 1시간 이상 "1시간 5분", 1분 미만 "40초"
  - 순수 함수로 분리 + 테스트(경계값: 999m, 1000m, 59초, 60초, 3600초)
- 재개 TTS 중복 방지(15초) 규칙은 시작/종료 문구에는 적용하지 않음.
- **종료 음성은 서비스 종료 전에 말이 끝나도록**: 종료 처리(DB 저장) 후 TTS가 끝날 때까지(UtteranceProgressListener onDone/onError, 최대 6초) 기다렸다가 stopSelf + shutdown. 이어폰 없거나 voice 꺼짐이면 기다리지 않음.
- 수동 일시정지/재개는 여전히 말하지 않음. restore로 마감되는 세션도 말하지 않음.
- 로그: 기존 `walk voice said=..|skipped(..)` 형식.
assembleDebug testDebugUnitTest 성공 후 tasks/walk-voice-startend-done.md 1~3줄.
