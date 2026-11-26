# 구현 요청 (codex-1): 시작·종료 음성 검수 지적 2건 수정
검수 결과: tasks/review-walk-voice-startend-result.md
1. **시작 안내 무음**: TTS 초기화(비동기) 전에 say()가 호출돼 skipped(not ready). → WalkVoice에 대기열: 준비 전 say 요청은 보관했다가 onInit 성공 시 재생(보관 후 5초 지나면 폐기). 초기화 실패 시 폐기+로그. 조건(이어폰·스위치)은 재생 시점에 다시 확인.
2. **거리 반올림**: 1km 미만은 10m 단위 **반올림**(995→1000이면 "1킬로미터"로 표기 전환, 994→"990미터"), 1km 이상은 소수 둘째 자리 반올림. 테스트 기대값 수정 + 994/995/999/1000/1004/1005 경계 추가.
walk/ 패키지와 테스트만. assembleDebug testDebugUnitTest 성공 후 tasks/fix-walk-voice-startend-done.md 1~2줄.
