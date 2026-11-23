# 구현 요청 (claude-1): 수면 미래 세션 제외

검수 지적: tasks/review-step2b-result.md 3번.
SleepRepository.kt의 buildSleepSummary에서 **endTime > now 인 세션은 제외**한다 (recent 합계, lastNight 모두). 다른 코드는 건드리지 않는다.
완료: assembleDebug 성공, tasks/fix-step2b-done.md 에 1~2줄.
